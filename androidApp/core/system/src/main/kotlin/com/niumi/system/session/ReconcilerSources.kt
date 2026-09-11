package com.niumi.system.session

import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.blocking.BlockedPackagesProjection
import com.niumi.system.readiness.SessionReadinessMonitor

/**
 * Regroupe ce que [SessionReconciler] consulte pour décider d'une reprise (SPEC_ANDROID §9.2,
 * §9.3, §13.1). Un seul paramètre de constructeur plutôt que plusieurs : la décomposition en
 * lectures distinctes reste dans le corps du réconciliateur, seule la signature du constructeur
 * en bénéficie (`LongParameterList` de detekt).
 *
 * `AccessibilityServiceStatus` en est sortie à l'étape 12, remplacée par [readinessMonitor] :
 * l'état du service d'accessibilité n'est plus lu isolément, il fait partie des six contrôles
 * que §13.1 surveille d'un seul tenant.
 */
data class ReconcilerSources(
    val alarmScheduler: AlarmScheduler,
    val blockedPackagesProjection: BlockedPackagesProjection,
    val readinessMonitor: SessionReadinessMonitor,
)
