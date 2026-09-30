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
    /**
     * Le NFC peut-il être jugé honnêtement maintenant ? Faux avant le premier déverrouillage et
     * pendant les premières secondes après le démarrage : [nfcReady] y dit « désactivé » pour un
     * NFC dont la pile n'a simplement pas fini de démarrer (étape 25, [SessionNfcEvaluability]).
     * Distinct de [nfcReady], dont le sens ne change pas : « non jugeable » n'est pas « prêt ».
     */
    val nfcEvaluable: Boolean,
)
