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
     * Passage de l'application au premier plan (§10.5, décision de l'étape 19 ; élargie à
     * l'étape 20). Deux faits distincts partagent cette raison :
     *
     * - la session attend un scan : republie la notification d'attente, seul rappel visible une
     *   fois l'écran de réveil fermé. Ajoutée sur une mesure — après un balayage de cette
     *   notification, aucune des huit autres raisons ne survient pendant qu'on se sert du
     *   téléphone (523 s mesurées sans republication), et un déverrouillage d'écran ordinaire n'y
     *   suffit pas non plus (`ACTION_USER_UNLOCKED` n'est émis qu'au premier déverrouillage après
     *   démarrage) ;
     * - `SessionSnapshotPublisher` n'a encore rien publié dans ce processus : un `onResume` peut
     *   survenir avant que la réconciliation de démarrage n'ait rien lu (processus recréé après
     *   une mort pendant `RINGING`, étape 20). Sans reconciliation ici, `SessionReadinessWatcher`
     *   sortait silencieusement (`publisher.snapshot.value == null → return`) et ne découvrait
     *   jamais la session.
     */
    FOREGROUND,

    /**
     * Tic de l'alarme de secours pendant `RINGING` (§10.2, étape 20). Émise par
     * `RingingWatchdogReceiver`, elle ne fait que rejouer une passe ordinaire : c'est
     * `resumeRinging` qui ranime le son si la session est toujours `RINGING`, et la politique du
     * watchdog qui réarme le prochain tic ou se désarme si l'état a changé entre-temps.
     */
    RINGING_WATCHDOG,
}
