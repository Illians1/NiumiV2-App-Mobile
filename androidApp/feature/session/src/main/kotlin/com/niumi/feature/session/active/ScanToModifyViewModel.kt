package com.niumi.feature.session.active

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.audio.VibrationController
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader
import com.niumi.system.nfc.NfcScanHandler
import com.niumi.system.nfc.ScanOutcome
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * État de l'écran 9. [isCancelled] et [isCompleted] ne sont vrais qu'après un état final **observé
 * sur le snapshot publié**, jamais sur le seul [ScanOutcome.Accepted] : SPEC_ANDROID §11.3 interdit
 * de présenter la session comme terminée avant `RELEASE_SUCCEEDED`, et §4 rappelle que `RELEASING`
 * autorise un nettoyage partiel.
 *
 * **[isCompleted] ajouté à l'étape 18, sur un défaut mesuré sur appareil.** Un scan depuis cet écran
 * ne pouvait jusque-là donner que `CANCELLED`, la session étant forcément `ARMED` avant l'heure —
 * l'écran n'avait donc qu'une sortie. Le correctif de `SessionReconciler` (garde de permission levée
 * pour `BEFORE_SCAN`) rend atteignable `ARMED` après l'heure → `TRIGGER_ELAPSED` →
 * `TRIGGERED_AWAITING_NFC` → `COMPLETED`, et l'écran restait alors bloqué sur « Scan requis » alors
 * que la session était terminée.
 */
data class ScanToModifyUiState(
    val availability: NfcAvailability = NfcAvailability.ENABLED,
    val lastOutcome: ScanOutcome? = null,
    val isReleasing: Boolean = false,
    val isCancelled: Boolean = false,
    val isCompleted: Boolean = false,
)

/**
 * Écran 9 : scan requis pour modifier ou annuler (SPEC_ANDROID §15, §11.2, §11.3). Le ViewModel ne
 * décide jamais de la validité d'un payload — il délègue entièrement à [scanHandler]
 * (SPEC_CORE_KMP §9.3), comme `AlarmActivity` le fait déjà pour la sonnerie.
 *
 * [scanHandler] est `HandleValidNfcUseCase` depuis l'étape 18, seule liaison de [NfcScanHandler] :
 * le qualificatif qui protégeait cet écran du handler POC n'a plus lieu d'être, les deux
 * implémentations concurrentes ayant disparu.
 *
 * Aucune écriture directe : ni `AppSelectionStore`, ni `PairedBoxStore`, ni `SessionCoordinator`
 * ne sont touchés ici. Une modification de sélection ou de boîtier pendant une session est
 * interdite en amont (SPEC_CORE_KMP §2 point 11, §4) ; seule la libération écrit, et elle passe
 * entièrement par [scanHandler].
 */
@HiltViewModel
class ScanToModifyViewModel
    @Inject
    constructor(
        private val nfcReader: NfcReader,
        private val scanHandler: NfcScanHandler,
        private val vibrationController: VibrationController,
        private val technicalEventLog: TechnicalEventLog,
        private val snapshotPublisher: SessionSnapshotPublisher,
    ) : ViewModel() {
        var state by mutableStateOf(ScanToModifyUiState(availability = nfcReader.availability))
            private set

        /**
         * Garde de réentrance : un tag posé émet plusieurs lectures, et sans elle un même scan
         * serait traité en parallèle. Même patron qu'`AlarmNfcScanCoordinator` — non synchronisée,
         * donc à n'appeler que depuis le dispatcher principal, ce que fait la `Route`.
         */
        private var processingScan = false

        init {
            viewModelScope.launch {
                snapshotPublisher.snapshot.collect { snapshot ->
                    state =
                        state.copy(
                            isReleasing = snapshot?.state == SessionStateDto.RELEASING,
                            isCancelled = snapshot?.state == SessionStateDto.CANCELLED,
                            isCompleted = snapshot?.state == SessionStateDto.COMPLETED,
                        )
                }
            }
        }

        fun refreshAvailability() {
            state = state.copy(availability = nfcReader.availability)
            if (state.availability == NfcAvailability.DISABLED) {
                technicalEventLog.log(TechnicalEventType.NFC_DISABLED)
            }
        }

        /**
         * L'`Activity` est reçue en paramètre et jamais conservée (même règle que
         * `PairingViewModel`) : un ViewModel survit à l'`Activity`, la retenir serait une fuite.
         *
         * Les deux callbacks arrivent sur un thread binder (voir `ReaderModeNfcReader`) : ils sont
         * renvoyés sur `viewModelScope`, confiné au dispatcher principal, ce qui est la condition
         * de validité de la garde [processingScan] et des écritures de [state].
         */
        fun startReaderMode(activity: Activity) {
            if (nfcReader.availability != NfcAvailability.ENABLED) return
            nfcReader.start(activity = activity, onUri = ::onUriRead, onUnreadable = ::onUnreadable)
        }

        fun stopReaderMode(activity: Activity) {
            nfcReader.stop(activity)
        }

        /**
         * Point d'entrée d'un scan. `viewModelScope` est confiné au dispatcher principal, ce qui
         * ramène le callback du thread binder là où [processingScan] et [state] sont valides.
         */
        fun onUriRead(uri: String) {
            viewModelScope.launch {
                if (processingScan) return@launch
                processingScan = true
                val outcome =
                    try {
                        scanHandler.onUriRead(uri)
                    } finally {
                        processingScan = false
                    }
                record(outcome)
            }
        }

        fun onUnreadable() {
            viewModelScope.launch { record(ScanOutcome.Unreadable) }
        }

        /**
         * `Accepted` ne fait rien avancer ici : l'état final vient du snapshot publié, une fois la
         * libération réussie (§11.3). `Ignored` ne laisse aucune trace à l'écran — aucun scan n'a
         * été reconnu, il n'y a rien à dire à l'utilisateur.
         */
        private fun record(outcome: ScanOutcome) {
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

                ScanOutcome.Ignored -> {
                    // La session n'est pas dans un état qui accepte un scan, ou le moteur a refusé
                    // la transition : rien n'a bougé et il n'y a rien à annoncer — ce n'est pas un
                    // mauvais boîtier, donc surtout pas de vibration d'erreur.
                }
            }
            state = state.copy(lastOutcome = outcome.takeIf { it != ScanOutcome.Ignored })
        }
    }
