package com.niumi.feature.ringing

import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.audio.VibrationController
import com.niumi.system.nfc.NfcScanHandler
import com.niumi.system.nfc.ScanOutcome

/**
 * Traite un scan NFC reçu par `AlarmActivity` (SPEC_ANDROID §11.2, §11.3) : garde de
 * réentrance, journal technique, vibration d'erreur. Extrait de `AlarmActivity` pour rester
 * sous le seuil detekt `TooManyFunctions` (même motif que `SystemModule`/`AudioModule`,
 * ETAPE-03.md, décision 6). Toutes ses méthodes doivent être appelées depuis le dispatcher
 * principal : [processingScan] n'est protégé par aucune synchronisation.
 */
class AlarmNfcScanCoordinator(
    private val vibrationController: VibrationController,
    private val technicalEventLog: TechnicalEventLog,
) {
    private var processingScan = false

    /**
     * `null` si un scan est déjà en cours de traitement ou si [scanHandler] est absent
     * (avant l'étape 18 en release) : dans les deux cas, aucune décision n'est prise et
     * l'écran ne doit pas changer.
     */
    suspend fun handleUri(
        scanHandler: NfcScanHandler?,
        uri: String,
    ): ScanOutcome? {
        if (processingScan || scanHandler == null) return null
        processingScan = true
        val outcome =
            try {
                scanHandler.onUriRead(uri)
            } finally {
                processingScan = false
            }
        recordOutcome(outcome)
        return outcome
    }

    fun handleUnreadable(): ScanOutcome {
        recordOutcome(ScanOutcome.Unreadable)
        return ScanOutcome.Unreadable
    }

    private fun recordOutcome(outcome: ScanOutcome) {
        when (outcome) {
            ScanOutcome.Accepted -> {
                technicalEventLog.log(TechnicalEventType.NFC_SCAN_VALID)
            }

            ScanOutcome.UnknownBox -> {
                technicalEventLog.log(TechnicalEventType.NFC_SCAN_INVALID)
                vibrationController.vibrateError()
            }

            ScanOutcome.Unreadable -> {
                technicalEventLog.log(TechnicalEventType.NFC_SCAN_INVALID)
            }

            ScanOutcome.Ignored -> {}
        }
    }
}
