package com.niumi.database.directboot

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.niumi.database.EffectStatus
import com.niumi.database.NiumiDatabase
import com.niumi.database.SessionStoreUnreadableException
import com.niumi.database.entity.AlarmSessionEntity
import com.niumi.database.entity.SessionEffectOutboxEntity
import com.niumi.database.mapping.toEntity
import javax.inject.Provider

/**
 * Statuts terminaux d'un effet : une fois atteints, ils ne redeviennent jamais rejouables.
 * `SATISFIED` en fait partie — la précondition avait disparu, il n'y a rien à refaire
 * (SPEC_CORE_KMP §6.1).
 */
private val TERMINAL_STATUSES = setOf(EffectStatus.SUCCEEDED, EffectStatus.SATISFIED)

/** Résultat d'une fusion Direct Boot → Room. */
sealed interface DirectBootMergeResult {
    /**
     * Fusion appliquée. Les trois compteurs décrivent ce qui a réellement changé dans Room et
     * valent tous zéro quand la projection ne portait rien de neuf — le cas ordinaire, puisque
     * `mirrorActiveSessionToDirectBoot` la réécrit depuis Room après chaque décision.
     */
    data class Merged(
        val sessionAdvanced: Boolean,
        val receiptsInserted: Int,
        val effectsInserted: Int,
        val effectsAdvanced: Int,
    ) : DirectBootMergeResult

    /**
     * `domainRevision` de la projection strictement inférieure à la `revision` Room pour la même
     * session (SPEC_ANDROID §7.3) : Room a continué sans elle, la projection est périmée. Rien
     * n'est écrit ; l'appelant réécrit la projection depuis Room.
     */
    data object StaleRevision : DirectBootMergeResult

    /**
     * La session de la projection n'existe pas dans Room. Aucun reçu ni effet ne peut y être
     * inséré — ils portent une clé étrangère vers `alarm_session` — et rien ne permet de
     * reconstruire la session : une activation a toujours lieu appareil déverrouillé
     * (SPEC_ANDROID §9.2), donc ce cas ne décrit qu'une base effacée ou une projection orpheline.
     * Rien n'est écrit, et surtout rien n'est supprimé.
     */
    data object UnknownSession : DirectBootMergeResult
}

/**
 * Fusion idempotente du registre et de l'outbox Direct Boot dans Room, au déverrouillage
 * (SPEC_ANDROID §9.3, dernier alinéa ; SPEC_CORE_KMP §13).
 *
 * Interface dédiée plutôt qu'une méthode de plus sur `SessionStore` : cette classe est au plafond
 * `TooManyFunctions` de detekt (11) depuis l'étape 11, et la fusion n'entre dans aucune transaction
 * de décision. Même précédent que `RoomSessionIncidentsReader` (étape 15).
 */
interface DirectBootRoomMerge {
    suspend fun merge(projection: DirectBootSnapshot.Active): DirectBootMergeResult
}

/**
 * Tout se joue dans une seule transaction : une fusion à moitié appliquée laisserait Room avec un
 * snapshot avancé mais sans les effets qui le justifient, donc une session que la réconciliation
 * suivante croirait terminée.
 *
 * [databaseProvider] et la garde [UnlockState], mêmes motifs que `RoomSessionStore` : la fusion
 * n'a de sens qu'appareil déverrouillé et ne doit jamais faire construire la base avant
 * (SPEC_ANDROID §7.3). Construite par `@Provides` et non `@Inject`, pour la même raison que
 * `RoomSessionStore` : [nowEpochMillis] a une valeur par défaut, invisible de Dagger.
 */
class RoomDirectBootMerge(
    private val unlockState: UnlockState,
    private val databaseProvider: Provider<NiumiDatabase>,
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
) : DirectBootRoomMerge {
    private val database: NiumiDatabase
        get() {
            check(unlockState.isUserUnlocked) { "ROOM_BEFORE_UNLOCK" }
            return databaseProvider.get()
        }

    // `SQLiteException` traduite comme dans `RoomSessionStore.activeSession()` : aucune exception
    // SQLite brute ne sort de `:core:database`. **Défaut mesuré sur appareil le 2026-09-15** : la
    // fusion touche Room *avant* le `gateway.load()` de la passe, et une base illisible y faisait
    // planter le processus à chaque démarrage — donc une boucle de plantage, et l'écran de
    // diagnostic que §18 promet jamais atteint. La garde `ROOM_BEFORE_UNLOCK`
    // (`IllegalStateException`) continue, elle, de remonter : c'est un défaut de programmation.
    override suspend fun merge(projection: DirectBootSnapshot.Active): DirectBootMergeResult =
        try {
            database.withTransaction {
                val existing =
                    database.sessionDao().findById(projection.sessionId)
                        ?: return@withTransaction DirectBootMergeResult.UnknownSession
                if (projection.domainRevision < existing.revision) {
                    return@withTransaction DirectBootMergeResult.StaleRevision
                }
                applyMerge(projection, existing)
            }
        } catch (exception: SQLiteException) {
            throw SessionStoreUnreadableException(exception.message ?: "ROOM_UNREADABLE", exception)
        }

    private suspend fun applyMerge(
        projection: DirectBootSnapshot.Active,
        existing: AlarmSessionEntity,
    ): DirectBootMergeResult.Merged {
        val sessionAdvanced = projection.domainRevision > existing.revision
        if (sessionAdvanced) {
            // `upsert`, jamais `INSERT OR REPLACE` : celui-ci déclencherait les CASCADE des cinq
            // tables enfants et effacerait le journal de la session (régression mesurée à
            // l'étape 11, couverte par `RoomSessionStoreHistoryTest`).
            //
            // Les quatre champs figés à l'activation (SPEC_ANDROID §7.2) sont repris de Room et
            // non de la projection : c'est Room qui les a figés, et la fenêtre Direct Boot ne
            // peut pas les modifier. Les applications bloquées ne sont pas réécrites non plus —
            // elles sont figées de la même façon et les réécrire ferait du bruit de CASCADE.
            database.sessionDao().upsert(
                projection.toSnapshotDto().toEntity(
                    projection.toExtras().copy(
                        boxId = existing.boxId,
                        boxTokenSha256Hex = existing.boxTokenSha256Hex,
                        ringtoneKey = existing.ringtoneKey,
                        vibrationEnabled = existing.vibrationEnabled,
                    ),
                ),
            )
        }
        return DirectBootMergeResult.Merged(
            sessionAdvanced = sessionAdvanced,
            receiptsInserted = mergeReceipts(projection),
            effectsInserted = mergeEffects(projection),
            effectsAdvanced = advanceEffectStatuses(projection),
        )
    }

    private suspend fun mergeReceipts(projection: DirectBootSnapshot.Active): Int {
        val known = database.receiptDao().forSession(projection.sessionId).mapTo(mutableSetOf()) { it.eventId }
        val missing = projection.toReceipts().filterNot { it.eventId in known }
        if (missing.isEmpty()) return 0
        database.receiptDao().insertAllIgnoringDuplicates(missing.map { it.toEntity() })
        return missing.size
    }

    private suspend fun mergeEffects(projection: DirectBootSnapshot.Active): Int {
        val known = database.outboxDao().forSession(projection.sessionId).mapTo(mutableSetOf()) { it.effectId }
        val missing = projection.toPendingEffects().filterNot { it.effectId in known }
        if (missing.isEmpty()) return 0
        database.outboxDao().insertAll(missing.map { it.toEntity(nowEpochMillis()) })
        return missing.size
    }

    /**
     * Un effet exécuté pendant la fenêtre Direct Boot est terminal dans la projection et encore
     * `PENDING`/`FAILED` dans Room : le rejouer au déverrouillage referait le travail. La
     * remontée ne va jamais dans l'autre sens — un effet déjà terminal dans Room ne redevient
     * pas rejouable parce qu'une projection plus ancienne le croyait en attente.
     */
    private suspend fun advanceEffectStatuses(projection: DirectBootSnapshot.Active): Int {
        val terminalInProjection =
            projection.toPendingEffects().filter { it.status in TERMINAL_STATUSES }.associateBy { it.effectId }
        if (terminalInProjection.isEmpty()) return 0
        val advanced =
            database.outboxDao().forSession(projection.sessionId).filter { room: SessionEffectOutboxEntity ->
                room.status !in TERMINAL_STATUSES && room.effectId in terminalInProjection
            }
        advanced.forEach { room ->
            val fromProjection = terminalInProjection.getValue(room.effectId)
            database.outboxDao().updateStatus(
                effectId = room.effectId,
                status = fromProjection.status,
                error = fromProjection.lastError,
                updatedAtEpochMillis = nowEpochMillis(),
            )
        }
        return advanced.size
    }
}
