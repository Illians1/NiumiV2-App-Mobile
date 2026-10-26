package com.niumi.feature.session.home

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.audio.VibrationController
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader
import com.niumi.system.nfc.NfcScanHandler
import com.niumi.system.nfc.ScanFailureReason
import com.niumi.system.nfc.ScanOutcome
import com.niumi.system.nfc.ScanSheetState
import com.niumi.system.nfc.scanFailureOf
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionHistoryReader
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.TrackedSessionEnd
import com.niumi.system.session.isBlockingInForce
import com.niumi.system.session.needsTrackedSessionRead
import com.niumi.system.session.trackedSessionEndOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Ce qui met fin à la feuille d'annulation. */
sealed interface CancelSessionEnd {
    /** Réveil annulé : accueil clair et bulle « Réveil annulé… ». */
    data object Cancelled : CancelSessionEnd

    /** L'alarme a sonné entre-temps et le scan a terminé la session : écran 11. */
    data class Completed(
        val sessionId: String,
    ) : CancelSessionEnd

    /** Plus rien à annuler (alarme qui sonne, session disparue) : la feuille se ferme. */
    data object Closed : CancelSessionEnd
}

/**
 * [sheet] : `Hidden` tant que seule la question « Annuler le réveil ? » est montrée ; `Ready` ou
 * `Failed` quand la feuille de scan est ouverte par-dessus.
 */
data class CancelSessionUiState(
    val sheet: ScanSheetState = ScanSheetState.Hidden,
    val nfcAvailability: NfcAvailability = NfcAvailability.ENABLED,
    val end: CancelSessionEnd? = null,
    val releasing: Boolean = false,
) {
    /**
     * Vrai seulement feuille « Prêt à scanner » ouverte et NFC allumé (Q4, comme l'écran 10), jamais
     * pendant la libération ([releasing] : la feuille montre alors le cercle de chargement).
     */
    val readerModeRequested: Boolean
        get() = end == null && !releasing && sheet == ScanSheetState.Ready && nfcAvailability == NfcAvailability.ENABLED
}

/** Lecteur NFC, traitement du scan, vibration d'erreur et journal. */
class CancelSessionScanSources
    @Inject
    constructor(
        val nfcReader: NfcReader,
        val scanHandler: NfcScanHandler,
        val vibrationController: VibrationController,
        val technicalEventLog: TechnicalEventLog,
    )

/** Dépendances de la feuille, regroupées pour rester sous `LongParameterList` de detekt. */
class CancelSessionSources
    @Inject
    constructor(
        val scan: CancelSessionScanSources,
        val snapshotPublisher: SessionSnapshotPublisher,
        val historyReader: SessionHistoryReader,
        val gateway: SessionPersistenceGateway,
    )

/**
 * Feuille « Annuler le réveil ? » (`design-plan/SPECIFICATION.md` §11, étape 46 ; remplace l'écran
 * de scan requis). Elle suit la session ouverte par son identifiant et ne conclut que sur cette
 * session relue finie ([SessionHistoryReader]) : `Accepted` ne dit pas que la libération a réussi
 * (SPEC_ANDROID §11.3), et après un scan du matin le snapshot publié passe aussitôt à la session
 * suivante. Elle n'écrit rien : le scan passe par [NfcScanHandler] (`HandleValidNfcUseCase`).
 *
 * Mêmes règles de lecture que l'écran 10 (SPEC_ANDROID §11.2) : lecture feuille « Prêt à scanner »
 * ouverte seulement, mêmes échecs et même vibration, NFC relu au premier plan (journalisé) et à
 * chaque état stable de l'adaptateur (sans journal) — vigilance 13. Les rappels du lecteur, reçus
 * sur un fil binder, repassent par le fil principal : [processingScan] n'a pas d'autre garde.
 */
@Suppress("TooManyFunctions") // Une fonction par geste et par rappel du lecteur.
@HiltViewModel
class CancelSessionViewModel
    @Inject
    constructor(
        private val sources: CancelSessionSources,
    ) : ViewModel() {
        var state by mutableStateOf(CancelSessionUiState(nfcAvailability = sources.scan.nfcReader.availability))
            private set

        private var trackedSessionId: String? = null
        private var processingScan = false

        init {
            viewModelScope.launch { sources.snapshotPublisher.snapshot.collect(::adoptSnapshot) }
        }

        /**
         * Le lien « Annuler le réveil » : repart de zéro et suit la session alors publiée. Le
         * ViewModel vit autant que l'accueil ; sans cela, une deuxième ouverture rejouerait la fin
         * de la première.
         */
        fun open() {
            trackedSessionId = null
            state = CancelSessionUiState(nfcAvailability = sources.scan.nfcReader.availability)
            viewModelScope.launch { adoptSnapshot(sources.snapshotPublisher.snapshot.value) }
        }

        /** « Scanner le Niumi Spot » ; « Réessayer » fait de même. */
        fun openScanSheet() {
            updateSheet(
                if (state.nfcAvailability ==
                    NfcAvailability.DISABLED
                ) {
                    NFC_DISABLED_SHEET
                } else {
                    ScanSheetState.Ready
                },
            )
        }

        fun retryScan() = openScanSheet()

        /** « Annuler », le voile, le glissement ou le retour de la feuille de scan. */
        fun cancelScan() = updateSheet(ScanSheetState.Hidden)

        /** Retour au premier plan : relit le NFC et journalise un NFC coupé (SPEC_ANDROID §11.2). */
        fun refreshAvailability() {
            adoptAvailability(sources.scan.nfcReader.availability)
            if (state.nfcAvailability == NfcAvailability.DISABLED) {
                sources.scan.technicalEventLog.log(TechnicalEventType.NFC_DISABLED)
            }
        }

        /** État stable de l'adaptateur (étape 31) : relit le NFC, sans journal. */
        fun onAdapterSettled() = adoptAvailability(sources.scan.nfcReader.availability)

        fun startReaderMode(activity: Activity) {
            if (!state.readerModeRequested) return
            sources.scan.nfcReader.start(
                activity = activity,
                onUri = ::onUriRead,
                onUnreadable = ::onUnreadable,
                onForeignTag = ::onForeignTag,
            )
        }

        fun stopReaderMode(activity: Activity) = sources.scan.nfcReader.stop(activity)

        fun onUriRead(uri: String) {
            viewModelScope.launch {
                if (processingScan) return@launch
                processingScan = true
                val outcome =
                    try {
                        sources.scan.scanHandler.onUriRead(uri)
                    } finally {
                        processingScan = false
                    }
                record(outcome)
                if (outcome == ScanOutcome.Accepted) resolveEnd(sources.snapshotPublisher.snapshot.value)
            }
        }

        // Les rappels du lecteur arrivent sur un fil binder (`ReaderModeNfcReader`) : l'état ne se
        // touche que depuis le fil principal.
        fun onUnreadable() {
            viewModelScope.launch { record(ScanOutcome.Unreadable) }
        }

        /** Tag lu sans NDEF ni URI : « Ce n'est pas un Niumi Spot. », comme à l'écran 10. */
        fun onForeignTag() {
            viewModelScope.launch { record(ScanOutcome.NotANiumiSpot) }
        }

        private fun record(outcome: ScanOutcome) {
            when (outcome) {
                ScanOutcome.Accepted -> {
                    sources.scan.technicalEventLog.log(TechnicalEventType.NFC_SCAN_VALID)
                }

                // Un tag lu puis refusé : la vibration d'erreur accompagne la feuille d'échec.
                ScanOutcome.UnknownBox, ScanOutcome.NotANiumiSpot -> {
                    sources.scan.technicalEventLog.log(TechnicalEventType.NFC_SCAN_INVALID)
                    sources.scan.vibrationController.vibrateError()
                }

                ScanOutcome.Unreadable -> {
                    sources.scan.technicalEventLog.log(TechnicalEventType.NFC_SCAN_INVALID)
                }

                ScanOutcome.Ignored -> {}
            }
            // Seul un scan reçu feuille « Prêt à scanner » ouverte ouvre un échec.
            if (state.sheet == ScanSheetState.Ready) {
                scanFailureOf(outcome, state.nfcAvailability)?.let { updateSheet(ScanSheetState.Failed(it)) }
            }
        }

        private fun adoptAvailability(availability: NfcAvailability) {
            val current = state.sheet
            val sheet =
                when {
                    current == ScanSheetState.Ready && availability == NfcAvailability.DISABLED -> NFC_DISABLED_SHEET
                    current == NFC_DISABLED_SHEET && availability == NfcAvailability.ENABLED -> ScanSheetState.Ready
                    else -> current
                }
            state = state.copy(nfcAvailability = availability, sheet = sheet)
        }

        private fun updateSheet(sheet: ScanSheetState) {
            if (state.end == null) state = state.copy(sheet = sheet)
        }

        private suspend fun adoptSnapshot(published: SessionSnapshotDto?) {
            val tracked = trackedSessionId
            when {
                state.end != null -> Unit

                // Processus neuf (feuille restaurée) : rien de publié, la persistance tranche.
                tracked == null && published == null -> adoptLoaded(sources.gateway.load())

                // Seul un blocage en vigueur, ou sa libération, s'annule ici : jamais la session du
                // lendemain armée mais pas encore en blocage (feuille restaurée après une mort du
                // processus), qu'un scan annulerait en éteignant l'interrupteur.
                tracked == null && published.isCancellable() -> trackedSessionId = published?.sessionId

                tracked == null -> end(CancelSessionEnd.Closed)

                needsTrackedSessionRead(tracked, published) -> resolveEnd(published)

                // Même session, en cours : plus rien à annuler hors du blocage (l'alarme sonne).
                !published.isCancellable() -> end(CancelSessionEnd.Closed)

                // Choix de Mehdi du 2026-10-26 : cercle de chargement pendant la libération.
                else -> state = state.copy(releasing = published?.state == SessionStateDto.RELEASING)
            }
        }

        private suspend fun adoptLoaded(loaded: LoadResult) {
            when (loaded) {
                is LoadResult.Present -> adoptSnapshot(loaded.snapshot)

                LoadResult.Absent -> end(CancelSessionEnd.Closed)

                // Persistance illisible : ne jamais conclure « pas de session » (SPEC_CORE_KMP §13).
                is LoadResult.Unreadable -> Unit
            }
        }

        private suspend fun resolveEnd(published: SessionSnapshotDto?) {
            val tracked = trackedSessionId ?: return
            val session = sources.historyReader.find(tracked)
            // La feuille a pu être rouverte pendant la relecture : la réponse ne la concerne plus.
            if (trackedSessionId != tracked) return
            when (trackedSessionEndOf(session)) {
                is TrackedSessionEnd.Completed -> end(CancelSessionEnd.Completed(tracked))

                TrackedSessionEnd.Cancelled -> end(CancelSessionEnd.Cancelled)

                TrackedSessionEnd.Other -> end(CancelSessionEnd.Closed)

                // Pas encore finie : on attend, sauf si elle n'est plus publiée et introuvable.
                null -> if (needsTrackedSessionRead(tracked, published)) end(CancelSessionEnd.Closed)
            }
        }

        private fun end(end: CancelSessionEnd) {
            if (state.end == null) state = state.copy(end = end, sheet = ScanSheetState.Hidden)
        }

        private companion object {
            val NFC_DISABLED_SHEET = ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED)

            /** Blocage en vigueur, ou libération en cours après un scan accepté. */
            fun SessionSnapshotDto?.isCancellable(): Boolean =
                when (this?.state) {
                    SessionStateDto.ARMED -> isBlockingInForce()
                    SessionStateDto.RELEASING -> true
                    else -> false
                }
        }
    }
