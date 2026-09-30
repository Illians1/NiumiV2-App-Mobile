package com.niumi.system.readiness

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.session.LoadResult
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
     * déclencher que la surveillance de §13.1 et le réconciliateur d'exécution — jamais une
     * réconciliation complète, qui tournerait alors à chaque ouverture de l'application.
     *
     * Preuve : un réveil dépassé de 20 minutes n'est **pas** traité. Une réconciliation complète
     * dispatcherait `TRIGGER_ELAPSED` ([reconcileTriggerDelay]) ; ici, aucune décision n'est prise.
     * Jusqu'à l'étape 25, la preuve était l'alarme absente non reprogrammée : le réconciliateur
     * d'exécution, rejoué depuis au premier plan pour le NFC, répare justement cette alarme — ce
     * que l'ouverture de l'application gagne à faire.
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
            harness.clock.now = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS + TWENTY_MINUTES

            val watcher = watcherFor(harness, testScheduler)
            watcher.evaluate()

            assertThat(harness.recordingReducer.callCount).isEqualTo(0)
            assertThat(harness.gateway.load().let { (it as LoadResult.Present).snapshot.state })
                .isEqualTo(SessionStateDto.ARMED)
        }

    /** L'alarme d'une session `ARMED` disparue est réparée dès l'ouverture de l'application (§18). */
    @Test
    fun aForegroundPassRepairsAnArmedAlarmThatVanished() =
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

            assertThat(harness.alarmScheduler.isScheduled(snapshot.sessionId)).isTrue()
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.ALARM_RESCHEDULED)
            assertThat(harness.gateway.incidentsRecorded).isEmpty()
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

    /**
     * **Défaut mesuré sur appareil le 2026-09-27 (étape 25), corrigé ici.** NFC coupé pendant une
     * session `ARMED`, Niumi ouvert : aucun incident, aucun encadré. Le NFC est jugé par
     * `SessionRuntimeReconciler`, qui ne tournait qu'en fin de réconciliation complète — jamais au
     * premier plan hors états de scan. §13.1 range pourtant le NFC parmi les contrôles surveillés,
     * avec le premier plan comme déclencheur : un NFC coupé le soir n'était signalé qu'au réveil.
     */
    @Test
    fun aForegroundPassDetectsAnNfcTurnedOffWhileArmed() =
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
            harness.alarmScheduler.schedule(
                snapshot.sessionId,
                snapshot.revision,
                SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS,
            )
            harness.runtimeStatusProbe.nfcReady = false

            val watcher = watcherFor(harness, testScheduler)
            watcher.evaluate()

            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .containsExactly(IncidentCodes.NFC_DISABLED)
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.NFC_DISABLED)
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
            harness.runtimeReconciler,
            harness.coordinator,
            StandardTestDispatcher(scheduler),
        )

    private companion object {
        const val TWENTY_MINUTES = 20 * 60 * 1_000L
    }
}
