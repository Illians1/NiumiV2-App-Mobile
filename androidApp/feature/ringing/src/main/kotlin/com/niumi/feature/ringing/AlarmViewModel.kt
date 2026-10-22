package com.niumi.feature.ringing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.feature.ringing.ui.AlarmUiState
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanFailureReason
import com.niumi.system.nfc.ScanOutcome
import com.niumi.system.nfc.ScanSheetState
import com.niumi.system.nfc.scanFailureOf
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * État de l'écran de réveil (SPEC_ANDROID §10.4). Lit la persistance **puis** suit le publisher,
 * jamais l'inverse : `SessionSnapshotPublisher` vit en mémoire et vaut `null` dans tout processus
 * neuf — ce peut être le cas quand la notification de sonnerie ouvre l'activité après un réveil
 * (mort du processus, reconstruction du service). Collecter le
 * flux brut fermerait l'écran avant même que le coordinateur ait publié quoi que ce soit, privant
 * l'utilisateur du seul accès au scan.
 *
 * N'écrit jamais : ni `dispatch`, ni `commit`. L'écran de réveil observe, le scan décide
 * (SPEC_ANDROID §3, §11.3) — d'où l'absence de `SessionCoordinator` ici.
 *
 * Les entrées locales à l'appareil (disponibilité NFC, résultat d'un scan) sont poussées par
 * l'activité : elles n'ont pas de source observable côté domaine, et les garder ici évite à
 * l'activité de recomposer elle-même un état déjà décrit par [AlarmUiState].
 *
 * **Feuille de scan (étape 38).** « Scanner le Niumi Point » ouvre « Prêt à scanner », la seule
 * feuille qui demande la lecture NFC (Q4, [readerModeRequested]) ; un échec la remplace et coupe la
 * lecture, « Réessayer » la rouvre. Le NFC coupé ouvre ou remplace par l'échec `NFC_DISABLED`, et
 * son retour à un état stable « activé » rend « Prêt à scanner » seul (SPEC_ANDROID §11.2,
 * vigilance 13). Aucune de ces actions ne touche la session.
 */
@HiltViewModel
class AlarmViewModel
    @Inject
    constructor(
        private val gateway: SessionPersistenceGateway,
        private val snapshotPublisher: SessionSnapshotPublisher,
    ) : ViewModel() {
        private var sessionState: SessionStateDto? = null
        private var sessionConfirmedGone = false
        private var nfcAvailability = NfcAvailability.ENABLED
        private var sheet: ScanSheetState = ScanSheetState.Hidden

        var state: AlarmUiState by mutableStateOf(AlarmUiState.Loading)
            private set

        /** Vrai seulement feuille « Prêt à scanner » ouverte, NFC allumé, session en attente de scan. */
        val readerModeRequested: Boolean
            get() = (state as? AlarmUiState.Visible)?.screen?.readerModeRequested == true

        init {
            viewModelScope.launch {
                adoptLoaded(gateway.load())
                snapshotPublisher.snapshot.filterNotNull().collect { adoptSnapshot(it) }
            }
        }

        fun onNfcAvailabilityChanged(availability: NfcAvailability) {
            nfcAvailability = availability
            sheet =
                when {
                    sheet == ScanSheetState.Ready && availability == NfcAvailability.DISABLED -> NFC_DISABLED_SHEET
                    sheet == NFC_DISABLED_SHEET && availability == NfcAvailability.ENABLED -> ScanSheetState.Ready
                    else -> sheet
                }
            recompute()
        }

        /** « Scanner le Niumi Point » ; « Réessayer » fait de même. */
        fun openScanSheet() {
            sheet = if (nfcAvailability == NfcAvailability.DISABLED) NFC_DISABLED_SHEET else ScanSheetState.Ready
            recompute()
        }

        fun retryScan() = openScanSheet()

        /** « Annuler », le voile, le glissement ou le retour. */
        fun cancelScan() {
            sheet = ScanSheetState.Hidden
            recompute()
        }

        /**
         * Seul un scan reçu feuille « Prêt à scanner » ouverte compte : un résultat arrivé après
         * « Annuler » n'ouvre rien. Un scan accepté ou ignoré ne change pas la feuille ; c'est le
         * snapshot (`RELEASING`) qui la fermera.
         */
        fun onScanOutcome(outcome: ScanOutcome) {
            if (sheet != ScanSheetState.Ready) return
            scanFailureOf(outcome, nfcAvailability)?.let { sheet = ScanSheetState.Failed(it) }
            recompute()
        }

        private fun adoptLoaded(loaded: LoadResult) {
            when (loaded) {
                is LoadResult.Present -> adoptSnapshot(loaded.snapshot)

                // Seule une absence confirmée par la persistance ferme l'écran.
                LoadResult.Absent -> confirmSessionGone()

                // SPEC_CORE_KMP §13 : un snapshot illisible ne se lit jamais « pas de session ».
                // L'écran garde ce qu'il sait, et la réconciliation tranchera.
                is LoadResult.Unreadable -> Unit
            }
        }

        private fun confirmSessionGone() {
            sessionConfirmedGone = true
            recompute()
        }

        private fun adoptSnapshot(snapshot: SessionSnapshotDto) {
            sessionConfirmedGone = false
            sessionState = snapshot.state
            // Plus rien à scanner hors des trois états d'attente : la feuille se ferme pour de bon.
            if (snapshot.state !in SCAN_STATES) sheet = ScanSheetState.Hidden
            recompute()
        }

        private fun recompute() {
            val current = sessionState
            state =
                when {
                    sessionConfirmedGone -> {
                        AlarmUiState.Close
                    }

                    current == null -> {
                        AlarmUiState.Loading
                    }

                    else -> {
                        AlarmUiState.forSession(
                            sessionState = current,
                            nfcAvailability = nfcAvailability,
                            sheet = sheet,
                        )
                    }
                }
        }

        private companion object {
            val NFC_DISABLED_SHEET = ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED)

            val SCAN_STATES =
                setOf(SessionStateDto.RINGING, SessionStateDto.AWAITING_NFC, SessionStateDto.TRIGGERED_AWAITING_NFC)
        }
    }
