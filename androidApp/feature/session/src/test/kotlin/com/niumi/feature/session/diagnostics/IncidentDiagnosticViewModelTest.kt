package com.niumi.feature.session.diagnostics

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.logging.DeviceContext
import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.database.logging.TechnicalEventType
import com.niumi.feature.session.active.fakes.FakeSessionIncidentsReader
import com.niumi.feature.session.active.fakes.FakeSessionPersistenceGateway
import com.niumi.feature.session.active.fakes.presentSession
import com.niumi.feature.session.wake.fakes.FakeTimeZoneProvider
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheck
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome
import com.niumi.system.readiness.ReadinessReport
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.StorageIntegrityState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Écran 12 — diagnostic d'incident (SPEC_ANDROID §15, §17, §18 ; SPEC_CORE_KMP §7.3). Présente les
 * incidents de la session (`CRITICAL` en tête), le résultat du dernier diagnostic de §13 et les
 * 200 événements techniques, et produit l'export texte.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IncidentDiagnosticViewModelTest {
    private val now = 1_700_000_000_000L
    private val boxId = "550e8400-e29b-41d4-a716-446655440000"

    private val snapshotPublisher = SessionSnapshotPublisher()
    private val gateway = FakeSessionPersistenceGateway()
    private val incidentsReader = FakeSessionIncidentsReader()
    private val technicalEventLog = ReplayingTechnicalEventLog()
    private val storageIntegrity = StorageIntegrityState()
    private val deviceContext =
        DeviceContext(deviceModel = "Pixel Test", androidVersion = "16 (API 36)", appVersion = "1.0.0 (1)")

    private var checks: List<ReadinessCheck> = emptyList()
    private val readinessChecker =
        DeviceReadinessChecker {
            ReadinessReport(
                checks = checks,
                appSelectionCount = 1,
                hasPairedBox = true,
                candidateTriggerAtEpochMillis = null,
                nowEpochMillis = now,
            )
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun snapshot() =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = 2,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule =
                WakeScheduleDto(
                    localDateIso = "2026-09-04",
                    localTimeIso = "07:00",
                    zoneIdAtActivation = "Europe/Paris",
                    triggerAtEpochMillis = now + 60_000L,
                ),
            state = SessionStateDto.ARMED,
            releaseTarget = null,
            health = SessionHealthDto.DEGRADED,
            createdAtEpochMillis = now,
            armedAtEpochMillis = now,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
        )

    private fun incident(
        code: String,
        severity: IncidentSeverityDto,
        occurredAtEpochMillis: Long = now,
    ) = SessionIncidentDto(
        code = code,
        severity = severity,
        occurredAtEpochMillis = occurredAtEpochMillis,
        platform = PlatformDto.ANDROID,
    )

    private fun viewModel() =
        IncidentDiagnosticViewModel(
            sources =
                DiagnosticSources(
                    snapshotPublisher = snapshotPublisher,
                    gateway = gateway,
                    incidentsReader = incidentsReader,
                    technicalEventLog = technicalEventLog,
                    readinessChecker = readinessChecker,
                    storageIntegrity = storageIntegrity,
                ),
            deviceContext = deviceContext,
            timeZoneProvider = FakeTimeZoneProvider(zoneId = "Europe/Paris"),
        )

    /** SPEC_CORE_KMP §7.3 : `CRITICAL` avant `DEGRADED` avant `WARNING`. */
    @Test
    fun incidentsAreOrderedBySeverityThenByMostRecent() {
        gateway.result = presentSession(snapshot(), emptyList())
        snapshotPublisher.publish(snapshot())
        incidentsReader.incidents =
            listOf(
                incident("TIME_CHANGED", IncidentSeverityDto.WARNING, now - 3_000L),
                incident("RELEASE_PARTIAL_FAILURE", IncidentSeverityDto.DEGRADED, now - 2_000L),
                incident("BLOCKING_PERMISSION_REVOKED", IncidentSeverityDto.CRITICAL, now - 1_000L),
                incident("ALARM_PERMISSION_REVOKED", IncidentSeverityDto.CRITICAL, now),
            )

        val viewModel = viewModel()
        viewModel.refresh()

        assertThat(viewModel.state.incidents.map { it.code })
            .containsExactly(
                "ALARM_PERMISSION_REVOKED",
                "BLOCKING_PERMISSION_REVOKED",
                "RELEASE_PARTIAL_FAILURE",
                "TIME_CHANGED",
            ).inOrder()
    }

    @Test
    fun theChecksOfTheLastReadinessRunArePresented() {
        gateway.result = presentSession(snapshot(), emptyList())
        snapshotPublisher.publish(snapshot())
        checks =
            listOf(
                ReadinessCheck(
                    id = ReadinessCheckId.ACCESSIBILITY_SERVICE,
                    severity = ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                    outcome = ReadinessOutcome.FAILED,
                    action = ReadinessAction.OpenAccessibilitySettings,
                ),
            )

        val viewModel = viewModel()
        viewModel.refresh()

        assertThat(viewModel.state.checks.map { it.id })
            .containsExactly(ReadinessCheckId.ACCESSIBILITY_SERVICE)
    }

    @Test
    fun theTechnicalEventsArePresented() {
        gateway.result = presentSession(snapshot(), emptyList())
        snapshotPublisher.publish(snapshot())
        technicalEventLog.entries =
            listOf(
                TechnicalEventEntry(
                    type = TechnicalEventType.SESSION_ARMED,
                    sessionId = "session-1",
                    detailsJson = null,
                    occurredAtEpochMillis = now,
                    deviceModel = deviceContext.deviceModel,
                    androidVersion = deviceContext.androidVersion,
                    appVersion = deviceContext.appVersion,
                ),
            )

        val viewModel = viewModel()
        viewModel.refresh()

        assertThat(viewModel.state.events.map { it.type }).containsExactly(TechnicalEventType.SESSION_ARMED)
    }

    /**
     * §17 : l'export masque le token, son hash complet et tout identifiant matériel. Le test part
     * des `extras` réels — qui **contiennent** `boxTokenSha256Hex` — pour prouver que le ViewModel
     * ne le transmet jamais à l'exporteur.
     */
    @Test
    fun theExportedTextNeverCarriesTheTokenHashAndTruncatesTheBoxId() {
        gateway.result = presentSession(snapshot(), emptyList())
        snapshotPublisher.publish(snapshot())
        val viewModel = viewModel()
        viewModel.refresh()

        val text = viewModel.exportText()

        assertThat(text).contains("550e8400")
        assertThat(text).doesNotContain(boxId)
        assertThat(text).doesNotContain("a".repeat(64))
    }

    @Test
    fun aDiagnosticWithoutAnySessionStillExports() {
        val viewModel = viewModel()
        viewModel.refresh()

        assertThat(viewModel.state.hasSession).isFalse()
        assertThat(viewModel.exportText()).contains(IncidentDiagnosticTexts.EXPORT_TITLE)
    }

    /**
     * SPEC_ANDROID §18, §20 : l'écran doit afficher la limite plutôt que rester en chargement
     * indéfini, et ne jamais présenter le blocage comme levé pendant qu'il l'est effectivement.
     */
    @Test
    fun aStorageFailureIsShownWithoutLeavingTheScreenLoading() {
        storageIntegrity.reportUnreadable("SQLITE_CORRUPT")

        val viewModel = viewModel()
        viewModel.refresh()

        assertThat(viewModel.state.storageFailureReason).isEqualTo("SQLITE_CORRUPT")
        assertThat(viewModel.state.isLoading).isFalse()
    }

    /**
     * Filet de sécurité : une source qui échoue malgré tout (au-delà de ce que Room protège déjà)
     * ne doit jamais laisser l'écran figé en chargement.
     */
    @Test
    fun anUnexpectedFailureIsShownWithoutLeavingTheScreenLoading() {
        gateway.result = presentSession(snapshot(), emptyList())
        snapshotPublisher.publish(snapshot())
        incidentsReader.failure = IllegalStateException("unexpected")

        val viewModel = viewModel()
        viewModel.refresh()

        assertThat(viewModel.state.isLoading).isFalse()
        assertThat(viewModel.state.storageFailureReason).isEqualTo("DIAGNOSTIC_UNAVAILABLE")
    }
}
