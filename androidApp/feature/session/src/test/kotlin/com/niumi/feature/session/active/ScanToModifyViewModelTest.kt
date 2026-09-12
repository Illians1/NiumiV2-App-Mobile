package com.niumi.feature.session.active

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.logging.TechnicalEventType
import com.niumi.feature.session.active.fakes.FakeNfcReader
import com.niumi.feature.session.active.fakes.RecordingTechnicalEventLog
import com.niumi.feature.session.active.fakes.RecordingVibrationController
import com.niumi.feature.session.active.fakes.ScriptedNfcScanHandler
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanOutcome
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
 * Écran 9 : scan requis pour modifier ou annuler (SPEC_ANDROID §15, §11.2, §11.3).
 *
 * Deux invariants portent cet écran :
 * - **aucune donnée n'est modifiée avant l'état final** : le ViewModel n'écrit nulle part, et ses
 *   fakes de passerelle et de contrôleur lèvent si on tente de le faire (SPEC_CORE_KMP §2 point 11) ;
 * - **`Accepted` ne suffit pas** à présenter la session comme annulée : §11.3 interdit de le faire
 *   avant `RELEASE_SUCCEEDED`, seul l'état final publié y autorise.
 *
 * À cette étape, la liaison de production est `PendingNfcScanHandler` et rend `Ignored` :
 * `HandleValidNfcUseCase` arrive à l'étape 18, sans toucher à cette classe.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanToModifyViewModelTest {
    private val nfcReader = FakeNfcReader()
    private val scanHandler = ScriptedNfcScanHandler()
    private val vibrationController = RecordingVibrationController()
    private val technicalEventLog = RecordingTechnicalEventLog()
    private val snapshotPublisher = SessionSnapshotPublisher()

    // `startReaderMode`/`stopReaderMode` ne sont pas couverts en JVM : ils exigent une `Activity`,
    // qui n'est pas instanciable hors instrumentation. Les tests pilotent les deux callbacks que
    // ces méthodes câblent — même convention que `PairingViewModelTest`.

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        ScanToModifyViewModel(
            nfcReader = nfcReader,
            scanHandler = scanHandler,
            vibrationController = vibrationController,
            technicalEventLog = technicalEventLog,
            snapshotPublisher = snapshotPublisher,
        )

    private fun snapshot(state: SessionStateDto) =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = 2,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule = WakeScheduleDto("2026-09-04", "07:00", "Europe/Paris", 1_800_000_000_000L),
            state = state,
            releaseTarget = null,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = 1_000L,
            armedAtEpochMillis = 1_100L,
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
    fun aDisabledNfcIsReportedToTheUserAndLogged() {
        nfcReader.availabilityValue = NfcAvailability.DISABLED
        val viewModel = viewModel()

        viewModel.refreshAvailability()

        assertThat(viewModel.state.availability).isEqualTo(NfcAvailability.DISABLED)
        assertThat(ScanToModifyTexts.availabilityMessage(NfcAvailability.DISABLED)).isNotNull()
        assertThat(technicalEventLog.logged).contains(TechnicalEventType.NFC_DISABLED)
    }

    /** Le cas de production de cette étape : rien ne se passe, et l'écran n'annonce rien. */
    @Test
    fun anIgnoredScanChangesNothingAndSaysNothing() {
        scanHandler.outcome = ScanOutcome.Ignored
        val viewModel = viewModel()
        viewModel.onUriRead("https://niumi.app/b/box-1")

        assertThat(scanHandler.calls).isEqualTo(1)
        assertThat(viewModel.state.lastOutcome).isNull()
        assertThat(viewModel.state.isCancelled).isFalse()
        assertThat(technicalEventLog.logged).doesNotContain(TechnicalEventType.NFC_SCAN_VALID)
    }

    @Test
    fun anUnknownBoxIsRejectedWithAnErrorVibrationAndChangesNoState() {
        scanHandler.outcome = ScanOutcome.UnknownBox
        val viewModel = viewModel()
        viewModel.onUriRead("https://niumi.app/b/autre")

        assertThat(viewModel.state.lastOutcome).isEqualTo(ScanOutcome.UnknownBox)
        assertThat(viewModel.state.isCancelled).isFalse()
        assertThat(vibrationController.errorVibrations).isEqualTo(1)
        assertThat(technicalEventLog.logged).contains(TechnicalEventType.NFC_SCAN_INVALID)
    }

    /** §11.2 : un tag illisible a son propre texte, et ne vibre pas (ce n'est pas un refus). */
    @Test
    fun anUnreadableTagHasItsOwnMessageAndNoErrorVibration() {
        val viewModel = viewModel()
        viewModel.onUnreadable()

        assertThat(viewModel.state.lastOutcome).isEqualTo(ScanOutcome.Unreadable)
        assertThat(vibrationController.errorVibrations).isEqualTo(0)
        assertThat(technicalEventLog.logged).contains(TechnicalEventType.NFC_SCAN_INVALID)
    }

    /**
     * Cœur de §11.3 : « l'application ne présente pas la session comme terminée avant
     * `RELEASE_SUCCEEDED` ». Un `Accepted` place la session dans `RELEASING`, jamais dans un état
     * final — l'écran 11 ne doit donc pas être atteint sur ce seul signal.
     */
    @Test
    fun anAcceptedScanAloneDoesNotDeclareTheSessionCancelled() {
        scanHandler.outcome = ScanOutcome.Accepted
        val viewModel = viewModel()
        viewModel.onUriRead("https://niumi.app/b/box-1")

        assertThat(viewModel.state.isCancelled).isFalse()
        assertThat(technicalEventLog.logged).contains(TechnicalEventType.NFC_SCAN_VALID)
    }

    @Test
    fun aReleasingSessionIsShownAsSuchWithoutBeingCancelled() {
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot(SessionStateDto.RELEASING))

        assertThat(viewModel.state.isReleasing).isTrue()
        assertThat(viewModel.state.isCancelled).isFalse()
    }

    @Test
    fun onlyAFinalCancelledStateDeclaresTheSessionCancelled() {
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot(SessionStateDto.CANCELLED))

        assertThat(viewModel.state.isCancelled).isTrue()
        assertThat(viewModel.state.isReleasing).isFalse()
    }

    /** Une session terminée au réveil relève de l'écran 10 (étape 17), pas de l'écran 11. */
    @Test
    fun aCompletedSessionDoesNotLeadToTheCancelledScreen() {
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot(SessionStateDto.COMPLETED))

        assertThat(viewModel.state.isCancelled).isFalse()
    }

    /**
     * Un tag posé émet plusieurs lectures : sans garde, le même scan serait traité deux fois et, à
     * l'étape 18, deux libérations concurrentes seraient dispatchées. Le test exige un vrai
     * chevauchement — un dispatcher ordonnancé et une lecture bloquée — et non deux appels
     * successifs, que la garde laisse passer à raison.
     */
    @Test
    fun aSecondScanArrivingWhileTheFirstIsStillRunningIsIgnored() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val gate = CompletableDeferred<Unit>()
            scanHandler.gate = gate
            val viewModel = viewModel()

            viewModel.onUriRead("https://niumi.app/b/box-1")
            runCurrent()
            assertThat(scanHandler.calls).isEqualTo(1)

            viewModel.onUriRead("https://niumi.app/b/box-1")
            runCurrent()

            assertThat(scanHandler.calls).isEqualTo(1)
            gate.complete(Unit)
            runCurrent()
        }

    @Test
    fun everyNfcAvailabilityIsAccountedFor() {
        assertThat(ScanToModifyTexts.availabilityMessage(NfcAvailability.ENABLED)).isNull()
        assertThat(ScanToModifyTexts.availabilityMessage(NfcAvailability.DISABLED)).isNotEmpty()
        assertThat(ScanToModifyTexts.availabilityMessage(NfcAvailability.ABSENT)).isNotEmpty()
    }
}
