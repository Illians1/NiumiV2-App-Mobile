package com.niumi.database.pairing

import androidx.room.withTransaction
import com.niumi.core.diagnostics.PairedBoxLimit
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.NiumiDatabase
import com.niumi.database.directboot.UnlockState
import com.niumi.database.mapping.toDto
import com.niumi.database.mapping.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Provider

private const val ROOM_BEFORE_UNLOCK_MESSAGE = "ROOM_BEFORE_UNLOCK"
private const val NAME_INVALID_MESSAGE = "PAIRED_BOX_NAME_INVALID"

/**
 * Implémentation Room de [PairedBoxStore] (SPEC_ANDROID §11.1, étape 13, Lot 8). Chaque écriture
 * se fait dans une seule transaction : [add] compte les boîtiers avant d'insérer, pour qu'un
 * doublon ou un onzième boîtier ne laisse jamais de ligne partielle ; [remove] refuse le dernier
 * boîtier (décision Q22) et renumérote les positions restantes pour qu'elles restent contiguës à
 * partir de 0. C'est le dépôt qui tient
 * l'invariant de §7.2 sur `name` (1 à 30 caractères après `trim()`) : [add] et [rename] passent le
 * nom par [PairedBoxName.sanitize] avant tout accès à la base et écrivent la valeur nettoyée ; un
 * nom refusé lève `PAIRED_BOX_NAME_INVALID`, l'écran ayant la même fonction pour valider avant
 * d'appeler.
 *
 * [all] s'écarte volontairement du patron `ROOM_BEFORE_UNLOCK` strict de [RoomSessionStore][
 * com.niumi.database.RoomSessionStore] : `AndroidDeviceReadinessChecker` est ré-exécuté pendant la
 * réconciliation Direct Boot (raison `LOCKED_BOOT`, avant `UserManager.isUserUnlocked`), donc avant
 * tout déverrouillage. Lever comme `RoomSessionStore` ferait échouer cette réconciliation. Renvoyer
 * une liste vide avant déverrouillage est sans risque : le contrôle `PAIRED_BOX` n'est pas
 * surveillé pendant une session `ARMED` ([com.niumi.system.readiness.MonitoredReadinessChecks] ne
 * le liste pas), et la vérification d'un scan, qui lit ce dépôt (étape 43), n'a lieu que sur un
 * appareil déverrouillé (SPEC_ANDROID §4.4) : un scan verrouillé, impossible par conception, serait
 * refusé, jamais accepté à tort. [points] suit la même règle. [add], [rename] et [remove] conservent
 * la garde stricte : une association ne se fait que depuis l'écran dédié, donc après déverrouillage.
 */
class RoomPairedBoxStore(
    private val databaseProvider: Provider<NiumiDatabase>,
    private val unlockState: UnlockState,
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
) : PairedBoxStore {
    private val database: NiumiDatabase
        get() {
            requireUnlocked()
            return databaseProvider.get()
        }

    private fun requireUnlocked() = check(unlockState.isUserUnlocked) { ROOM_BEFORE_UNLOCK_MESSAGE }

    private fun sanitizedName(name: String): String =
        requireNotNull(PairedBoxName.sanitize(name)) { NAME_INVALID_MESSAGE }

    override suspend fun all(): List<PairedBoxDto> {
        if (!unlockState.isUserUnlocked) return emptyList()
        return databaseProvider
            .get()
            .pairedBoxDao()
            .all()
            .map { it.toDto() }
    }

    override val points: Flow<List<PairedBoxDto>> =
        flow {
            if (!unlockState.isUserUnlocked) {
                emit(emptyList())
            } else {
                emitAll(
                    databaseProvider
                        .get()
                        .pairedBoxDao()
                        .observe()
                        .map { boxes -> boxes.map { it.toDto() } },
                )
            }
        }

    override suspend fun add(
        credential: PairedBoxCredentialDto,
        name: String,
    ): PairedBoxAddResult {
        requireUnlocked()
        val cleanName = sanitizedName(name)
        return database.withTransaction {
            val dao = database.pairedBoxDao()
            val count = dao.count()
            when {
                dao.findById(credential.boxId) != null -> {
                    PairedBoxAddResult.AlreadyPaired
                }

                count >= PairedBoxLimit.MAX -> {
                    PairedBoxAddResult.LimitReached
                }

                else -> {
                    dao.upsert(credential.toEntity(cleanName, count, nowEpochMillis()))
                    PairedBoxAddResult.Added
                }
            }
        }
    }

    override suspend fun rename(
        boxId: String,
        name: String,
    ) {
        requireUnlocked()
        val cleanName = sanitizedName(name)
        database.withTransaction { database.pairedBoxDao().rename(boxId, cleanName) }
    }

    override suspend fun remove(boxId: String): PairedBoxRemoveResult =
        database.withTransaction {
            val dao = database.pairedBoxDao()
            when {
                dao.findById(boxId) == null -> {
                    PairedBoxRemoveResult.Unknown
                }

                dao.count() <= PairedBoxLimit.MIN -> {
                    PairedBoxRemoveResult.LastBoxKept
                }

                else -> {
                    dao.delete(boxId)
                    dao.all().forEachIndexed { index, box ->
                        if (box.position != index) dao.upsert(box.copy(position = index))
                    }
                    PairedBoxRemoveResult.Removed
                }
            }
        }
}
