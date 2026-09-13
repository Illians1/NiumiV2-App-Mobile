package com.niumi.system.session

import android.app.NotificationManager
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.readiness.AndroidIncidentCodes
import com.niumi.system.ringing.ServiceCommandExtras
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Déclenchement de l'alarme par le coordinateur (SPEC_ANDROID §10.1, §13, §13.1 ; SPEC_CORE_KMP
 * §6). `AlarmReceiver` n'est plus qu'une coquille : toute la décision se prouve ici en JVM.
 */
class AlarmTriggerHandlerTest {
    private val activationEventId = "00000000-0000-0000-0000-000000000001"

    private fun handlerFor(harness: TestCoordinatorHarness) =
        AlarmTriggerHandler(
            gateway = harness.gateway,
            coordinator = harness.coordinator,
            eventFactory = harness.eventFactory,
            readinessMonitor = harness.readinessMonitor,
            technicalEventLog = harness.technicalEventLog,
        )

    /** Arme une session réelle par le coordinateur : `PREPARING` (révision 1) puis `ARMED` (2). */
    private suspend fun armedHarness(): TestCoordinatorHarness {
        val harness = TestCoordinatorHarness()
        harness.coordinator.dispatch(
            SessionDtoFixtures.activationRequested(eventId = activationEventId),
            SessionDtoFixtures.extras(),
        )
        return harness
    }

    private suspend fun TestCoordinatorHarness.currentSnapshot() = (gateway.load() as LoadResult.Present).snapshot

    private fun extrasFor(
        sessionId: String = SessionDtoFixtures.SESSION_ID,
        revision: Long,
    ) = ServiceCommandExtras(sessionId = sessionId, revision = revision)

    @Test
    fun anArmedSessionDispatchesAlarmFiredAndStartsRinging() =
        runTest {
            val harness = armedHarness()
            val armedRevision = harness.currentSnapshot().revision

            val outcome = handlerFor(harness).handle(extrasFor(revision = armedRevision))

            assertThat(outcome).isInstanceOf(AlarmTriggerOutcome.Dispatched::class.java)
            val dispatched = (outcome as AlarmTriggerOutcome.Dispatched).result
            assertThat(dispatched).isInstanceOf(DispatchResult.Applied::class.java)
            assertThat((dispatched as DispatchResult.Applied).snapshot?.state)
                .isEqualTo(SessionStateDto.RINGING)
            assertThat(harness.journal.calls).contains("RingingController.startRinging")
        }

    @Test
    fun alarmReceivedIsLoggedWithTheSessionIdBeforeAnyDecision() =
        runTest {
            val harness = armedHarness()
            harness.technicalEventLog.entries.clear()

            handlerFor(harness).handle(extrasFor(revision = harness.currentSnapshot().revision))

            assertThat(harness.technicalEventLog.entries.first())
                .isEqualTo(TechnicalEventType.ALARM_RECEIVED to SessionDtoFixtures.SESSION_ID)
        }

    /**
     * Le cas qui aurait rendu le réveil muet avec une garde d'égalité stricte : §13.1 fabrique des
     * incidents pendant `ARMED`, `INCIDENT_REPORTED` incrémente la révision et ne reprogramme
     * jamais l'alarme — l'extra du `PendingIntent` est donc couramment en retard.
     */
    @Test
    fun aRevisionOlderThanTheSnapshotStillRings() =
        runTest {
            val harness = armedHarness()
            val armedRevision = harness.currentSnapshot().revision
            harness.coordinator.dispatch(
                harness.eventFactory.incidentReported(
                    harness.currentSnapshot(),
                    harness.eventFactory.buildIncident("ANY_CODE", IncidentSeverityDto.DEGRADED),
                ),
            )
            assertThat(harness.currentSnapshot().revision).isGreaterThan(armedRevision)

            val outcome = handlerFor(harness).handle(extrasFor(revision = armedRevision))

            val dispatched = (outcome as AlarmTriggerOutcome.Dispatched).result
            assertThat((dispatched as DispatchResult.Applied).snapshot?.state)
                .isEqualTo(SessionStateDto.RINGING)
        }

    @Test
    fun aRevisionAheadOfTheSnapshotIsRefusedButStillLogged() =
        runTest {
            val harness = armedHarness()
            val armedRevision = harness.currentSnapshot().revision
            harness.technicalEventLog.entries.clear()

            val outcome = handlerFor(harness).handle(extrasFor(revision = armedRevision + 1))

            assertThat(outcome).isEqualTo(AlarmTriggerOutcome.RevisionAhead)
            assertThat(harness.currentSnapshot().state).isEqualTo(SessionStateDto.ARMED)
            assertThat(harness.journal.calls).doesNotContain("RingingController.startRinging")
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.ALARM_RECEIVED)
        }

    @Test
    fun anUnknownSessionIdIsRefused() =
        runTest {
            val harness = armedHarness()
            val revision = harness.currentSnapshot().revision

            val outcome =
                handlerFor(harness).handle(
                    extrasFor(sessionId = SessionDtoFixtures.OTHER_SESSION_ID, revision = revision),
                )

            assertThat(outcome).isEqualTo(AlarmTriggerOutcome.UnknownSession)
            assertThat(harness.currentSnapshot().state).isEqualTo(SessionStateDto.ARMED)
            assertThat(harness.journal.calls).doesNotContain("RingingController.startRinging")
        }

    /** Un `PendingIntent` qui survit à une session effacée ne réveille personne. */
    @Test
    fun anAbsentSessionDispatchesNothing() =
        runTest {
            val harness = TestCoordinatorHarness()

            val outcome = handlerFor(harness).handle(extrasFor(revision = 2L))

            assertThat(outcome).isEqualTo(AlarmTriggerOutcome.NoSession)
            assertThat(harness.journal.calls).doesNotContain("RingingController.startRinging")
        }

    /** SPEC_CORE_KMP §13 : un snapshot illisible ne se lit jamais « pas de session ». */
    @Test
    fun anUnreadableSnapshotDispatchesNothingAndClearsNothing() =
        runTest {
            val harness = armedHarness()
            val revision = harness.currentSnapshot().revision
            harness.gateway.forceUnreadable = "SNAPSHOT_CORRUPTED"

            val outcome = handlerFor(harness).handle(extrasFor(revision = revision))

            assertThat(outcome).isEqualTo(AlarmTriggerOutcome.UnreadableSnapshot)
            assertThat(harness.journal.calls).doesNotContain("gateway.clearActive")
            harness.gateway.forceUnreadable = null
            assertThat(harness.currentSnapshot().state).isEqualTo(SessionStateDto.ARMED)
        }

    @Test
    fun invalidExtrasAreRefusedAndLoggedWithoutASessionId() =
        runTest {
            val harness = armedHarness()
            harness.technicalEventLog.entries.clear()

            val outcome =
                handlerFor(harness).handle(ServiceCommandExtras(sessionId = null, revision = 2L))

            assertThat(outcome).isEqualTo(AlarmTriggerOutcome.InvalidCommand)
            assertThat(harness.technicalEventLog.entries)
                .containsExactly(TechnicalEventType.ALARM_RECEIVED to null)
            assertThat(harness.currentSnapshot().state).isEqualTo(SessionStateDto.ARMED)
        }

    /**
     * SPEC_ANDROID §13 : « `AlarmReceiver` relit le filtre d'interruption au moment de sonner et,
     * s'il vaut `INTERRUPTION_FILTER_NONE`, crée un incident `ANDROID_ALARM_MUTED_BY_DND` de
     * gravité `CRITICAL` ». La détection passe par la surveillance de §13.1, pas par un second
     * lecteur : une seconde voie recréerait les doublons corrigés à l'étape 16.
     */
    @Test
    fun totalSilenceRecordsTheDndIncidentAndItsTechnicalEvents() =
        runTest {
            val harness = armedHarness()
            harness.readinessSources.interruptionFilterSource.filter =
                NotificationManager.INTERRUPTION_FILTER_NONE

            handlerFor(harness).handle(extrasFor(revision = harness.currentSnapshot().revision))

            val recorded = harness.gateway.incidentsRecorded.map { it.second }
            val dndIncident = recorded.single { it.code == AndroidIncidentCodes.ALARM_MUTED_BY_DND }
            assertThat(dndIncident.severity).isEqualTo(IncidentSeverityDto.CRITICAL)
            assertThat(harness.technicalEventLog.logged)
                .containsAtLeast(
                    TechnicalEventType.SESSION_READINESS_DEGRADED,
                    TechnicalEventType.ALARM_MUTED_BY_DND,
                )
        }

    /** §13 : « sans empêcher le reste de la chaîne : la session reste active et le blocage est
     * conservé ». C'est aussi la preuve que le snapshot est relu après la surveillance — sinon
     * `ALARM_FIRED` porterait une révision périmée et serait rejeté en `STALE_REVISION`. */
    @Test
    fun theDndIncidentDoesNotPreventTheAlarmFromRinging() =
        runTest {
            val harness = armedHarness()
            harness.readinessSources.interruptionFilterSource.filter =
                NotificationManager.INTERRUPTION_FILTER_NONE

            val outcome =
                handlerFor(harness).handle(extrasFor(revision = harness.currentSnapshot().revision))

            val dispatched = (outcome as AlarmTriggerOutcome.Dispatched).result
            assertThat(dispatched).isInstanceOf(DispatchResult.Applied::class.java)
            assertThat((dispatched as DispatchResult.Applied).snapshot?.state)
                .isEqualTo(SessionStateDto.RINGING)
            assertThat(harness.blockingController.effectivePackages()).isNotEmpty()
        }

    /** L'incident précède la sonnerie : après `ALARM_FIRED`, l'état est `RINGING` et
     * `monitoredChecksFor` ne surveille plus que l'accessibilité — le DND ne serait jamais vu. */
    @Test
    fun readinessIsEvaluatedBeforeTheAlarmFiredDispatch() =
        runTest {
            val harness = armedHarness()
            harness.readinessSources.interruptionFilterSource.filter =
                NotificationManager.INTERRUPTION_FILTER_NONE

            handlerFor(harness).handle(extrasFor(revision = harness.currentSnapshot().revision))

            val incidentIndex = harness.journal.calls.indexOf("gateway.recordIncident")
            val ringingIndex = harness.journal.calls.indexOf("RingingController.startRinging")
            assertThat(incidentIndex).isAtLeast(0)
            assertThat(incidentIndex).isLessThan(ringingIndex)
        }

    /** La déduplication de l'étape 16 tient aussi sur ce chemin : l'avertissement est republié,
     * l'incident ne l'est pas. */
    @Test
    fun anAlreadyRecordedDndIncidentIsNotWrittenTwice() =
        runTest {
            val harness = armedHarness()
            harness.readinessSources.interruptionFilterSource.filter =
                NotificationManager.INTERRUPTION_FILTER_NONE
            harness.incidentsReader.record(
                SessionDtoFixtures.SESSION_ID,
                harness.eventFactory.buildIncident(
                    AndroidIncidentCodes.ALARM_MUTED_BY_DND,
                    IncidentSeverityDto.CRITICAL,
                ),
            )

            handlerFor(harness).handle(extrasFor(revision = harness.currentSnapshot().revision))

            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .doesNotContain(AndroidIncidentCodes.ALARM_MUTED_BY_DND)
            assertThat(harness.warningNotifier.presented).isNotEmpty()
        }

    /** La règle d'état reste dans `:shared:core` : le handler ne la duplique pas. */
    @Test
    fun aSessionAlreadyRingingIsRefusedByTheEngine() =
        runTest {
            val harness = armedHarness()
            val handler = handlerFor(harness)
            handler.handle(extrasFor(revision = harness.currentSnapshot().revision))

            val second = handler.handle(extrasFor(revision = harness.currentSnapshot().revision))

            val dispatched = (second as AlarmTriggerOutcome.Dispatched).result
            assertThat(dispatched).isInstanceOf(DispatchResult.Rejected::class.java)
            assertThat(harness.journal.calls.count { it == "RingingController.startRinging" })
                .isEqualTo(1)
        }
}
