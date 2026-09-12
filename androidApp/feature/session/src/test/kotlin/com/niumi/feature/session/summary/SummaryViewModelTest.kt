package com.niumi.feature.session.summary

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.DomainViolationDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.BlockedPackage
import com.niumi.feature.session.activation.ActivationSources
import com.niumi.feature.session.activation.ArmSessionUseCase
import com.niumi.feature.session.activation.fakes.CallJournal
import com.niumi.feature.session.activation.fakes.RecordingAppSelectionStore
import com.niumi.feature.session.activation.fakes.RecordingPairedBoxStore
import com.niumi.feature.session.activation.fakes.RecordingReadinessChecker
import com.niumi.feature.session.activation.fakes.RecordingSessionCoordinator
import com.niumi.feature.session.activation.fakes.RecordingTimeZoneProvider
import com.niumi.feature.session.wake.fakes.FakeClock
import com.niumi.system.common.UuidIdGenerator
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheck
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome
import com.niumi.system.readiness.ReadinessReport
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.SessionEventFactory
import com.niumi.system.session.SessionSnapshotPublisher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Écran 6 (SPEC_ANDROID §15, §19.1 « impossibilité de confirmer si un contrôle bloquant échoue »).
 * `ArmSessionUseCase` et la façade commune sont réels : seuls les dépôts et le coordinateur sont
 * simulés, pour qu'aucune règle d'activation ne soit réécrite dans le test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {
    private fun paris(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long =
        ZonedDateTime
            .of(year, month, day, hour, minute, 0, 0, ZoneId.of("Europe/Paris"))
            .toInstant()
            .toEpochMilli()

    private val now = paris(2026, 9, 3, 20, 0)
    private val journal = CallJournal()
    private val credential =
        PairedBoxCredentialDto(
            protocolVersion = 1,
            boxId = "b0c1d2e3-4455-6677-8899-aabbccddeeff",
            tokenSha256Hex = "a".repeat(64),
        )
    private val selection = listOf(BlockedPackage("com.example.chat", "Chat"))

    private var readinessCheckCalls = 0

    private val readinessChecker =
        RecordingReadinessChecker(
            journal,
            reportForNull = report(candidateTriggerAtEpochMillis = null),
            reportForCandidate = { candidate ->
                readinessCheckCalls++
                report(candidateTriggerAtEpochMillis = candidate)
            },
        )
    private val timeZoneProvider = RecordingTimeZoneProvider(journal, zoneId = "Europe/Paris")
    private val pairedBoxStore = RecordingPairedBoxStore(journal, credential)
    private val appSelectionStore = RecordingAppSelectionStore(journal, selection)
    private val coordinator = RecordingSessionCoordinator(journal)
    private val snapshotPublisher = SessionSnapshotPublisher()

    private fun report(
        candidateTriggerAtEpochMillis: Long?,
        checks: List<ReadinessCheck> = listOf(passedCheck()),
        appSelectionCount: Int = selection.size,
        hasPairedBox: Boolean = true,
    ) = ReadinessReport(
        checks = checks,
        appSelectionCount = appSelectionCount,
        hasPairedBox = hasPairedBox,
        candidateTriggerAtEpochMillis = candidateTriggerAtEpochMillis,
        nowEpochMillis = now,
    )

    private fun passedCheck() =
        ReadinessCheck(
            id = ReadinessCheckId.NFC_ENABLED,
            severity = ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
            outcome = ReadinessOutcome.PASSED,
            action = ReadinessAction.OpenNfcSettings,
        )

    private fun failedCheck() =
        ReadinessCheck(
            id = ReadinessCheckId.ALARM_VOLUME,
            severity = ReadinessSeverityDto.BLOCKING_FOR_ALARM,
            outcome = ReadinessOutcome.FAILED,
            action = ReadinessAction.OpenSoundSettings,
        )

    private fun armedSnapshot(
        state: SessionStateDto = SessionStateDto.ARMED,
        failureCode: String? = null,
    ) = SessionSnapshotDto(
        schemaVersion = 1,
        revision = 2,
        sessionId = "11111111-1111-1111-1111-111111111111",
        wakeSchedule =
            WakeScheduleDto(
                localDateIso = "2026-09-04",
                localTimeIso = "07:00",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = paris(2026, 9, 4, 7, 0),
            ),
        state = state,
        releaseTarget = null,
        health = SessionHealthDto.HEALTHY,
        createdAtEpochMillis = now,
        armedAtEpochMillis = now,
        ringingAtEpochMillis = null,
        alarmSoundStoppedAtEpochMillis = null,
        triggerElapsedAtEpochMillis = null,
        nfcVerifiedAtEpochMillis = null,
        releasingAtEpochMillis = null,
        completedAtEpochMillis = null,
        cancelledAtEpochMillis = null,
        failureCode = failureCode,
    )

    private fun viewModel(): SummaryViewModel {
        val useCase =
            ArmSessionUseCase(
                readinessChecker = readinessChecker,
                facade = NiumiCoreFacade(),
                coordinator = coordinator,
                eventFactory = SessionEventFactory(UuidIdGenerator(), FakeClock(now)),
                sources = ActivationSources(pairedBoxStore, appSelectionStore, timeZoneProvider),
            )
        return SummaryViewModel(useCase, snapshotPublisher)
    }

    private fun loadedViewModel(): SummaryViewModel =
        viewModel().also { it.refresh(localTimeIso = "07:00", use24Hour = true) }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aNominalPreviewShowsTheScheduleTheApplicationsAndTheTruncatedBoxId() {
        val viewModel = loadedViewModel()

        assertThat(viewModel.state.display?.relativeDayLabel).isEqualTo("Demain")
        assertThat(viewModel.state.blockedPackages).containsExactlyElementsIn(selection)
        assertThat(viewModel.state.truncatedBoxId).isEqualTo("b0c1d2e3")
        assertThat(viewModel.state.canActivate).isTrue()
    }

    @Test
    fun aBlockingCheckDisablesTheActivationButton() {
        readinessChecker.reportForCandidate = { candidate ->
            report(candidateTriggerAtEpochMillis = candidate, checks = listOf(failedCheck()))
        }

        val viewModel = loadedViewModel()

        assertThat(viewModel.state.canActivate).isFalse()
        assertThat(viewModel.state.message).isEqualTo(SummaryTexts.blockingReason("READINESS_BLOCKING_FOR_ALARM"))
    }

    @Test
    fun anEmptySelectionDisablesTheActivationButton() {
        readinessChecker.reportForCandidate = { candidate ->
            report(candidateTriggerAtEpochMillis = candidate, appSelectionCount = 0)
        }

        val viewModel = loadedViewModel()

        assertThat(viewModel.state.canActivate).isFalse()
    }

    @Test
    fun aMissingPairedBoxDisablesTheActivationButton() {
        readinessChecker.reportForCandidate = { candidate ->
            report(candidateTriggerAtEpochMillis = candidate, hasPairedBox = false)
        }
        pairedBoxStore.credential = null

        val viewModel = loadedViewModel()

        assertThat(viewModel.state.canActivate).isFalse()
        assertThat(viewModel.state.truncatedBoxId).isNull()
    }

    @Test
    fun refreshReplaysTheDiagnosis() {
        val viewModel = loadedViewModel()
        val callsAfterFirstLoad = readinessCheckCalls

        viewModel.refresh(localTimeIso = "07:00", use24Hour = true)

        assertThat(readinessCheckCalls).isGreaterThan(callsAfterFirstLoad)
    }

    @Test
    fun activatingWithAGreenDiagnosisExposesTheArmedSnapshot() {
        val snapshot = armedSnapshot()
        coordinator.result = DispatchResult.Applied(snapshot, requiredEffectsSucceeded = true)
        val viewModel = loadedViewModel()

        viewModel.activate("07:00")

        assertThat(viewModel.armedSnapshot).isEqualTo(snapshot)
        assertThat(viewModel.state.isActivating).isFalse()
    }

    @Test
    fun activatingWithABlockingCheckNeverReachesTheUseCase() {
        readinessChecker.reportForCandidate = { candidate ->
            report(candidateTriggerAtEpochMillis = candidate, checks = listOf(failedCheck()))
        }
        val viewModel = loadedViewModel()

        viewModel.activate("07:00")

        assertThat(coordinator.dispatchCount).isEqualTo(0)
        assertThat(viewModel.armedSnapshot).isNull()
    }

    @Test
    fun aCoordinatorFailureNamesItsFailureCodeAndStillAllowsARetry() {
        coordinator.result =
            DispatchResult.Applied(
                armedSnapshot(state = SessionStateDto.FAILED, failureCode = "ANDROID_ALARM_SCHEDULE_FAILED"),
                requiredEffectsSucceeded = false,
            )
        val viewModel = loadedViewModel()

        viewModel.activate("07:00")

        assertThat(viewModel.state.message).contains("ANDROID_ALARM_SCHEDULE_FAILED")
        assertThat(viewModel.armedSnapshot).isNull()

        // `ACTIVATION_FAILED` a effacé le pointeur actif : un nouveau diagnostic redonne la main.
        viewModel.refresh(localTimeIso = "07:00", use24Hour = true)

        assertThat(viewModel.state.canActivate).isTrue()
    }

    @Test
    fun aRejectedDispatchShowsADistinctMessage() {
        coordinator.result =
            DispatchResult.Rejected(listOf(DomainViolationDto("INVALID_STATE_TRANSITION", "Session déjà active")))
        val viewModel = loadedViewModel()

        viewModel.activate("07:00")

        assertThat(viewModel.state.message).isEqualTo(SummaryTexts.REJECTED_MESSAGE)
    }

    @Test
    fun aSessionAlreadyInProgressDisablesTheActivationButton() {
        val viewModel = loadedViewModel()
        assertThat(viewModel.state.canActivate).isTrue()

        snapshotPublisher.publish(armedSnapshot())

        assertThat(viewModel.state.isSessionInProgress).isTrue()
        assertThat(viewModel.state.canActivate).isFalse()
    }

    @Test
    fun aFinalSnapshotIsNotASessionInProgress() {
        val viewModel = loadedViewModel()

        snapshotPublisher.publish(armedSnapshot(state = SessionStateDto.COMPLETED))

        assertThat(viewModel.state.isSessionInProgress).isFalse()
        assertThat(viewModel.state.canActivate).isTrue()
    }

    @Test
    fun theStateNeverCarriesTheTokenNorTheFullBoxId() {
        val viewModel = loadedViewModel()

        val rendered = viewModel.state.toString()

        assertThat(rendered).doesNotContain(credential.tokenSha256Hex)
        assertThat(viewModel.state.truncatedBoxId).isNotEqualTo(credential.boxId)
        assertThat(viewModel.state.truncatedBoxId!!.length).isLessThan(credential.boxId.length)
    }
}
