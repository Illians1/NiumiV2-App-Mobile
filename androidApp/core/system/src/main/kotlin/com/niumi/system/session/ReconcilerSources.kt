package com.niumi.system.session

import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.blocking.AccessibilityServiceStatus
import com.niumi.system.blocking.BlockedPackagesProjection

/**
 * Regroupe les trois sources natives lues par [SessionReconciler] pour décider d'une reprise
 * (SPEC_ANDROID §9.2, §9.3, §13.1). Un seul paramètre de constructeur plutôt que trois : la
 * décomposition en trois lectures distinctes reste dans le corps du réconciliateur, seule la
 * signature du constructeur en bénéficie (`LongParameterList` de detekt).
 */
data class ReconcilerSources(
    val alarmScheduler: AlarmScheduler,
    val accessibilityServiceStatus: AccessibilityServiceStatus,
    val blockedPackagesProjection: BlockedPackagesProjection,
)
