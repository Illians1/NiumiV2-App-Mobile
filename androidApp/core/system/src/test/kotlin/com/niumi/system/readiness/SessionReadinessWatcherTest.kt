package com.niumi.system.readiness

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * [SessionReadinessWatcher] n'avait aucun test dédié avant l'étape 20 : son comportement n'était
 * couvert qu'indirectement, à travers `SessionReconcilerBootTest` (côté réconciliateur) et
 * `SessionScanStatesTest` (côté règle partagée). Ce fichier prouve la branche propre au
 * déclencheur de premier plan : ce qu'il fait quand aucun snapshot n'a encore été publié dans ce
 * processus, et ce qu'il ne fait pas dans les autres cas.
 */
class SessionReadinessWatcherTest {
    /**
     * Défaut corrigé à l'étape 20. Un processus recréé après une mort pendant `RINGING` affiche un
     * `onResume` avant que `SessionStartupReconciler`, lancé en tâche de fond, ait eu le temps de
     * publier un snapshot. Sortir silencieusement sur `publisher.snapshot.value == null`, comme
     * avant cette étape, laissait cet `onResume` sans aucun effet.
     */
    @Test
    fun aForegroundPassReconcilesWhenNoSnapshotHasBeenPublishedYet() =
        runTest {
            val harness = TestCoordinatorHarness()
            val snapshot =
                SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED).copy(health = SessionHealthDto.HEALTHY)
            harness.gateway.commit(
                StoredDecision(
                    snapshot = snapshot,
                    receipt = EventReceipt("seed", snapshot.sessionId, "hash", 1, 900L),
                    effects = emptyList(),
                    androidExtras = SessionDtoFixtures.extras(),
                ),
            )
            // Rien n'a encore publié cette session dans ce processus, et son alarme a disparu avec
            // lui (redémarrage du service, mort de processus).
            assertThat(harness.publisher.snapshot.value).isNull()
            assertThat(harness.alarmScheduler.isScheduled(snapshot.sessionId)).isFalse()

            val watcher = watcherFor(harness, testScheduler)
            watcher.evaluate()

            val published = harness.publisher.snapshot.value
            assertThat(published?.sessionId).isEqualTo(snapshot.sessionId)
            assertThat(harness.alarmScheduler.isScheduled(snapshot.sessionId)).isTrue()
        }

    /**
     * Une session déjà connue de ce processus, dans un état qui n'attend pas de scan, ne doit
     * déclencher que la surveillance de §13.1 — jamais une réconciliation complète, qui tournerait
     * alors à chaque ouverture de l'application. Preuve : une alarme absente n'est **pas**
     * reprogrammée, alors qu'une réconciliation complète le ferait ([reconcileTriggerDelay]).
     */
    @Test
    fun aForegroundPassOnAPublishedNonScanStateOnlyMonitorsWithoutReconciling() =
        runTest {
            val harness = TestCoordinatorHarness()
            val snapshot =
                SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED).copy(health = SessionHealthDto.HEALTHY)
            harness.gateway.commit(
                StoredDecision(
                    snapshot = snapshot,
                    receipt = EventReceipt("seed", snapshot.sessionId, "hash", 1, 900L),
                    effects = emptyList(),
                    androidExtras = SessionDtoFixtures.extras(),
                ),
            )
            harness.publisher.publish(snapshot)

            val watcher = watcherFor(harness, testScheduler)
            watcher.evaluate()

            assertThat(harness.alarmScheduler.isScheduled(snapshot.sessionId)).isFalse()
            assertThat(harness.journal.calls).doesNotContain("AlarmScheduler.schedule")
        }

    /**
     * §12.2 : le service d'accessibilité reste surveillé dans tous les états non finaux, y compris
     * `RINGING` — qui n'est pas un état de scan et ne déclenche donc aucune réconciliation. La
     * surveillance de §13.1 doit malgré tout produire son incident.
     */
    @Test
    fun aForegroundPassStillMonitorsTheAccessibilityServiceOutsideArmed() =
        runTest {
            val harness = TestCoordinatorHarness()
            val snapshot =
                SessionDtoFixtures.snapshotInState(SessionStateDto.RINGING).copy(health = SessionHealthDto.HEALTHY)
            harness.gateway.commit(
                StoredDecision(
                    snapshot = snapshot,
                    receipt = EventReceipt("seed", snapshot.sessionId, "hash", 1, 900L),
                    effects = emptyList(),
                    androidExtras = SessionDtoFixtures.extras(),
                ),
            )
            harness.publisher.publish(snapshot)
            harness.accessibilityServiceStatus.enabled = false

            val watcher = watcherFor(harness, testScheduler)
            watcher.evaluate()

            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .contains(IncidentCodes.BLOCKING_PERMISSION_REVOKED)
        }

    /** §10.5, comportement de l'étape 19 préservé après l'élargissement de l'étape 20. */
    @Test
    fun aForegroundPassOnAScanStateStillRepublishesTheNotification() =
        runTest {
            val harness = TestCoordinatorHarness()
            val snapshot = SessionDtoFixtures.snapshotInState(SessionStateDto.TRIGGERED_AWAITING_NFC)
            harness.gateway.commit(
                StoredDecision(
                    snapshot = snapshot,
                    receipt = EventReceipt("seed", snapshot.sessionId, "hash", 1, 900L),
                    effects = emptyList(),
                    androidExtras = SessionDtoFixtures.extras(),
                ),
            )
            harness.publisher.publish(snapshot)

            val watcher = watcherFor(harness, testScheduler)
            watcher.evaluate()

            assertThat(harness.scanRequestNotifier.presentCallCount).isEqualTo(1)
        }

    private fun watcherFor(
        harness: TestCoordinatorHarness,
        scheduler: TestCoroutineScheduler,
    ): SessionReadinessWatcher =
        SessionReadinessWatcher(
            harness.publisher,
            harness.readinessMonitor,
            harness.coordinator,
            StandardTestDispatcher(scheduler),
        )
}
