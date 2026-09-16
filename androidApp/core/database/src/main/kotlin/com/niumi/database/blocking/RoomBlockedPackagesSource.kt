package com.niumi.database.blocking

import android.database.sqlite.SQLiteException
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.isBlockingPending
import com.niumi.database.BlockedPackage
import com.niumi.database.EffectStatus
import com.niumi.database.NiumiDatabase
import com.niumi.database.directboot.DirectBootSnapshot
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.directboot.UnlockState
import com.niumi.database.entity.AlarmSessionEntity
import com.niumi.database.mapping.toBlockedPackage
import com.niumi.database.mapping.toSnapshotDto
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import com.niumi.database.directboot.toSnapshotDto as projectionToSnapshotDto

/**
 * Reconstruit la projection de blocage depuis la persistance (SPEC_ANDROID §12.2 : « le service
 * doit recharger l'état actif depuis Room ou le snapshot après recréation »). Remplace la seule
 * mémoire du contrôleur, qui disparaissait avec le processus et levait le blocage en silence
 * alors que la session courait toujours (limite de l'étape 5, `ETAPE-05.md`).
 *
 * Route vers Room une fois l'appareil déverrouillé, vers [DirectBootStore] avant, sur le modèle
 * d'`UnlockAwareTechnicalEventLog` : [databaseProvider] n'est jamais résolu tant que
 * [UnlockState.isUserUnlocked] est faux (SPEC_ANDROID §7.3).
 *
 * Fonction `suspend` : tous les DAO le sont. Le service d'accessibilité, lui, doit décider
 * immédiatement dans `onAccessibilityEvent` — c'est `BlockedPackagesProjection` (`:core:system`)
 * qui garde le résultat en cache et appelle cette lecture aux moments où elle peut attendre.
 */
@Singleton
class RoomBlockedPackagesSource
    @Inject
    constructor(
        private val unlockState: UnlockState,
        private val directBootStore: DirectBootStore,
        private val databaseProvider: Provider<NiumiDatabase>,
    ) : BlockedPackagesSource {
        override suspend fun read(): BlockedPackagesRead =
            if (unlockState.isUserUnlocked) readFromRoom() else readFromDirectBoot()

        // `SQLiteException` seule (SPEC_ANDROID §18, étape 20) : avant cette étape, elle traversait
        // le `collect` de `BlockingProjectionRefresher.observeDecisions()` sans jamais être
        // rattrapée, ce qui arrêtait **définitivement** le rafraîchissement du blocage — un défaut
        // réel, indépendant du reste de l'étape, découvert en la cherchant.
        private suspend fun readFromRoom(): BlockedPackagesRead =
            try {
                val database = databaseProvider.get()
                val sessionId = database.activeSessionPointerDao().current()?.sessionId
                val session =
                    sessionId?.let { database.sessionDao().findById(it) }
                        ?: return BlockedPackagesRead.Resolved(BlockedPackagesState.Inactive)
                val packages =
                    database
                        .blockedAppDao()
                        .forSession(session.id)
                        .map { it.toBlockedPackage() }
                        .toSet()
                BlockedPackagesRead.Resolved(roomStateOf(database, session, packages))
            } catch (exception: SQLiteException) {
                BlockedPackagesRead.Unreadable(exception.message ?: "ROOM_UNREADABLE")
            }

        /**
         * En Room, un effet est décidé dès que sa ligne existe : l'absence de ligne signifie
         * « jamais décidé », et seule la révision la plus récente décrit l'état courant.
         *
         * **`ARMED` a sa propre branche depuis le Lot 6** : l'état ne dit plus si le blocage est
         * appliqué, `blockingAppliedAtEpochMillis` le dit (SPEC_ANDROID §12.2, table de projection).
         * Le laisser tomber dans le `else` bloquerait les applications dès l'armement, avant l'heure
         * de début choisie par l'utilisateur. La condition passe par `isBlockingPending`
         * (`:shared:core`) et non par une recopie de la règle : le KDoc de `BlockingStatus` interdit
         * toute troisième copie, côté commun comme côté natif.
         *
         * C'est bien le champ du snapshot qui décide, et non le statut de l'effet `APPLY_BLOCKING`
         * comme pour `PREPARING` : dans le miroir Direct Boot, une session différée n'a aucun
         * `APPLY_BLOCKING` avant son début, et « absent donc exécuté » y donnerait un blocage actif
         * avant l'heure.
         */
        private suspend fun roomStateOf(
            database: NiumiDatabase,
            session: AlarmSessionEntity,
            packages: Set<BlockedPackage>,
        ): BlockedPackagesState =
            when (session.state) {
                in FINAL_STATES -> {
                    BlockedPackagesState.Inactive
                }

                SessionStateDto.RELEASING -> {
                    BlockedPackagesState.Releasing(
                        sessionId = session.id,
                        effectivePackages =
                            if (hasSucceededInRoom(database, session.id, SessionEffectKindDto.REMOVE_BLOCKING)) {
                                emptySet()
                            } else {
                                packages
                            },
                    )
                }

                SessionStateDto.PREPARING -> {
                    if (hasSucceededInRoom(database, session.id, SessionEffectKindDto.APPLY_BLOCKING)) {
                        BlockedPackagesState.Active(session.id, packages)
                    } else {
                        BlockedPackagesState.Inactive
                    }
                }

                SessionStateDto.ARMED -> {
                    if (session.toSnapshotDto().isBlockingPending) {
                        BlockedPackagesState.Inactive
                    } else {
                        BlockedPackagesState.Active(session.id, packages)
                    }
                }

                else -> {
                    BlockedPackagesState.Active(session.id, packages)
                }
            }

        private suspend fun hasSucceededInRoom(
            database: NiumiDatabase,
            sessionId: String,
            kind: SessionEffectKindDto,
        ): Boolean =
            database
                .outboxDao()
                .forSessionAndKind(sessionId, kind)
                .firstOrNull()
                ?.status in SUCCEEDED_STATUSES

        private fun readFromDirectBoot(): BlockedPackagesRead =
            when (val stored = directBootStore.read()) {
                null -> BlockedPackagesRead.Resolved(BlockedPackagesState.Inactive)
                is DirectBootSnapshot.Corrupted -> BlockedPackagesRead.Unreadable(stored.reason)
                is DirectBootSnapshot.Active -> BlockedPackagesRead.Resolved(directBootStateOf(stored))
            }

        /**
         * Le miroir Direct Boot ne recopie que les effets rejouables (`UnlockAwarePersistenceGateway`
         * l'alimente depuis `SessionStore.pendingEffects`, donc `PENDING`/`FAILED` seuls) : ici
         * l'**absence** d'un effet vaut « déjà exécuté », exactement l'inverse de la règle Room.
         */
        private fun directBootStateOf(stored: DirectBootSnapshot.Active): BlockedPackagesState {
            val packages =
                stored.blockedPackages
                    .map { BlockedPackage(it.packageName, it.displayNameSnapshot) }
                    .toSet()
            return when (stored.state) {
                in FINAL_STATES -> {
                    BlockedPackagesState.Inactive
                }

                SessionStateDto.RELEASING -> {
                    BlockedPackagesState.Releasing(
                        sessionId = stored.sessionId,
                        effectivePackages =
                            if (isPendingInDirectBoot(stored, SessionEffectKindDto.REMOVE_BLOCKING)) {
                                packages
                            } else {
                                emptySet()
                            },
                    )
                }

                SessionStateDto.PREPARING -> {
                    if (isPendingInDirectBoot(stored, SessionEffectKindDto.APPLY_BLOCKING)) {
                        BlockedPackagesState.Inactive
                    } else {
                        BlockedPackagesState.Active(stored.sessionId, packages)
                    }
                }

                // `projectionToSnapshotDto` est l'alias d'import de `toSnapshotDto` côté Direct
                // Boot : les deux mappers exposent ce nom, et l'alias lève le conflit sans recopier
                // la règle de `isBlockingPending` d'un côté ou de l'autre.
                SessionStateDto.ARMED -> {
                    if (stored.projectionToSnapshotDto().isBlockingPending) {
                        BlockedPackagesState.Inactive
                    } else {
                        BlockedPackagesState.Active(stored.sessionId, packages)
                    }
                }

                else -> {
                    BlockedPackagesState.Active(stored.sessionId, packages)
                }
            }
        }

        private fun isPendingInDirectBoot(
            stored: DirectBootSnapshot.Active,
            kind: SessionEffectKindDto,
        ): Boolean = stored.pendingEffects.any { it.kind == kind }

        private companion object {
            /**
             * Miroir de `SESSION_FINAL_STATES` (`:core:system`, inaccessible ici :
             * `:core:database` ne dépend pas de `:core:system`, SPEC_ANDROID §6). Même motif
             * que `TechnicalEventLogScope`.
             */
            val FINAL_STATES =
                setOf(SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.FAILED)

            /**
             * `SATISFIED` compte comme réussi : SPEC_CORE_KMP §6 en fait une précondition déjà
             * atteinte, pas un échec — le retrait du blocage est satisfait d'office quand le
             * service d'accessibilité a déjà été désactivé par l'utilisateur (SPEC_ANDROID §11.3).
             */
            val SUCCEEDED_STATUSES = setOf(EffectStatus.SUCCEEDED, EffectStatus.SATISFIED)
        }
    }
