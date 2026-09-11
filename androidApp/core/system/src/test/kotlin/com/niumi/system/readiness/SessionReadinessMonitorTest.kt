package com.niumi.system.readiness

import android.app.NotificationManager
import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.readiness.fakes.FakeSessionWarningNotifier
import com.niumi.system.readiness.fakes.ReadinessTestSources
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.SessionEventFactory
import com.niumi.system.session.fakes.FakeClock
import com.niumi.system.session.fakes.FakeTechnicalEventLog
import com.niumi.system.session.fakes.SequentialIdGenerator
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val NOW = 1_757_000_000_000L
private const val TRIGGER_AT = NOW + 8 * 3_600_000L
private const val ANDROID_16 = 36

/**
 * Surveillance pendant une session armée (SPEC_ANDROID §13.1) : un incident et une notification
 * par contrôle devenu faux, une seule fois tant que l'état ne change pas.
 */
class SessionReadinessMonitorTest {
    private val sources = ReadinessTestSources()
    private val notifier = FakeSessionWarningNotifier()
    private val technicalEventLog = FakeTechnicalEventLog()
    private val dispatched = mutableListOf<SessionEventDto>()

    private val monitor =
        SessionReadinessMonitor(
            readinessChecker = AndroidDeviceReadinessChecker(sources.build(), FakeClock(NOW), ANDROID_16),
            warningNotifier = notifier,
            eventFactory = SessionEventFactory(SequentialIdGenerator(), FakeClock(NOW)),
            technicalEventLog = technicalEventLog,
        )

    private val dispatch: suspend (SessionEventDto) -> DispatchResult = { event ->
        dispatched += event
        DispatchResult.Applied(snapshot(), requiredEffectsSucceeded = true)
    }

    private fun snapshot(state: SessionStateDto = SessionStateDto.ARMED) =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = 2,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule = WakeScheduleDto("2026-09-12", "07:00", "Europe/Paris", TRIGGER_AT),
            state = state,
            releaseTarget = null,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = NOW - 1_000L,
            armedAtEpochMillis = NOW - 900L,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
        )

    private fun breakCheck(id: ReadinessCheckId) {
        when (id) {
            ReadinessCheckId.EXACT_ALARM -> {
                sources.alarmScheduler.canScheduleExactValue = false
            }

            ReadinessCheckId.FULL_SCREEN_INTENT -> {
                sources.notificationAvailability.fullScreenAllowed = false
            }

            ReadinessCheckId.NOTIFICATIONS -> {
                sources.notificationAvailability.notificationsEnabled = false
            }

            ReadinessCheckId.ALARM_VOLUME -> {
                sources.alarmVolumeSource.volume = 0
            }

            ReadinessCheckId.DND_TOTAL_SILENCE -> {
                sources.interruptionFilterSource.filter = NotificationManager.INTERRUPTION_FILTER_NONE
            }

            ReadinessCheckId.ACCESSIBILITY_SERVICE -> {
                sources.accessibilityServiceStatus.enabled = false
            }

            else -> {
                error("Contrôle non surveillé : $id")
            }
        }
    }

    private fun repairCheck(id: ReadinessCheckId) {
        when (id) {
            ReadinessCheckId.EXACT_ALARM -> {
                sources.alarmScheduler.canScheduleExactValue = true
            }

            ReadinessCheckId.FULL_SCREEN_INTENT -> {
                sources.notificationAvailability.fullScreenAllowed = true
            }

            ReadinessCheckId.NOTIFICATIONS -> {
                sources.notificationAvailability.notificationsEnabled = true
            }

            ReadinessCheckId.ALARM_VOLUME -> {
                sources.alarmVolumeSource.volume = 7
            }

            ReadinessCheckId.DND_TOTAL_SILENCE -> {
                sources.interruptionFilterSource.filter = NotificationManager.INTERRUPTION_FILTER_ALL
            }

            ReadinessCheckId.ACCESSIBILITY_SERVICE -> {
                sources.accessibilityServiceStatus.enabled = true
            }

            else -> {
                error("Contrôle non surveillé : $id")
            }
        }
    }

    @Test
    fun anArmedSessionWithEveryControlHealthyReportsNothing() =
        runTest {
            val result = monitor.evaluate(snapshot(), dispatch)

            assertThat(result.failing).isEmpty()
            assertThat(result.newlyReported).isEmpty()
            assertThat(notifier.presented).isEmpty()
            assertThat(dispatched).isEmpty()
        }

    @Test
    fun eachMonitoredControlThatBreaksProducesItsOwnIncidentAndItsOwnWarning() =
        runTest {
            MonitoredReadinessChecks.incidentCodes.forEach { (checkId, expectedCode) ->
                val fresh = SessionReadinessMonitorTest()
                fresh.breakCheck(checkId)

                val result = fresh.monitor.evaluate(fresh.snapshot(), fresh.dispatch)

                assertThat(result.failing).contains(checkId)
                assertThat(result.newlyReported.map { it.checkId }).containsExactly(checkId)
                assertThat(result.newlyReported.single().incidentCode).isEqualTo(expectedCode)
                assertThat(fresh.notifier.presented).containsExactly(checkId)
                assertThat(fresh.dispatched.single().kind).isEqualTo(SessionEventKindDto.INCIDENT_REPORTED)
                assertThat(
                    fresh.dispatched
                        .single()
                        .incident
                        ?.code,
                ).isEqualTo(expectedCode)
                assertThat(fresh.technicalEventLog.logged)
                    .contains(TechnicalEventType.SESSION_READINESS_DEGRADED)
            }
        }

    @Test
    fun theTwoPermissionLossesKeepTheirCommonCodesRatherThanAnAndroidPrefix() =
        runTest {
            assertThat(MonitoredReadinessChecks.incidentCodes[ReadinessCheckId.EXACT_ALARM])
                .isEqualTo(IncidentCodes.ALARM_PERMISSION_REVOKED)
            assertThat(MonitoredReadinessChecks.incidentCodes[ReadinessCheckId.ACCESSIBILITY_SERVICE])
                .isEqualTo(IncidentCodes.BLOCKING_PERMISSION_REVOKED)
        }

    @Test
    fun aSecondPassWithoutAnyChangeReportsNothingAgain() =
        runTest {
            breakCheck(ReadinessCheckId.ALARM_VOLUME)

            monitor.evaluate(snapshot(), dispatch)
            val second = monitor.evaluate(snapshot(), dispatch)

            assertThat(second.newlyReported).isEmpty()
            // Le contrôle reste cassé : l'appelant doit pouvoir le savoir sans nouvel incident.
            assertThat(second.failing).containsExactly(ReadinessCheckId.ALARM_VOLUME)
            assertThat(notifier.presented).hasSize(1)
            assertThat(dispatched).hasSize(1)
        }

    @Test
    fun aControlThatRecoversClearsItsWarningAndCanBeReportedAgainLater() =
        runTest {
            breakCheck(ReadinessCheckId.DND_TOTAL_SILENCE)
            monitor.evaluate(snapshot(), dispatch)

            repairCheck(ReadinessCheckId.DND_TOTAL_SILENCE)
            val recovered = monitor.evaluate(snapshot(), dispatch)
            assertThat(recovered.failing).isEmpty()
            assertThat(notifier.cleared).containsExactly(ReadinessCheckId.DND_TOTAL_SILENCE)

            breakCheck(ReadinessCheckId.DND_TOTAL_SILENCE)
            val again = monitor.evaluate(snapshot(), dispatch)
            assertThat(again.newlyReported.map { it.checkId }).containsExactly(ReadinessCheckId.DND_TOTAL_SILENCE)
            assertThat(dispatched).hasSize(2)
        }

    @Test
    fun twoControlsBrokenTogetherProduceTwoIncidentsAndTwoWarnings() =
        runTest {
            breakCheck(ReadinessCheckId.ALARM_VOLUME)
            breakCheck(ReadinessCheckId.NOTIFICATIONS)

            val result = monitor.evaluate(snapshot(), dispatch)

            assertThat(result.newlyReported.map { it.checkId })
                .containsExactly(ReadinessCheckId.NOTIFICATIONS, ReadinessCheckId.ALARM_VOLUME)
            assertThat(notifier.presented).hasSize(2)
            assertThat(dispatched).hasSize(2)
        }

    @Test
    fun aSessionThatIsNoLongerArmedIsNotMonitoredAndItsWarningsAreWithdrawn() =
        runTest {
            breakCheck(ReadinessCheckId.ALARM_VOLUME)
            monitor.evaluate(snapshot(), dispatch)

            val result = monitor.evaluate(snapshot(SessionStateDto.RINGING), dispatch)

            assertThat(result.failing).isEmpty()
            assertThat(result.newlyReported).isEmpty()
            assertThat(notifier.clearAllCallCount).isEqualTo(1)
            assertThat(dispatched).hasSize(1)
        }
}
