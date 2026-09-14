package com.niumi.database.directboot

import com.niumi.database.SessionStore

/**
 * Réécrit la projection Direct Boot depuis Room (SPEC_ANDROID §9.2 : « Room fait foi, et
 * `SessionReconciler` réécrit la projection tant que `domainRevision` n'est pas régressée »).
 *
 * Fonction partagée plutôt que méthode privée : elle a deux appelants légitimes — chaque écriture
 * de `UnlockAwarePersistenceGateway` après déverrouillage, et la fin de la fusion
 * Direct Boot → Room (étape 19). La recopier aurait laissé deux projections susceptibles de
 * diverger.
 *
 * `null` quand aucune session active n'existe : il n'y a alors rien à projeter, et surtout rien à
 * effacer — la suppression du fichier est le geste explicite de `clearActive`, jamais un effet de
 * bord d'un miroir.
 *
 * N'embarque que les effets rejouables (`PENDING`/`FAILED`), ce que renvoie
 * [SessionStore.pendingEffects] : la projection de §7.3 s'appelle `pendingEffects` et un effet
 * terminal n'a plus rien à apporter avant déverrouillage.
 */
suspend fun mirrorActiveSessionToDirectBoot(
    sessionStore: SessionStore,
    directBootStore: DirectBootStore,
): DirectBootWriteResult? {
    val stored = sessionStore.activeSession() ?: return null
    return directBootStore.write(
        DirectBootMapper.projectionOf(
            snapshot = stored.snapshot,
            extras = stored.extras,
            receipts = sessionStore.receipts(stored.snapshot.sessionId),
            effects = sessionStore.pendingEffects(stored.snapshot.sessionId),
        ),
    )
}
