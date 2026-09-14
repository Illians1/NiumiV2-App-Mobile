package com.niumi.app.poc

/**
 * Session fictive de la route POC (debug uniquement, SPEC_ANDROID §22 Lot 0). L'identifiant est
 * un UUID canonique fixe : `ServiceCommand.from` (`:feature:ringing`) refuse toute autre forme
 * quand `AlarmReceiver` reçoit le broadcast programmé par `AlarmScheduler`. Extrait de
 * `PocViewModel` à l'étape 4 : le handler NFC du POC, supprimé à l'étape 18, partageait cet
 * identifiant pour arrêter la sonnerie sur scan valide. Plus aucun scan ne l'arrête désormais —
 * cette session n'existe pas en base, et `HandleValidNfcUseCase` l'ignore. Toute la route POC
 * disparaît à l'étape 21.
 */
internal object PocSession {
    const val ID = "00000000-0000-4000-8000-000000000000"
    const val REVISION = 1L
}
