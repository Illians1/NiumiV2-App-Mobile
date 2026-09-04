package com.niumi.feature.ringing.ui

/**
 * Phase atteignable sur Android par `AlarmActivity` (SPEC_ANDROID §10.4). Reflète les états
 * communs `RINGING`, `TRIGGERED_AWAITING_NFC` et `AWAITING_NFC` qui n'existent pas encore côté
 * moteur (Phase C) : cet enum local anticipe leur mapping vers l'écran, remplacé par un
 * mapping direct depuis `SessionSnapshotDto` à l'étape 17.
 */
enum class AlarmRingingPhase {
    RINGING,
    TRIGGERED_AWAITING_NFC,
    AWAITING_NFC,
}

/** État d'affichage pur de `AlarmScreen`, sans dépendance à Compose ni à Android. */
data class AlarmScreenState(
    val phase: AlarmRingingPhase,
    val deviceLocked: Boolean,
    val instructionText: String,
) {
    companion object {
        fun from(
            phase: AlarmRingingPhase,
            deviceLocked: Boolean,
        ): AlarmScreenState =
            AlarmScreenState(
                phase = phase,
                deviceLocked = deviceLocked,
                instructionText = if (deviceLocked) LOCKED_TEXT else textFor(phase),
            )

        private fun textFor(phase: AlarmRingingPhase): String =
            when (phase) {
                AlarmRingingPhase.RINGING -> {
                    "Scanne ton boîtier Niumi pour arrêter l'alarme."
                }

                AlarmRingingPhase.TRIGGERED_AWAITING_NFC -> {
                    "L'heure de ton réveil est passée. Scanne ton boîtier Niumi pour débloquer tes applications."
                }

                AlarmRingingPhase.AWAITING_NFC -> {
                    "Le son est arrêté, mais tes applications restent bloquées. " +
                        "Scanne ton boîtier Niumi pour terminer la session."
                }
            }

        private const val LOCKED_TEXT = "Déverrouille ton téléphone, puis approche-le du boîtier."
    }
}
