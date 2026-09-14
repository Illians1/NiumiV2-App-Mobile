package com.niumi.system.session

/** Déclencheurs de réconciliation (« Interfaces transverses » du plan MVP, SPEC_ANDROID §9.3, §13.1). */
enum class ReconcileReason {
    PROCESS_START,
    USER_UNLOCKED,
    LOCKED_BOOT,
    BOOT,
    PACKAGE_REPLACED,
    TIME_CHANGED,
    TIMEZONE_CHANGED,
    BEFORE_SCAN,
    SERVICE_RECREATED,

    /**
     * Passage de l'application au premier plan alors que la session attend un scan (§10.5,
     * décision de l'étape 19). Ajoutée sur une mesure : après un balayage de la notification
     * d'attente de scan, aucune des huit autres raisons ne survient pendant qu'on se sert du
     * téléphone — 523 s mesurées sans republication, et rien ne la ramenait avant le prochain
     * démarrage de processus. Un déverrouillage d'écran ordinaire n'y suffit pas non plus :
     * `ACTION_USER_UNLOCKED` n'est émis qu'au premier déverrouillage après démarrage.
     */
    FOREGROUND_AWAITING_SCAN,
}
