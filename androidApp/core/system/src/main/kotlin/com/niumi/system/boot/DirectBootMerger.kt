package com.niumi.system.boot

import com.niumi.database.SessionStore
import com.niumi.database.directboot.DirectBootMergeResult
import com.niumi.database.directboot.DirectBootRoomMerge
import com.niumi.database.directboot.DirectBootSnapshot
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.directboot.UnlockState
import com.niumi.database.directboot.mirrorActiveSessionToDirectBoot

/** Ce qu'une tentative de fusion a fait, pour le journal de réconciliation et les tests. */
sealed interface DirectBootMergeOutcome {
    /** Appareil encore verrouillé, ou aucune projection à fusionner. Aucun accès à Room. */
    data object NothingToMerge : DirectBootMergeOutcome

    /**
     * Projection illisible (SPEC_CORE_KMP §13, « corruption traitée explicitement »). Rien n'est
     * fusionné et **rien n'est effacé** : `SessionReconciler` produit `SnapshotCorrupted` sur la
     * passe qui suit, et Room reste la source intacte.
     */
    data class Corrupted(
        val reason: String,
    ) : DirectBootMergeOutcome

    /** Fusion tentée ; [result] dit ce que Room a réellement absorbé. */
    data class Merged(
        val result: DirectBootMergeResult,
    ) : DirectBootMergeOutcome
}

/**
 * Fusion idempotente de la projection Direct Boot dans Room au déverrouillage, puis réécriture de
 * la projection depuis Room (SPEC_ANDROID §9.3, dernier alinéa ; §9.2).
 *
 * **Pourquoi elle est indispensable.** Entre un redémarrage et le premier déverrouillage, le
 * coordinateur décide sur la seule projection Direct Boot : `TRIGGER_ELAPSED`, l'incident
 * `MISSED_TRIGGER_WINDOW`, les reçus d'événements et les effets exécutés n'existent que là. Room,
 * resté à l'état d'avant le redémarrage, redeviendrait la source canonique au déverrouillage et
 * effacerait ces décisions.
 *
 * **Effet de bord voulu :** les effets `RECORD_INCIDENT` restés rejouables faute de Room
 * (`INCIDENT_DEFERRED_UNTIL_UNLOCK` dans [com.niumi.system.session.UnlockAwarePersistenceGateway])
 * rejoignent l'outbox Room et sont rejoués par la passe de réconciliation qui suit immédiatement.
 * C'est ce qui referme la boucle des incidents différés.
 *
 * **Quand elle tourne.** Sur `USER_UNLOCKED`, `BOOT` et `PROCESS_START`, appelée par
 * [com.niumi.system.session.DefaultSessionCoordinator] sous son mutex, avant le réconciliateur.
 * `USER_UNLOCKED` est le signal de §9.3 mais n'est délivré qu'à un receiver enregistré à chaud, donc
 * seulement si le processus était vivant à cet instant ; les deux autres raisons sont le filet pour
 * le cas contraire. La fusion étant idempotente et bornée à la lecture d'un fichier quand il n'y a
 * rien à faire, la tenter trois fois ne coûte rien.
 *
 * **Rien n'est journalisé.** §17 est une liste fermée de 26 types et aucun ne décrit une fusion de
 * projection. Le fait reste observable autrement : ce que la fusion a absorbé ressort dans les
 * événements que la passe de réconciliation suivante produit en rejouant les effets. Même
 * raisonnement que le dépassement de fenêtre d'`AlarmReceiver` (étape 17).
 */
class DirectBootMerger(
    private val unlockState: UnlockState,
    private val directBootStore: DirectBootStore,
    private val sessionStore: SessionStore,
    private val roomMerge: DirectBootRoomMerge,
) {
    suspend fun merge(): DirectBootMergeOutcome {
        if (!unlockState.isUserUnlocked) return DirectBootMergeOutcome.NothingToMerge
        return when (val projection = directBootStore.read()) {
            null -> DirectBootMergeOutcome.NothingToMerge
            is DirectBootSnapshot.Corrupted -> DirectBootMergeOutcome.Corrupted(projection.reason)
            is DirectBootSnapshot.Active -> DirectBootMergeOutcome.Merged(mergeActive(projection))
        }
    }

    private suspend fun mergeActive(projection: DirectBootSnapshot.Active): DirectBootMergeResult {
        val result = roomMerge.merge(projection)
        // Réécriture systématique, y compris sur `StaleRevision` : c'est précisément le cas où la
        // projection est en retard sur Room et doit être remise à niveau (§9.2). Sur
        // `UnknownSession`, Room n'a pas la session et `mirrorActiveSessionToDirectBoot` ne trouve
        // rien à projeter : il ne touche alors à rien, ce qui est le comportement voulu — la
        // projection orpheline reste lisible pour un diagnostic.
        mirrorActiveSessionToDirectBoot(sessionStore, directBootStore)
        return result
    }
}
