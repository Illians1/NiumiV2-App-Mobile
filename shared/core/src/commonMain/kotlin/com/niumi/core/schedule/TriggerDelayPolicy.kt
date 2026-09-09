package com.niumi.core.schedule

/**
 * Issue de la fenêtre de grâce Android (SPEC_CORE_KMP §8.2) : politique de reprogrammation
 * Android, calculée et testée ici mais sans équivalent iOS (iOS produit `TRIGGER_ELAPSED` dès
 * l'heure atteinte, faute d'API de reprogrammation sonore équivalente à `setAlarmClock()`).
 */
public enum class TriggerDelayOutcome {
    NOT_REACHED,
    FIRE_NOW,
    MISSED,
}

/**
 * Évalue le retard entre l'instant de déclenchement et l'horloge courante (SPEC_CORE_KMP §8.2) :
 * au-delà de 15 minutes, l'application produit `TRIGGER_ELAPSED` avec `MISSED_TRIGGER_WINDOW`.
 * L'horloge est reçue explicitement, jamais lue implicitement (§14).
 */
public object TriggerDelayPolicy {
    /** Fenêtre de grâce de 15 minutes, en millisecondes (SPEC_CORE_KMP §8.2). */
    public const val GRACE_WINDOW_MILLIS: Long = 15 * 60 * 1_000L

    public fun evaluate(
        triggerAtEpochMillis: Long,
        nowEpochMillis: Long,
    ): TriggerDelayOutcome {
        val delayMillis = nowEpochMillis - triggerAtEpochMillis
        return when {
            delayMillis < 0 -> TriggerDelayOutcome.NOT_REACHED
            delayMillis <= GRACE_WINDOW_MILLIS -> TriggerDelayOutcome.FIRE_NOW
            else -> TriggerDelayOutcome.MISSED
        }
    }
}
