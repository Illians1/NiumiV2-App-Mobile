package com.niumi.feature.session.home

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.logging.TechnicalEventType
import com.niumi.feature.session.active.fakes.FakeNfcReader
import com.niumi.feature.session.active.fakes.FakeSessionPersistenceGateway
import com.niumi.feature.session.active.fakes.RecordingTechnicalEventLog
import com.niumi.feature.session.active.fakes.RecordingVibrationController
import com.niumi.feature.session.active.fakes.ScriptedNfcScanHandler
import com.niumi.feature.session.active.fakes.presentSession
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanFailureReason
import com.niumi.system.nfc.ScanOutcome
import com.niumi.system.nfc.ScanSheetState
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionHistoryReader
import com.niumi.system.session.SessionSnapshotPublisher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Feuille « Annuler le réveil ? » (étape 46, `SPECIFICATION.md` §11 ; SPEC_ANDROID §11.2, §11.3).
 *
 * - La lecture NFC n'a lieu que feuille « Prêt à scanner » ouverte (Q4, comme l'écran 10), avec
 *   les mêmes feuilles d'échec et la même relecture du NFC sur état stable (vigilance 13).
 * - **`Accepted` ne suffit pas** : seule la session suivie, relue finie par son identifiant,
 *   conclut. Après un scan du matin, le snapshot publié passe aussitôt à la session suivante
 *   (`ARMED`, autre identifiant) : la relecture par identifiant est la seule à voir `COMPLETED`.
 * - Le ViewModel n'écrit rien : la passerelle et le contrôleur de vibration d'alarme lèvent.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CancelSessionViewModelTest {
    private val nfcReader = FakeNfcReader()
    private val scanHandler = ScriptedNfcScanHandler()
    private val vibrationController = RecordingVibrationController()
    private val technicalEventLog = RecordingTechnicalEventLog()
    private val snapshotPublisher = SessionSnapshotPublisher()
    private val history = mutableMapOf<String, SessionSnapshotDto>()
    private val historyReader = SessionHistoryReader { history[it] }
    private val gateway = FakeSessionPersistenceGateway()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        snapshotPublisher.publish(snapshot(SessionStateDto.ARMED))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        CancelSessionViewModel(
            CancelSessionSources(
                scan = CancelSessionScanSources(nfcReader, scanHandler, vibrationController, technicalEventLog),
                snapshotPublisher = snapshotPublisher,
                historyReader = historyReader,
                gateway = gateway,
            ),
        )

    private fun snapshot(
        state: SessionStateDto,
        sessionId: String = TRACKED_ID,
        blockingApplied: Boolean = true,
    ) = SessionSnapshotDto(
        schemaVersion = 3,
        revision = 2,
        sessionId = sessionId,
        wakeSchedule = WakeScheduleDto("2026-10-26", "07:00", "Europe/Paris", 1_800_000_000_000L),
        state = state,
        releaseTarget = null,
        health = SessionHealthDto.HEALTHY,
        createdAtEpochMillis = 1_000L,
        armedAtEpochMillis = 1_100L,
        blockingAppliedAtEpochMillis = 1_200L.takeIf { blockingApplied },
        ringingAtEpochMillis = null,
        alarmSoundStoppedAtEpochMillis = null,
        triggerElapsedAtEpochMillis = null,
        nfcVerifiedAtEpochMillis = null,
        releasingAtEpochMillis = null,
        completedAtEpochMillis = null,
        cancelledAtEpochMillis = null,
        failureCode = null,
    )

    @Test
    fun theSheetOpensOnTheQuestionWithoutReading() {
        val viewModel = viewModel()

        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Hidden)
        assertThat(viewModel.state.readerModeRequested).isFalse()
        assertThat(viewModel.state.end).isNull()
    }

    @Test
    fun theScanButtonOpensTheReadySheetAndOnlyThenReads() {
        val viewModel = viewModel()

        viewModel.openScanSheet()

        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Ready)
        assertThat(viewModel.state.readerModeRequested).isTrue()
    }

    @Test
    fun theScanButtonWithTheNfcOffOpensTheNfcFailure() {
        nfcReader.availabilityValue = NfcAvailability.DISABLED
        val viewModel = viewModel()

        viewModel.openScanSheet()

        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED))
        assertThat(viewModel.state.readerModeRequested).isFalse()
    }

    @Test
    fun cancellingTheScanSheetGoesBackToTheQuestion() {
        val viewModel = viewModel()
        viewModel.openScanSheet()

        viewModel.cancelScan()

        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Hidden)
        assertThat(viewModel.state.readerModeRequested).isFalse()
        assertThat(viewModel.state.end).isNull()
    }

    /**
     * Vigilance 13 (SPEC_ANDROID §11.2, étape 31) : un NFC lu coupé juste après un démarrage
     * revient seul dès l'état stable de l'adaptateur, et la feuille repasse à « Prêt à scanner ».
     */
    @Test
    fun theNfcFailureClearsItselfWhenTheAdapterSettlesOn() {
        val viewModel = viewModel()
        viewModel.openScanSheet()
        nfcReader.availabilityValue = NfcAvailability.DISABLED
        viewModel.refreshAvailability()
        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED))

        nfcReader.availabilityValue = NfcAvailability.ENABLED
        viewModel.onAdapterSettled()

        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Ready)
        assertThat(viewModel.state.readerModeRequested).isTrue()
    }

    /** `NFC_DISABLED` est consigné une fois par passage au premier plan, pas par transition système. */
    @Test
    fun aSettledAdapterNeverLogsNfcDisabledAgain() {
        nfcReader.availabilityValue = NfcAvailability.DISABLED
        val viewModel = viewModel()
        viewModel.refreshAvailability()

        viewModel.onAdapterSettled()
        nfcReader.availabilityValue = NfcAvailability.ENABLED
        viewModel.onAdapterSettled()
        nfcReader.availabilityValue = NfcAvailability.DISABLED
        viewModel.onAdapterSettled()

        assertThat(technicalEventLog.logged.count { it == TechnicalEventType.NFC_DISABLED }).isEqualTo(1)
        assertThat(viewModel.state.nfcAvailability).isEqualTo(NfcAvailability.DISABLED)
    }

    @Test
    fun anUnknownNiumiSpotShowsItsFailureWithAnErrorVibration() {
        scanHandler.outcome = ScanOutcome.UnknownBox
        val viewModel = viewModel()
        viewModel.openScanSheet()

        viewModel.onUriRead(URI)

        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Failed(ScanFailureReason.NOT_REGISTERED))
        assertThat(vibrationController.errorVibrations).isEqualTo(1)
        assertThat(technicalEventLog.logged).contains(TechnicalEventType.NFC_SCAN_INVALID)
        assertThat(viewModel.state.end).isNull()
    }

    @Test
    fun aForeignTagIsNotANiumiSpotWithAnErrorVibration() {
        val viewModel = viewModel()
        viewModel.openScanSheet()

        viewModel.onForeignTag()

        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Failed(ScanFailureReason.NOT_A_NIUMI_SPOT))
        assertThat(vibrationController.errorVibrations).isEqualTo(1)
    }

    @Test
    fun anUnreadableTagShowsItsFailureWithoutVibration() {
        val viewModel = viewModel()
        viewModel.openScanSheet()

        viewModel.onUnreadable()

        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Failed(ScanFailureReason.UNREADABLE))
        assertThat(vibrationController.errorVibrations).isEqualTo(0)
        assertThat(technicalEventLog.logged).contains(TechnicalEventType.NFC_SCAN_INVALID)
    }

    @Test
    fun retryReopensTheReadySheet() {
        val viewModel = viewModel()
        viewModel.openScanSheet()
        viewModel.onUnreadable()

        viewModel.retryScan()

        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Ready)
    }

    @Test
    fun aResultArrivingAfterCancelOpensNothing() {
        val viewModel = viewModel()
        viewModel.openScanSheet()
        viewModel.cancelScan()

        viewModel.onUnreadable()

        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Hidden)
    }

    /** SPEC_ANDROID §11.3 : `RELEASING` n'est pas une fin, la feuille reste. */
    @Test
    fun anAcceptedScanAloneDoesNotConclude() {
        scanHandler.outcome = ScanOutcome.Accepted
        history[TRACKED_ID] = snapshot(SessionStateDto.RELEASING)
        val viewModel = viewModel()
        viewModel.openScanSheet()

        viewModel.onUriRead(URI)

        assertThat(viewModel.state.end).isNull()
        assertThat(technicalEventLog.logged).contains(TechnicalEventType.NFC_SCAN_VALID)
    }

    @Test
    fun anAcceptedScanWhoseSessionIsReadCancelledEndsCancelled() {
        scanHandler.outcome = ScanOutcome.Accepted
        history[TRACKED_ID] = snapshot(SessionStateDto.CANCELLED)
        val viewModel = viewModel()
        viewModel.openScanSheet()

        viewModel.onUriRead(URI)

        assertThat(viewModel.state.end).isEqualTo(CancelSessionEnd.Cancelled)
        assertThat(viewModel.state.readerModeRequested).isFalse()
    }

    @Test
    fun theTrackedSessionPublishedCancelledEndsCancelled() {
        history[TRACKED_ID] = snapshot(SessionStateDto.CANCELLED)
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot(SessionStateDto.CANCELLED))

        assertThat(viewModel.state.end).isEqualTo(CancelSessionEnd.Cancelled)
    }

    /**
     * L'alarme a sonné entre l'ouverture et le scan : le scan termine la session (`COMPLETED`), le
     * crochet arme aussitôt la suivante. La relecture par identifiant mène à l'écran 11.
     */
    @Test
    fun aCompletedTrackedSessionReplacedByTheNextOneLeadsToTheSuccessScreen() {
        history[TRACKED_ID] = snapshot(SessionStateDto.COMPLETED)
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot(SessionStateDto.ARMED, sessionId = NEXT_ID))

        assertThat(viewModel.state.end).isEqualTo(CancelSessionEnd.Completed(TRACKED_ID))
    }

    /** L'alarme sonne pendant que la feuille est ouverte : l'écran 10 prend la main. */
    @Test
    fun theTrackedSessionStartingToRingClosesTheSheet() {
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot(SessionStateDto.RINGING))

        assertThat(viewModel.state.end).isEqualTo(CancelSessionEnd.Closed)
    }

    /** Ouverte au moment où l'alarme part : rien à suivre, la feuille se ferme aussitôt. */
    @Test
    fun aSessionAlreadyRingingAtOpeningClosesTheSheet() {
        snapshotPublisher.publish(snapshot(SessionStateDto.RINGING))

        val viewModel = viewModel()

        assertThat(viewModel.state.end).isEqualTo(CancelSessionEnd.Closed)
    }

    @Test
    fun aReleasingTrackedSessionKeepsTheSheet() {
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot(SessionStateDto.RELEASING))

        assertThat(viewModel.state.end).isNull()
    }

    /**
     * Le ViewModel vit autant que l'accueil : une nouvelle ouverture repart de zéro et suit la
     * session alors publiée, sans rejouer la fin de la précédente.
     */
    @Test
    fun reopeningStartsAfreshOnTheSessionThenPublished() {
        history[TRACKED_ID] = snapshot(SessionStateDto.CANCELLED)
        val viewModel = viewModel()
        viewModel.openScanSheet()
        snapshotPublisher.publish(snapshot(SessionStateDto.CANCELLED))
        snapshotPublisher.publish(snapshot(SessionStateDto.ARMED, sessionId = NEXT_ID))

        viewModel.open()

        assertThat(viewModel.state.end).isNull()
        assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Hidden)
        history[NEXT_ID] = snapshot(SessionStateDto.CANCELLED, sessionId = NEXT_ID)
        snapshotPublisher.publish(snapshot(SessionStateDto.CANCELLED, sessionId = NEXT_ID))
        assertThat(viewModel.state.end).isEqualTo(CancelSessionEnd.Cancelled)
    }

    /**
     * Relecture : `ReaderModeNfcReader` appelle ses rappels sur un fil binder. Une lecture ratée
     * ou un tag étranger repassent par le fil principal avant de toucher l'état, comme le scan.
     */
    @Test
    fun readerCallbacksTouchTheStateOnlyFromTheMainDispatcher() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = viewModel()
            runCurrent()
            viewModel.openScanSheet()

            viewModel.onUnreadable()
            viewModel.onForeignTag()
            assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Ready)

            runCurrent()
            assertThat(viewModel.state.sheet).isEqualTo(ScanSheetState.Failed(ScanFailureReason.UNREADABLE))
        }

    /**
     * Défaut différé 2 : la feuille, restaurée après une mort du processus, ne doit pas suivre la
     * session du lendemain armée mais pas encore en blocage — un scan l'annulerait et éteindrait
     * l'interrupteur. Seul un blocage en vigueur (ou sa libération) s'annule ici.
     */
    @Test
    fun anArmedSessionWhoseBlockingHasNotStartedIsNotCancellableHere() {
        snapshotPublisher.publish(snapshot(SessionStateDto.ARMED, sessionId = NEXT_ID, blockingApplied = false))

        val viewModel = viewModel()

        assertThat(viewModel.state.end).isEqualTo(CancelSessionEnd.Closed)
    }

    /**
     * Défaut différé 2, suite : dans un processus neuf, rien n'est encore publié. La persistance
     * tranche, comme à l'écran de réveil : aucune session (réveil ponctuel terminé), la feuille se
     * ferme ; un blocage en vigueur, elle le suit.
     */
    @Test
    fun inAFreshProcessWithNoSessionTheRestoredSheetCloses() {
        val viewModel = viewModelWithEmptyPublisher(LoadResult.Absent)

        assertThat(viewModel.state.end).isEqualTo(CancelSessionEnd.Closed)
    }

    @Test
    fun inAFreshProcessABlockingInForceIsFollowed() {
        val viewModel = viewModelWithEmptyPublisher(presentSession(snapshot(SessionStateDto.ARMED), emptyList()))

        assertThat(viewModel.state.end).isNull()
    }

    private fun viewModelWithEmptyPublisher(loaded: LoadResult): CancelSessionViewModel {
        gateway.result = loaded
        return CancelSessionViewModel(
            CancelSessionSources(
                CancelSessionScanSources(nfcReader, scanHandler, vibrationController, technicalEventLog),
                SessionSnapshotPublisher(),
                historyReader,
                gateway,
            ),
        )
    }

    /**
     * Défaut différé 1 : une relecture de l'ancienne session, encore en cours quand la feuille est
     * rouverte, ne conclut pas la nouvelle ouverture.
     */
    @Test
    fun aReadOfThePreviousSessionFinishingAfterReopeningIsIgnored() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val gate = CompletableDeferred<Unit>()
            val slowHistory =
                SessionHistoryReader { id ->
                    gate.await()
                    history[id]
                }
            history[TRACKED_ID] = snapshot(SessionStateDto.COMPLETED)
            val viewModel =
                CancelSessionViewModel(
                    CancelSessionSources(
                        CancelSessionScanSources(nfcReader, scanHandler, vibrationController, technicalEventLog),
                        snapshotPublisher,
                        slowHistory,
                        gateway,
                    ),
                )
            runCurrent()
            snapshotPublisher.publish(snapshot(SessionStateDto.ARMED, sessionId = NEXT_ID))
            runCurrent()

            viewModel.open()
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            assertThat(viewModel.state.end).isNull()
        }

    /**
     * Défaut différé 3 (choix de Mehdi du 2026-10-26) : pendant la libération, la feuille de scan
     * montre le cercle de chargement et la lecture s'arrête.
     */
    @Test
    fun whileTheTrackedSessionIsReleasingTheSheetShowsTheLoaderAndStopsReading() {
        val viewModel = viewModel()
        viewModel.openScanSheet()

        snapshotPublisher.publish(snapshot(SessionStateDto.RELEASING))

        assertThat(viewModel.state.releasing).isTrue()
        assertThat(viewModel.state.readerModeRequested).isFalse()
        assertThat(viewModel.state.end).isNull()
    }

    /** Un tag posé émet plusieurs lectures : un vrai chevauchement, une seule lecture traitée. */
    @Test
    fun aSecondScanArrivingWhileTheFirstIsStillRunningIsIgnored() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val gate = CompletableDeferred<Unit>()
            scanHandler.gate = gate
            val viewModel = viewModel()
            runCurrent()
            viewModel.openScanSheet()

            viewModel.onUriRead(URI)
            runCurrent()
            viewModel.onUriRead(URI)
            runCurrent()

            assertThat(scanHandler.calls).isEqualTo(1)
            gate.complete(Unit)
            runCurrent()
        }

    private companion object {
        const val TRACKED_ID = "11111111-1111-1111-1111-111111111111"
        const val NEXT_ID = "22222222-2222-2222-2222-222222222222"
        const val URI = "https://niumi.app/b/box-1"
    }
}
