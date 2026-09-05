package com.niumi.app.poc

/**
 * Session fictive de la route POC (debug uniquement, SPEC_ANDROID §22 Lot 0). L'identifiant est
 * un UUID canonique fixe : `ServiceCommand.from` (`:feature:ringing`) refuse toute autre forme
 * quand `AlarmReceiver` reçoit le broadcast programmé par `AlarmScheduler`. Extrait de
 * `PocViewModel` à l'étape 4 : `PocNfcScanHandler` a besoin du même identifiant pour arrêter
 * la sonnerie sur scan valide.
 */
internal object PocSession {
    const val ID = "00000000-0000-4000-8000-000000000000"
    const val REVISION = 1L
}
