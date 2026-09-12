package com.niumi.feature.session.activation

import com.google.common.truth.Truth.assertThat
import com.niumi.core.diagnostics.ActivationReasonCode
import com.niumi.core.interop.DomainViolationDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.core.interop.WakeScheduleStatusDto
import com.niumi.database.BlockedPackage
import com.niumi.database.EventReceipt
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
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Activation en deux phases (SPEC_ANDROID §9.2, SPEC_CORE_KMP §10). Le journal ne couvre que les
 * points 1 à 3 : les points 4 à 9 sont internes au coordinateur et prouvés dans `:core:system`
 * (voir le tableau de traçabilité du KDoc d'[ArmSessionUseCase]). La façade commune et
 * `SessionEventFactory` sont réelles : aucune politique d'activation n'est simulée ici.
 */
class ArmSessionUseCaseTest {
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
    private val selection =
        listOf(
            BlockedPackage("com.example.chat", "Chat"),
            BlockedPackage("com.example.social", "Réseau social"),
        )

    private val readinessChecker =
        RecordingReadinessChecker(
            journal,
            reportForNull = report(candidateTriggerAtEpochMillis = null),
            reportForCandidate = { candidate -> report(candidateTriggerAtEpochMillis = candidate) },
        )
    private val timeZoneProvider = RecordingTimeZoneProvider(journal, zoneId = "Europe/Paris")
    private val pairedBoxStore = RecordingPairedBoxStore(journal, credential)
    private val appSelectionStore = RecordingAppSelectionStore(journal, selection)
    private val coordinator = RecordingSessionCoordinator(journal)

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

    private fun failedCheck(severity: ReadinessSeverityDto = ReadinessSeverityDto.BLOCKING_FOR_ALARM) =
        ReadinessCheck(
            id = ReadinessCheckId.ALARM_VOLUME,
            severity = severity,
            outcome = ReadinessOutcome.FAILED,
            action = ReadinessAction.OpenSoundSettings,
        )

    private fun useCase(): ArmSessionUseCase =
        ArmSessionUseCase(
            readinessChecker = readinessChecker,
            facade = NiumiCoreFacade(),
            coordinator = coordinator,
            eventFactory = SessionEventFactory(idGenerator = UuidIdGenerator(), clock = FakeClock(now)),
            sources = ActivationSources(pairedBoxStore, appSelectionStore, timeZoneProvider),
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

    @Test
    fun theNominalPathFollowsTheOrderOfTheFirstThreePointsOfTheSpec() =
        runTest {
            coordinator.result = DispatchResult.Applied(armedSnapshot(), requiredEffectsSucceeded = true)

            useCase().arm("07:00")

            assertThat(journal.snapshot())
                .containsExactly(
                    "readiness.check(null)",
                    "timeZone.current",
                    "readiness.check(candidate)",
                    "pairedBox.current",
                    "appSelection.selection",
                    "coordinator.dispatch(${SessionEventKindDto.ACTIVATION_REQUESTED})",
                ).inOrder()
        }

    @Test
    fun aBlockingCheckRefusesActivationWithoutAnyDispatch() =
        runTest {
            readinessChecker.reportForCandidate = { candidate ->
                report(candidateTriggerAtEpochMillis = candidate, checks = listOf(failedCheck()))
            }

            val result = useCase().arm("07:00")

            assertThat(result).isInstanceOf(ArmSessionResult.Failed::class.java)
            assertThat((result as ArmSessionResult.Failed).failure).isInstanceOf(ActivationFailure.Blocked::class.java)
            assertThat(coordinator.dispatchCount).isEqualTo(0)
        }

    @Test
    fun anEmptyApplicationSelectionRefusesActivationWithoutAnyDispatch() =
        runTest {
            readinessChecker.reportForCandidate = { candidate ->
                report(candidateTriggerAtEpochMillis = candidate, appSelectionCount = 0)
            }

            val result = useCase().arm("07:00")

            val failure = (result as ArmSessionResult.Failed).failure as ActivationFailure.Blocked
            assertThat(failure.reasons.map { it.code }).contains(ActivationReasonCode.INVALID_APP_SELECTION)
            assertThat(coordinator.dispatchCount).isEqualTo(0)
        }

    @Test
    fun fiftyOneApplicationsRefuseActivationWithoutAnyDispatch() =
        runTest {
            readinessChecker.reportForCandidate = { candidate ->
                report(candidateTriggerAtEpochMillis = candidate, appSelectionCount = 51)
            }

            val result = useCase().arm("07:00")

            val failure = (result as ArmSessionResult.Failed).failure as ActivationFailure.Blocked
            assertThat(failure.reasons.map { it.code }).contains(ActivationReasonCode.INVALID_APP_SELECTION)
            assertThat(coordinator.dispatchCount).isEqualTo(0)
        }

    @Test
    fun aMissingPairedBoxRefusesActivationWithoutAnyDispatch() =
        runTest {
            readinessChecker.reportForCandidate = { candidate ->
                report(candidateTriggerAtEpochMillis = candidate, hasPairedBox = false)
            }
            pairedBoxStore.credential = null

            val result = useCase().arm("07:00")

            val failure = (result as ArmSessionResult.Failed).failure as ActivationFailure.Blocked
            assertThat(failure.reasons.map { it.code }).contains(ActivationReasonCode.NO_PAIRED_BOX)
            assertThat(coordinator.dispatchCount).isEqualTo(0)
        }

    @Test
    fun theCredentialIsReadExactlyOnceAndFrozenIntoTheExtras() =
        runTest {
            coordinator.result = DispatchResult.Applied(armedSnapshot(), requiredEffectsSucceeded = true)

            useCase().arm("07:00")

            assertThat(pairedBoxStore.currentCallCount).isEqualTo(1)
            assertThat(coordinator.lastExtras?.boxId).isEqualTo(credential.boxId)
            assertThat(coordinator.lastExtras?.boxTokenSha256Hex).isEqualTo(credential.tokenSha256Hex)
        }

    @Test
    fun theExtrasCarryTheProductionRingtoneVibrationAndFrozenLabels() =
        runTest {
            coordinator.result = DispatchResult.Applied(armedSnapshot(), requiredEffectsSucceeded = true)

            useCase().arm("07:00")

            assertThat(coordinator.lastExtras?.ringtoneKey).isEqualTo("niumi_alarm")
            assertThat(coordinator.lastExtras?.vibrationEnabled).isTrue()
            assertThat(coordinator.lastExtras?.blockedPackages).containsExactlyElementsIn(selection).inOrder()
        }

    @Test
    fun theRequestedCountMatchesTheFrozenPackagesFromASingleRead() =
        runTest {
            coordinator.result = DispatchResult.Applied(armedSnapshot(), requiredEffectsSucceeded = true)

            useCase().arm("07:00")

            val count =
                coordinator.lastEvent
                    ?.activationRequest
                    ?.appSelection
                    ?.count
            assertThat(count).isEqualTo(coordinator.lastExtras?.blockedPackages?.size)
        }

    @Test
    fun theTimeZoneIsRecomputedAtActivationRatherThanInheritedFromTheWakeTimeScreen() =
        runTest {
            coordinator.result = DispatchResult.Applied(armedSnapshot(), requiredEffectsSucceeded = true)
            timeZoneProvider.zoneId = "America/New_York"

            useCase().arm("07:00")

            val schedule = coordinator.lastEvent?.activationRequest?.wakeSchedule
            assertThat(schedule?.zoneIdAtActivation).isEqualTo("America/New_York")
            assertThat(schedule?.triggerAtEpochMillis).isNotEqualTo(paris(2026, 9, 4, 7, 0))
        }

    @Test
    fun anUnparseableLocalTimeRefusesActivationBeforeReadingAnyRepository() =
        runTest {
            val result = useCase().arm("25:00")

            val failure = (result as ArmSessionResult.Failed).failure as ActivationFailure.InvalidSchedule
            assertThat(failure.status).isEqualTo(WakeScheduleStatusDto.INVALID_TIME)
            assertThat(pairedBoxStore.currentCallCount).isEqualTo(0)
            assertThat(coordinator.dispatchCount).isEqualTo(0)
        }

    @Test
    fun anAppliedArmedSnapshotIsReportedAsArmed() =
        runTest {
            val snapshot = armedSnapshot()
            coordinator.result = DispatchResult.Applied(snapshot, requiredEffectsSucceeded = true)

            val result = useCase().arm("07:00")

            assertThat(result).isEqualTo(ArmSessionResult.Armed(snapshot))
        }

    @Test
    fun anAppliedFailedSnapshotCarriesItsFailureCodeBack() =
        runTest {
            coordinator.result =
                DispatchResult.Applied(
                    armedSnapshot(state = SessionStateDto.FAILED, failureCode = "ANDROID_ALARM_SCHEDULE_FAILED"),
                    requiredEffectsSucceeded = false,
                )

            val result = useCase().arm("07:00")

            val failure = (result as ArmSessionResult.Failed).failure as ActivationFailure.CoordinatorFailed
            assertThat(failure.failureCode).isEqualTo("ANDROID_ALARM_SCHEDULE_FAILED")
            assertThat(failure.state).isEqualTo(SessionStateDto.FAILED)
        }

    @Test
    fun anAppliedResultWithoutSnapshotIsReportedAsACoordinatorFailure() =
        runTest {
            coordinator.result = DispatchResult.Applied(snapshot = null, requiredEffectsSucceeded = false)

            val result = useCase().arm("07:00")

            val failure = (result as ArmSessionResult.Failed).failure as ActivationFailure.CoordinatorFailed
            assertThat(failure.failureCode).isNull()
            assertThat(failure.state).isNull()
        }

    @Test
    fun aRejectedDispatchCarriesTheViolationsBackWithoutRetrying() =
        runTest {
            val violations = listOf(DomainViolationDto("INVALID_STATE_TRANSITION", "Session déjà active"))
            coordinator.result = DispatchResult.Rejected(violations)

            val result = useCase().arm("07:00")

            val failure = (result as ArmSessionResult.Failed).failure as ActivationFailure.Rejected
            assertThat(failure.violations).isEqualTo(violations)
            assertThat(coordinator.dispatchCount).isEqualTo(1)
        }

    @Test
    fun aDuplicateDispatchNeverClaimsAnArmedSession() =
        runTest {
            coordinator.result =
                DispatchResult.Duplicate(
                    EventReceipt(
                        eventId = "22222222-2222-2222-2222-222222222222",
                        sessionId = "11111111-1111-1111-1111-111111111111",
                        payloadSha256Hex = "b".repeat(64),
                        appliedRevision = 1,
                        receivedAtEpochMillis = now,
                    ),
                )

            val result = useCase().arm("07:00")

            assertThat((result as ArmSessionResult.Failed).failure).isEqualTo(ActivationFailure.Duplicate)
        }

    @Test
    fun theDispatchAlwaysCarriesNonNullAndroidExtras() =
        runTest {
            coordinator.result = DispatchResult.Applied(armedSnapshot(), requiredEffectsSucceeded = true)

            useCase().arm("07:00")

            assertThat(coordinator.lastExtras).isNotNull()
        }

    @Test
    fun previewNeverDispatches() =
        runTest {
            useCase().preview("07:00")

            assertThat(coordinator.dispatchCount).isEqualTo(0)
        }

    @Test
    fun previewReportsWhatTheSummaryNeedsWithoutLeakingTheToken() =
        runTest {
            val preview = useCase().preview("07:00")

            assertThat(preview.canActivate).isTrue()
            assertThat(preview.boxId).isEqualTo(credential.boxId)
            assertThat(preview.blockedPackages).containsExactlyElementsIn(selection).inOrder()
            assertThat(preview.scheduleResult.status).isEqualTo(WakeScheduleStatusDto.VALID)
        }

    @Test
    fun armDoesNotReuseThePreviewDiagnosis() =
        runTest {
            coordinator.result = DispatchResult.Applied(armedSnapshot(), requiredEffectsSucceeded = true)
            val useCase = useCase()

            useCase.preview("07:00")
            useCase.arm("07:00")

            assertThat(journal.snapshot().count { it == "readiness.check(candidate)" }).isEqualTo(2)
        }

    @Test
    fun aWarningOnlyReportStillAllowsActivation() =
        runTest {
            readinessChecker.reportForCandidate = { candidate ->
                report(
                    candidateTriggerAtEpochMillis = candidate,
                    checks = listOf(failedCheck(severity = ReadinessSeverityDto.WARNING)),
                )
            }
            coordinator.result = DispatchResult.Applied(armedSnapshot(), requiredEffectsSucceeded = true)

            val result = useCase().arm("07:00")

            assertThat(result).isInstanceOf(ArmSessionResult.Armed::class.java)
        }

    @Test
    fun theDispatchedEventIsAnActivationRequestAcceptedByTheRealEngine() =
        runTest {
            coordinator.result = DispatchResult.Applied(armedSnapshot(), requiredEffectsSucceeded = true)

            useCase().arm("07:00")

            val event = coordinator.lastEvent!!
            assertThat(event.kind).isEqualTo(SessionEventKindDto.ACTIVATION_REQUESTED)
            assertThat(event.expectedRevision).isNull()
            assertThat(NiumiCoreFacade().reduce(null, event).violations).isEmpty()
        }
}
