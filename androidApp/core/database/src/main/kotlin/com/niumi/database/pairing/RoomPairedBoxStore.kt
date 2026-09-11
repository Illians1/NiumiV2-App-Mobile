package com.niumi.database.pairing

import androidx.room.withTransaction
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.NiumiDatabase
import com.niumi.database.directboot.UnlockState
import com.niumi.database.mapping.toDomain
import com.niumi.database.mapping.toEntity
import javax.inject.Provider

private const val ROOM_BEFORE_UNLOCK_MESSAGE = "ROOM_BEFORE_UNLOCK"

/**
 * Implémentation Room de [PairedBoxStore] (SPEC_ANDROID §11.1, étape 13). [replace] fait un
 * `DELETE` puis un `INSERT` dans une seule transaction : `OnConflictStrategy.REPLACE` de
 * [com.niumi.database.dao.PairedBoxDao.upsert] ne couvre que le remplacement d'une ligne portant
 * le même `boxId` ; une ré-association change presque toujours de boîtier, donc de clé primaire.
 *
 * [current] s'écarte volontairement du patron `ROOM_BEFORE_UNLOCK` strict de [RoomSessionStore][
 * com.niumi.database.RoomSessionStore] : `AndroidDeviceReadinessChecker` est ré-exécuté pendant la
 * réconciliation Direct Boot (raison `LOCKED_BOOT`, avant `UserManager.isUserUnlocked`), donc avant
 * tout déverrouillage. Lever comme `RoomSessionStore` ferait échouer cette réconciliation. Renvoyer
 * `null` avant déverrouillage est sans risque : le contrôle `PAIRED_BOX` n'est pas surveillé
 * pendant une session `ARMED` ([com.niumi.system.readiness.MonitoredReadinessChecks] ne le liste
 * pas), et SPEC_CORE_KMP §10 impose que toute vérification NFC d'une session déjà armée utilise le
 * credential figé à l'activation, jamais le dépôt courant. [replace] et [clear] conservent la
 * garde stricte : une association ne peut se produire qu'à l'écran dédié, hors session, donc
 * toujours après déverrouillage.
 */
class RoomPairedBoxStore(
    private val databaseProvider: Provider<NiumiDatabase>,
    private val unlockState: UnlockState,
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
) : PairedBoxStore {
    private val database: NiumiDatabase
        get() {
            check(unlockState.isUserUnlocked) { ROOM_BEFORE_UNLOCK_MESSAGE }
            return databaseProvider.get()
        }

    override suspend fun current(): PairedBoxCredentialDto? {
        if (!unlockState.isUserUnlocked) return null
        return databaseProvider
            .get()
            .pairedBoxDao()
            .current()
            ?.toDomain()
    }

    override suspend fun replace(credential: PairedBoxCredentialDto) {
        database.withTransaction {
            database.pairedBoxDao().deleteAll()
            database.pairedBoxDao().upsert(credential.toEntity(nowEpochMillis()))
        }
    }

    override suspend fun clear() {
        database.pairedBoxDao().deleteAll()
    }
}
