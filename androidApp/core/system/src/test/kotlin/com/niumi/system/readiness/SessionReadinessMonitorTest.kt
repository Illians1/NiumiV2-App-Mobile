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
import com.niumi.system.readiness.fakes.RecordingSessionIncidentsReader
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
    private val incidentsReader = RecordingSessionIncidentsReader()
    private val dispatched = mutableListOf<SessionEventDto>()

    /**
     * Fabrique plutôt qu'unique instance : la garde de déduplication des notifications vit dans le
     * monitor, donc un test qui veut repartir d'un état vierge — la même panne observée depuis
     * plusieurs états, ou après une mort de processus — a besoin d'une instance neuve. Les
     * incidents déjà enregistrés, eux, survivent dans [incidentsReader].
     */
    private fun newMonitor() =
        SessionReadinessMonitor(
            readinessChecker = AndroidDeviceReadinessChecker(sources.build(), FakeClock(NOW), ANDROID_16),
            warningNotifier = notifier,
            eventFactory = SessionEventFactory(SequentialIdGenerator(), FakeClock(NOW)),
            technicalEventLog = technicalEventLog,
            incidentsReader = incidentsReader,
        )

    private val monitor = newMonitor()

    /** Reflète en « base » ce que le monitor dispatche, comme le ferait `RecordIncidentExecutor`. */
    private val dispatch: suspend (SessionEventDto) -> DispatchResult = { event ->
        dispatched += event
        event.incident?.let { incidentsReader.record(event.sessionId, it) }
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

    /**
     * Changement de comportement de l'étape 16, assumé : un contrôle réparé puis re-cassé
     * **republie son avertissement** — l'utilisateur doit savoir que c'est de nouveau cassé — mais
     * n'enregistre **pas** un second incident. Un incident est un fait métier par session ; la
     * santé est déjà `DEGRADED` et n'en revient jamais (SPEC_CORE_KMP §7.3), si bien qu'un second
     * exemplaire n'ajouterait qu'un horodatage. Les deux détections restent dans le journal
     * technique, qui n'est pas dédupliqué.
     */
    @Test
    fun aControlThatRecoversRepublishesItsWarningButRecordsNoSecondIncident() =
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
            assertThat(notifier.presented).hasSize(2)
            assertThat(again.newlyReported.single().dispatchResult).isNull()
            assertThat(dispatched).hasSize(1)
            assertThat(technicalEventLog.logged.count { it == TechnicalEventType.ALARM_MUTED_BY_DND }).isEqualTo(2)
        }

    /**
     * Le défaut mesuré sur appareil à l'étape 16 : la déduplication vit en mémoire (§13.1) et ne
     * survit pas à une mort de processus, si bien qu'un monitor neuf réenregistrait un incident
     * pour un fait déjà consigné. L'écran 7 affichait alors deux fois le même texte avec deux
     * boutons identiques.
     *
     * Un monitor neuf doit republier l'avertissement — il est encore valable — sans réécrire
     * l'incident.
     */
    @Test
    fun afterAProcessDeathTheWarningIsRepublishedButTheIncidentIsNotRecordedTwice() =
        runTest {
            breakCheck(ReadinessCheckId.ACCESSIBILITY_SERVICE)
            monitor.evaluate(snapshot(), dispatch)
            assertThat(dispatched).hasSize(1)

            // Processus mort puis relancé : la garde en mémoire repart vide, la base non.
            val afterRestart = newMonitor().evaluate(snapshot(), dispatch)

            assertThat(afterRestart.newlyReported.map { it.checkId })
                .containsExactly(ReadinessCheckId.ACCESSIBILITY_SERVICE)
            assertThat(notifier.presented).hasSize(2)
            assertThat(dispatched).hasSize(1)
        }

    /**
     * Avant déverrouillage, `SessionIncidentsReader` ne peut rien lire (Room est inaccessible,
     * SPEC_ANDROID §7.3) et renvoie une liste vide. La déduplication est alors impossible : le
     * monitor enregistre l'incident plutôt que de le taire — même arbitrage qu'ailleurs, on ne
     * perd jamais une dégradation pour cause de stockage indisponible.
     */
    @Test
    fun beforeUnlockTheIncidentIsRecordedBecauseNothingCanBeReadBack() =
        runTest {
            incidentsReader.unreadable = true
            breakCheck(ReadinessCheckId.ACCESSIBILITY_SERVICE)
            monitor.evaluate(snapshot(), dispatch)

            newMonitor().evaluate(snapshot(), dispatch)

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

    /**
     * Étape 15 : au-delà de `ARMED`, les cinq contrôles de réveil se taisent. Un volume d'alarme
     * signalé pendant `ARMED` sort du périmètre en `RINGING` et son avertissement est retiré —
     * il resterait sinon affiché sans qu'aucune passe ne puisse plus le réévaluer.
     */
    @Test
    fun theWakeUpControlsGoSilentOnceTheSessionHasLeftArmed() =
        runTest {
            breakCheck(ReadinessCheckId.ALARM_VOLUME)
            monitor.evaluate(snapshot(), dispatch)

            val result = monitor.evaluate(snapshot(SessionStateDto.RINGING), dispatch)

            assertThat(result.failing).isEmpty()
            assertThat(result.newlyReported).isEmpty()
            assertThat(notifier.cleared).containsExactly(ReadinessCheckId.ALARM_VOLUME)
            assertThat(dispatched).hasSize(1)
        }

    /**
     * SPEC_ANDROID §12.2 : « si le service est désactivé pendant une session, Niumi doit le
     * détecter à sa prochaine exécution et afficher un incident ». Le blocage court jusqu'au scan
     * (§3), donc bien après `ARMED` — c'est le seul contrôle qui reste surveillé.
     */
    @Test
    fun theAccessibilityServiceIsStillMonitoredAfterArmed() =
        runTest {
            breakCheck(ReadinessCheckId.ACCESSIBILITY_SERVICE)

            listOf(
                SessionStateDto.RINGING,
                SessionStateDto.AWAITING_NFC,
                SessionStateDto.TRIGGERED_AWAITING_NFC,
                SessionStateDto.RELEASING,
            ).forEach { state ->
                val monitor = newMonitor()
                val result = monitor.evaluate(snapshot(state), dispatch)

                assertThat(result.failing).containsExactly(ReadinessCheckId.ACCESSIBILITY_SERVICE)
                assertThat(result.newlyReported.map { it.incidentCode })
                    .containsExactly(IncidentCodes.BLOCKING_PERMISSION_REVOKED)
            }
        }

    @Test
    fun aServiceLostWhileRingingIsReportedOnlyOnce() =
        runTest {
            breakCheck(ReadinessCheckId.ACCESSIBILITY_SERVICE)

            monitor.evaluate(snapshot(SessionStateDto.RINGING), dispatch)
            val second = monitor.evaluate(snapshot(SessionStateDto.RINGING), dispatch)

            assertThat(second.failing).containsExactly(ReadinessCheckId.ACCESSIBILITY_SERVICE)
            assertThat(second.newlyReported).isEmpty()
            assertThat(dispatched).hasSize(1)
        }

    /** Une session `ARMED` puis `RELEASING` ne doit pas resignaler le même service coupé. */
    @Test
    fun aServiceAlreadyReportedWhileArmedIsNotReportedAgainWhileReleasing() =
        runTest {
            breakCheck(ReadinessCheckId.ACCESSIBILITY_SERVICE)
            monitor.evaluate(snapshot(), dispatch)

            val result = monitor.evaluate(snapshot(SessionStateDto.RELEASING), dispatch)

            assertThat(result.newlyReported).isEmpty()
            assertThat(dispatched).hasSize(1)
        }

    @Test
    fun aFinishedSessionIsNotMonitoredAndAllItsWarningsAreWithdrawn() =
        runTest {
            breakCheck(ReadinessCheckId.ALARM_VOLUME)
            monitor.evaluate(snapshot(), dispatch)

            listOf(
                SessionStateDto.COMPLETED,
                SessionStateDto.CANCELLED,
                SessionStateDto.FAILED,
            ).forEach { state ->
                val result = monitor.evaluate(snapshot(state), dispatch)

                assertThat(result.failing).isEmpty()
                assertThat(result.newlyReported).isEmpty()
                // Un seul retrait global : les passes suivantes n'ont plus rien à retirer.
                assertThat(notifier.clearAllCallCount).isEqualTo(1)
            }
            assertThat(dispatched).hasSize(1)
        }
}
