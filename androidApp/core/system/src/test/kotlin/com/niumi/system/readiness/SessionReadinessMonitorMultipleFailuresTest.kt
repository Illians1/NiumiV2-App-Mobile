package com.niumi.system.readiness

import android.app.NotificationManager
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.system.session.LoadResult
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * SPEC_ANDROID §13.1 : « deux réglages cassés en même temps produisent deux avertissements
 * distincts ». Chacun porte son incident.
 *
 * Défaut mesuré sur appareil à l'étape 17 : le silence total fait aussi tomber le volume d'alarme
 * à zéro, donc deux contrôles surveillés échouent d'un seul coup. Le premier `INCIDENT_REPORTED`
 * incrémentant la révision, le second était bâti sur un snapshot périmé et rejeté en
 * `STALE_REVISION`, **silencieusement** : un seul incident était enregistré, et le journal laissait
 * croire que les deux l'avaient été.
 */
class SessionReadinessMonitorMultipleFailuresTest {
    private suspend fun armedHarness(): TestCoordinatorHarness {
        val harness = TestCoordinatorHarness()
        harness.gateway.commit(
            StoredDecision(
                snapshot = SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED, revision = 2),
                receipt =
                    EventReceipt(
                        eventId = "00000000-0000-0000-0000-00000000000b",
                        sessionId = SessionDtoFixtures.SESSION_ID,
                        payloadSha256Hex = "d".repeat(64),
                        appliedRevision = 2,
                        receivedAtEpochMillis = 1_000L,
                    ),
                effects = emptyList(),
                androidExtras = SessionDtoFixtures.extras(),
            ),
        )
        return harness
    }

    @Test
    fun twoChecksFailingTogetherEachRecordTheirIncident() =
        runTest {
            val harness = armedHarness()
            // Le silence total mute le flux d'alarme : les deux contrôles tombent ensemble, comme
            // sur appareil.
            harness.readinessSources.interruptionFilterSource.filter =
                NotificationManager.INTERRUPTION_FILTER_NONE
            harness.readinessSources.alarmVolumeSource.volume = 0

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            val codes = harness.gateway.incidentsRecorded.map { it.second.code }
            assertThat(codes).containsAtLeast(
                AndroidIncidentCodes.ALARM_MUTED_BY_DND,
                AndroidIncidentCodes.ALARM_VOLUME_ZERO,
            )
        }

    /** Chaque incident accepté fait avancer la révision : deux incidents, deux incréments. */
    @Test
    fun eachAcceptedIncidentAdvancesTheRevision() =
        runTest {
            val harness = armedHarness()
            harness.readinessSources.interruptionFilterSource.filter =
                NotificationManager.INTERRUPTION_FILTER_NONE
            harness.readinessSources.alarmVolumeSource.volume = 0

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat((harness.gateway.load() as LoadResult.Present).snapshot.revision).isEqualTo(4)
        }
}
