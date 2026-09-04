package com.niumi.system.intent

/**
 * Cible symbolique d'un `PendingIntent` interne. `:core:system` ne peut référencer aucune
 * classe de `:app` ni de `:feature:ringing` (SPEC_ANDROID §6) : la résolution vers un vrai
 * `ComponentName` est déléguée à [NiumiComponentResolver], implémenté dans `:app`, le seul
 * module qui voit l'ensemble du graphe de dépendances.
 */
enum class NiumiComponent {
    ALARM_RECEIVER,
    MAIN_ACTIVITY,
    ALARM_ACTIVITY,
}
