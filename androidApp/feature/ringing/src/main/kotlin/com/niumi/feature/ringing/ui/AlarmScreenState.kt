package com.niumi.feature.ringing.ui

import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanOutcome

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

/**
 * État d'affichage pur de `AlarmScreen`, sans dépendance à Compose ni à Android. Le texte
 * affiché suit un ordre de priorité, du plus bloquant au moins bloquant (SPEC_ANDROID §11.2,
 * §4.4, §10.4) : matériel NFC absent, NFC désactivé, téléphone verrouillé, résultat d'un scan
 * récent, puis le texte de phase par défaut. Les rangs « matériel absent », « NFC désactivé »
 * et « boîtier inconnu » ne sont pas des textes imposés mot pour mot par la spec — rédactions
 * du plan, consignées dans ETAPE-04.md.
 */
data class AlarmScreenState(
    val phase: AlarmRingingPhase,
    val deviceLocked: Boolean,
    val nfcAvailability: NfcAvailability,
    val lastScanOutcome: ScanOutcome?,
    val instructionText: String,
    val showsNfcSettingsShortcut: Boolean,
) {
    companion object {
        fun from(
            phase: AlarmRingingPhase,
            deviceLocked: Boolean,
            nfcAvailability: NfcAvailability = NfcAvailability.ENABLED,
            lastScanOutcome: ScanOutcome? = null,
        ): AlarmScreenState =
            AlarmScreenState(
                phase = phase,
                deviceLocked = deviceLocked,
                nfcAvailability = nfcAvailability,
                lastScanOutcome = lastScanOutcome,
                instructionText = instructionText(phase, deviceLocked, nfcAvailability, lastScanOutcome),
                showsNfcSettingsShortcut = nfcAvailability == NfcAvailability.DISABLED,
            )

        private fun instructionText(
            phase: AlarmRingingPhase,
            deviceLocked: Boolean,
            nfcAvailability: NfcAvailability,
            lastScanOutcome: ScanOutcome?,
        ): String =
            when {
                nfcAvailability == NfcAvailability.ABSENT -> ABSENT_TEXT
                nfcAvailability == NfcAvailability.DISABLED -> DISABLED_TEXT
                deviceLocked -> LOCKED_TEXT
                lastScanOutcome == ScanOutcome.Unreadable -> UNREADABLE_TEXT
                lastScanOutcome == ScanOutcome.UnknownBox -> UNKNOWN_BOX_TEXT
                else -> textFor(phase)
            }

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

        // SPEC_ANDROID §11.2, mot pour mot.
        private const val LOCKED_TEXT = "Déverrouille ton téléphone, puis approche-le du boîtier."
        private const val UNREADABLE_TEXT = "Boîtier non reconnu. Réessaie."

        // Rédactions du plan, non imposées mot pour mot par la spec (ETAPE-04.md).
        private const val ABSENT_TEXT = "Cet appareil ne prend pas en charge le NFC."
        private const val DISABLED_TEXT = "Le NFC est désactivé. Active-le pour scanner ton boîtier."
        private const val UNKNOWN_BOX_TEXT = "Ce boîtier n'est pas celui de ta session."
    }
}
