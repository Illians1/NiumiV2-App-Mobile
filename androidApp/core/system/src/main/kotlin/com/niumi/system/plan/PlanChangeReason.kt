package com.niumi.system.plan

/**
 * Pourquoi le planning est réappliqué (étape 44). Les cinq premières viennent de l'accueil
 * (étape 45) ; les autres, du coordinateur : démarrage du processus ou déverrouillage sans session
 * armée, déplacement de l'horloge ou du fuseau avant le coucher (décision Q9 du 2026-10-24, étendue
 * à l'horloge le même jour), fin d'une session (`COMPLETED`, ou `DISARMED` repris par le
 * réconciliateur).
 */
enum class PlanChangeReason {
    SWITCH,
    DAYS,
    WAKE_TIME,
    BEDTIME,
    APP_SELECTION,
    PROCESS_START,
    USER_UNLOCKED,
    TIME_CHANGED,
    TIMEZONE_CHANGED,
    SESSION_FINISHED,
}
