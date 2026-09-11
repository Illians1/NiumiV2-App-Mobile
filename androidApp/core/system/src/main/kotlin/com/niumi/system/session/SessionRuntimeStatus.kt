package com.niumi.system.session

/**
 * Diagnostic des sous-systèmes Android, distinct de `SessionState` (SPEC_ANDROID §7.1). Sert au
 * diagnostic et à la réconciliation ; ne remplace jamais l'état métier.
 */
data class SessionRuntimeStatus(
    val alarmScheduled: Boolean,
    val accessibilityReady: Boolean,
    val notificationReady: Boolean,
    val fullScreenReady: Boolean,
    val nfcReady: Boolean,
    val audioReady: Boolean,
)
