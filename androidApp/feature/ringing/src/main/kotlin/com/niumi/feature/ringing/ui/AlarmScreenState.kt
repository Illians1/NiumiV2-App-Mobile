package com.niumi.feature.ringing.ui

import com.niumi.core.interop.SessionStateDto
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanOutcome

/**
 * État d'affichage pur de `AlarmScreen`, sans dépendance à Compose ni à Android. Depuis l'étape
 * 17, il est indexé par [SessionStateDto] — l'enum local `AlarmRingingPhase` qui l'anticipait a
 * disparu : l'écran reflète désormais l'état réel du moteur, jamais une phase devinée.
 *
 * Le texte affiché suit un ordre de priorité (SPEC_ANDROID §11.2, §4.4, §10.4) : nettoyage en
 * cours, matériel NFC absent, NFC désactivé, téléphone verrouillé, résultat d'un scan récent, puis
 * le texte d'état. `RELEASING` passe **avant** tous les rangs NFC : le scan a déjà eu lieu, et
 * demander de déverrouiller ou d'approcher le boîtier n'y décrirait plus rien.
 *
 * Les rangs « matériel absent », « NFC désactivé », « boîtier inconnu » et le texte de nettoyage
 * ne sont pas imposés mot pour mot par la spec — rédactions du plan (ETAPE-04.md, ETAPE-17.md).
 */
data class AlarmScreenState(
    val sessionState: SessionStateDto,
    val deviceLocked: Boolean,
    val nfcAvailability: NfcAvailability,
    val lastScanOutcome: ScanOutcome?,
    val instructionText: String,
    val showsNfcSettingsShortcut: Boolean,
    val releaseSteps: List<ReleaseStep>,
) {
    companion object {
        fun from(
            sessionState: SessionStateDto,
            deviceLocked: Boolean,
            nfcAvailability: NfcAvailability = NfcAvailability.ENABLED,
            lastScanOutcome: ScanOutcome? = null,
            releaseSteps: List<ReleaseStep> = emptyList(),
        ): AlarmScreenState =
            AlarmScreenState(
                sessionState = sessionState,
                deviceLocked = deviceLocked,
                nfcAvailability = nfcAvailability,
                lastScanOutcome = lastScanOutcome,
                instructionText = instructionText(sessionState, deviceLocked, nfcAvailability, lastScanOutcome),
                // Pendant le nettoyage, plus rien à scanner : le raccourci NFC n'aurait pas d'objet.
                showsNfcSettingsShortcut =
                    nfcAvailability == NfcAvailability.DISABLED && sessionState != SessionStateDto.RELEASING,
                releaseSteps = releaseSteps,
            )

        private fun instructionText(
            sessionState: SessionStateDto,
            deviceLocked: Boolean,
            nfcAvailability: NfcAvailability,
            lastScanOutcome: ScanOutcome?,
        ): String =
            when {
                sessionState == SessionStateDto.RELEASING -> RELEASING_TEXT
                nfcAvailability == NfcAvailability.ABSENT -> ABSENT_TEXT
                nfcAvailability == NfcAvailability.DISABLED -> DISABLED_TEXT
                deviceLocked -> LOCKED_TEXT
                lastScanOutcome == ScanOutcome.Unreadable -> UNREADABLE_TEXT
                lastScanOutcome == ScanOutcome.UnknownBox -> UNKNOWN_BOX_TEXT
                else -> textFor(sessionState)
            }

        /**
         * Les quatre états affichables de §10.4. Les cinq autres ne construisent jamais de
         * [AlarmScreenState] — [AlarmUiState.forSession] les traite en fermeture ou en sortie —
         * mais le `when` reste exhaustif pour qu'un état ajouté à la machine commune force une
         * décision explicite ici.
         */
        private fun textFor(sessionState: SessionStateDto): String =
            when (sessionState) {
                SessionStateDto.RINGING -> {
                    "Scanne ton boîtier Niumi pour arrêter l'alarme."
                }

                SessionStateDto.TRIGGERED_AWAITING_NFC -> {
                    "L'heure de ton réveil est passée. Scanne ton boîtier Niumi pour débloquer tes applications."
                }

                SessionStateDto.AWAITING_NFC -> {
                    "Le son est arrêté, mais tes applications restent bloquées. " +
                        "Scanne ton boîtier Niumi pour terminer la session."
                }

                SessionStateDto.RELEASING -> {
                    RELEASING_TEXT
                }

                SessionStateDto.PREPARING,
                SessionStateDto.ARMED,
                SessionStateDto.COMPLETED,
                SessionStateDto.CANCELLED,
                SessionStateDto.FAILED,
                -> {
                    ""
                }
            }

        // SPEC_ANDROID §10.4 et §11.2, mot pour mot.
        private const val LOCKED_TEXT = "Déverrouille ton téléphone, puis approche-le du boîtier."
        private const val UNREADABLE_TEXT = "Boîtier non reconnu. Réessaie."

        // Rédactions du plan, non imposées mot pour mot par la spec (ETAPE-04.md, ETAPE-17.md).
        private const val ABSENT_TEXT = "Cet appareil ne prend pas en charge le NFC."
        private const val DISABLED_TEXT = "Le NFC est désactivé. Active-le pour scanner ton boîtier."
        private const val UNKNOWN_BOX_TEXT = "Ce boîtier n'est pas celui de ta session."
        private const val RELEASING_TEXT = "Ton scan est validé. Niumi termine la session."
    }
}
