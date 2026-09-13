package com.niumi.system.session

import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.blocking.BlockedPackagesProjection
import com.niumi.system.readiness.SessionReadinessMonitor
import com.niumi.system.ringing.RingingController

/**
 * Regroupe ce que [SessionReconciler] consulte pour décider d'une reprise (SPEC_ANDROID §9.2,
 * §9.3, §13.1). Un seul paramètre de constructeur plutôt que plusieurs : la décomposition en
 * lectures distinctes reste dans le corps du réconciliateur, seule la signature du constructeur
 * en bénéficie (`LongParameterList` de detekt).
 *
 * `AccessibilityServiceStatus` en est sortie à l'étape 12, remplacée par [readinessMonitor] :
 * l'état du service d'accessibilité n'est plus lu isolément, il fait partie des six contrôles
 * que §13.1 surveille d'un seul tenant.
 *
 * [ringingController] rejoint le groupe à l'étape 17 : une session `RINGING` dont le service a
 * disparu avec son processus doit retrouver son son à la première réconciliation (§10.2).
 *
 * [snapshotPublisher] rejoint le groupe à l'étape 14 : la réconciliation est le seul chemin qui
 * relit la persistance après un redémarrage du processus, c'est donc à elle de réamorcer le flux
 * que l'interface observe (défaut mesuré sur appareil, voir `ETAPE-14.md`).
 */
data class ReconcilerSources(
    val alarmScheduler: AlarmScheduler,
    val blockedPackagesProjection: BlockedPackagesProjection,
    val readinessMonitor: SessionReadinessMonitor,
    val snapshotPublisher: SessionSnapshotPublisher,
    val ringingController: RingingController,
)
