package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Réconciliation déclenchée par un événement système (SPEC_ANDROID §9.3 ; SPEC_CORE_KMP §8.2).
 * Couvre les trois bornes de la fenêtre de grâce de 15 minutes et le traitement d'un déplacement de
 * l'horloge, pour les raisons que `SystemEventsReceiver` produit désormais.
 *
 * La politique de retard elle-même vit dans `:shared:core` (`TriggerDelayPolicy`) et y est testée
 * aux bornes ; ici on prouve ce qu'Android **fait** de chacun de ses trois verdicts.
 */
class SessionReconcilerBootTest {
    @Test
    fun lockedBootRescheduleAnArmedSessionAtTheSameInstant() =
        runTest {
            val harness = armedSession(nowEpochMillis = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS - TEN_MINUTES)
            // Android efface les alarmes `AlarmManager` au redémarrage : la session est armée, mais
            // plus aucun `PendingIntent` n'existe.
            harness.alarmScheduler.cancel(SessionDtoFixtures.SESSION_ID)

            val result = harness.coordinator.reconcile(ReconcileReason.LOCKED_BOOT)

            assertThat(result.actions)
                .contains(ReconcileAction.AlarmRescheduled(SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS))
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.ALARM_RESCHEDULED)
        }

    /**
     * **Défaut mesuré sur appareil le 2026-09-14 (essai 1), corrigé ici.** Avant le premier
     * déverrouillage, Android refuse de lier le service d'accessibilité — il n'est pas
     * `directBootAware` — et remet `accessibility_enabled` à 0. Le contrôle `ACCESSIBILITY_SERVICE`
     * tombait en échec, la garde de [SessionReconciler.reconcileArmed] interrompait la passe, et
     * **l'alarme n'était jamais reprogrammée** : le réveil était perdu par le redémarrage même que
     * §9.3 doit rattraper. Le contrôle est désormais `NOT_APPLICABLE` tant que l'appareil est
     * verrouillé.
     */
    @Test
    fun lockedBootRescheduleEvenWhileTheAccessibilityServiceCannotRun() =
        runTest {
            val harness = armedSession(nowEpochMillis = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS - TEN_MINUTES)
            harness.alarmScheduler.cancel(SessionDtoFixtures.SESSION_ID)
            // Ce que voit le diagnostic en Direct Boot : service non lié, drapeau global à 0.
            harness.readinessSources.unlockState.isUserUnlocked = false
            harness.accessibilityServiceStatus.enabled = false

            val result = harness.coordinator.reconcile(ReconcileReason.LOCKED_BOOT)

            assertThat(result.actions)
                .contains(ReconcileAction.AlarmRescheduled(SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS))
            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .doesNotContain(IncidentCodes.BLOCKING_PERMISSION_REVOKED)
            assertThat(harness.warningNotifier.presented).isEmpty()
            assertThat(harness.gateway.load().healthOrNull()).isEqualTo(SessionHealthDto.HEALTHY)
        }

    /**
     * §9.3 : « jusqu'à 15 minutes, il reprogramme une alarme immédiate dont `AlarmReceiver`
     * produira `ALARM_FIRED` ». Le réconciliateur ne démarre donc jamais la sonnerie lui-même :
     * l'alarme repasse par la chaîne normale.
     */
    @Test
    fun aDelayInsideTheGraceWindowRescheduleAnImmediateAlarmWithoutRinging() =
        runTest {
            val now = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS + TEN_MINUTES
            val harness = armedSession(nowEpochMillis = now)

            val result = harness.coordinator.reconcile(ReconcileReason.BOOT)

            assertThat(result.actions).contains(ReconcileAction.AlarmRescheduled(now))
            assertThat(harness.journal.calls).doesNotContain("RingingController.startRinging")
            assertThat(harness.gateway.load().stateOrNull()).isEqualTo(SessionStateDto.ARMED)
        }

    /**
     * §9.3 et §20 : « au-delà, il applique `TRIGGER_ELAPSED` avec `MISSED_TRIGGER_WINDOW` […]
     * exécute `PRESENT_SCAN_REQUEST` […] sans démarrer le service de sonnerie ».
     */
    @Test
    fun aDelayBeyondTheGraceWindowTriggersElapsedDegradedAndAsksForAScan() =
        runTest {
            val harness = armedSession(nowEpochMillis = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS + TWENTY_MINUTES)

            harness.coordinator.reconcile(ReconcileReason.LOCKED_BOOT)

            val loaded = harness.gateway.load() as LoadResult.Present
            assertThat(loaded.snapshot.state).isEqualTo(SessionStateDto.TRIGGERED_AWAITING_NFC)
            assertThat(loaded.snapshot.health).isEqualTo(SessionHealthDto.DEGRADED)
            assertThat(harness.scanRequestNotifier.presentCallCount).isAtLeast(1)
            assertThat(harness.journal.calls).doesNotContain("RingingController.startRinging")
            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.MISSED_TRIGGER_WINDOW)
            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .contains(IncidentCodes.MISSED_TRIGGER_WINDOW)
        }

    /**
     * §8.2 : « un changement manuel de l'heure ou du fuseau demande aux adaptateurs natifs de
     * préserver le même instant ». Jamais un recalcul depuis l'heure locale d'origine.
     */
    @Test
    fun aClockChangeReRegistersTheAlarmAtTheSameInstant() =
        runTest {
            val harness = armedSession(nowEpochMillis = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS - TEN_MINUTES)

            val result = harness.coordinator.reconcile(ReconcileReason.TIME_CHANGED)

            assertThat(result.actions)
                .contains(ReconcileAction.AlarmRescheduled(SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS))
        }

    /**
     * La reprogrammation est inconditionnelle sur un déplacement d'horloge, alors qu'elle est
     * conditionnée à l'absence de `PendingIntent` sur les autres raisons : `isScheduled` ne prouve
     * que l'existence de l'intent, jamais que le système l'a conservé au bon instant.
     */
    @Test
    fun aClockChangeRescheduleEvenWhenAnAlarmIsStillRegistered() =
        runTest {
            val harness = armedSession(nowEpochMillis = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS - TEN_MINUTES)
            assertThat(harness.alarmScheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isTrue()

            val result = harness.coordinator.reconcile(ReconcileReason.TIMEZONE_CHANGED)

            assertThat(result.actions)
                .contains(ReconcileAction.AlarmRescheduled(SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS))
        }

    @Test
    fun aClockChangeRecordsAWarningIncidentWithoutDegradingHealth() =
        runTest {
            val harness = armedSession(nowEpochMillis = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS - TEN_MINUTES)

            val result = harness.coordinator.reconcile(ReconcileReason.TIME_CHANGED)

            assertThat(result.actions)
                .contains(ReconcileAction.IncidentDispatched(IncidentCodes.TIME_CHANGED, IncidentSeverityDto.WARNING))
            assertThat(harness.gateway.incidentsRecorded.map { it.second.code }).contains(IncidentCodes.TIME_CHANGED)
            assertThat(harness.gateway.load().healthOrNull()).isEqualTo(SessionHealthDto.HEALTHY)
        }

    /**
     * `android.intent.action.TIME_SET` est aussi émis à chaque correction d'horloge par le réseau.
     * Un incident par code et par session, même convention qu'à l'étape 16 — sinon l'écran 7
     * accumulerait des dizaines de lignes identiques en une nuit.
     */
    @Test
    fun repeatedClockChangesRecordASingleIncidentButAlwaysRescheduleTheAlarm() =
        runTest {
            val harness = armedSession(nowEpochMillis = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS - TEN_MINUTES)

            harness.coordinator.reconcile(ReconcileReason.TIME_CHANGED)
            val second = harness.coordinator.reconcile(ReconcileReason.TIME_CHANGED)
            val third = harness.coordinator.reconcile(ReconcileReason.TIME_CHANGED)

            assertThat(harness.gateway.incidentsRecorded.count { it.second.code == IncidentCodes.TIME_CHANGED })
                .isEqualTo(1)
            assertThat(second.actions)
                .contains(ReconcileAction.AlarmRescheduled(SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS))
            assertThat(third.actions)
                .contains(ReconcileAction.AlarmRescheduled(SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS))
        }

    /** Les deux codes sont distincts : un fuseau changé après une heure changée reste consigné. */
    @Test
    fun timeAndTimezoneChangesAreDeduplicatedSeparately() =
        runTest {
            val harness = armedSession(nowEpochMillis = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS - TEN_MINUTES)

            harness.coordinator.reconcile(ReconcileReason.TIME_CHANGED)
            harness.coordinator.reconcile(ReconcileReason.TIMEZONE_CHANGED)

            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .containsAtLeast(IncidentCodes.TIME_CHANGED, IncidentCodes.TIMEZONE_CHANGED)
        }

    /**
     * §10.5, décision de l'étape 19. Mesuré sur appareil le 2026-09-14 : après un balayage de la
     * notification d'attente de scan, elle reste absente **523 s et au-delà** — ni le passage au
     * premier plan ni un déverrouillage d'écran ordinaire ne la ramenaient, et seul un redémarrage
     * de processus y parvenait. `SessionReadinessWatcher` déclenche désormais cette raison au
     * premier plan quand la session attend un scan ; le réconciliateur doit y republier.
     */
    @Test
    fun aForegroundPassWhileAwaitingAScanRepublishesTheNotification() =
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

            val result = harness.coordinator.reconcile(ReconcileReason.FOREGROUND)

            assertThat(result.actions).contains(ReconcileAction.ScanRequestRepublished)
            assertThat(harness.scanRequestNotifier.presentCallCount).isEqualTo(1)
        }

    /**
     * La raison de premier plan ne doit rien faire d'autre que republier : elle survient à chaque
     * ouverture de l'application, et reprogrammer une alarme ou consigner un incident à ce rythme
     * serait un effet de bord inacceptable.
     */
    @Test
    fun aForegroundPassNeitherReschedulesAnAlarmNorRecordsAnIncident() =
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

            harness.coordinator.reconcile(ReconcileReason.FOREGROUND)

            assertThat(harness.journal.calls).doesNotContain("AlarmScheduler.schedule")
            assertThat(harness.gateway.incidentsRecorded).isEmpty()
        }

    /** Aucun incident d'horloge sur une raison qui ne décrit pas un déplacement de l'horloge. */
    @Test
    fun anOrdinaryReconciliationRecordsNoClockIncident() =
        runTest {
            val harness = armedSession(nowEpochMillis = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS - TEN_MINUTES)

            harness.coordinator.reconcile(ReconcileReason.PACKAGE_REPLACED)

            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .containsNoneOf(IncidentCodes.TIME_CHANGED, IncidentCodes.TIMEZONE_CHANGED)
        }

    /** Session `ARMED` persistée, alarme enregistrée, horloge positionnée par l'appelant. */
    private suspend fun armedSession(nowEpochMillis: Long): TestCoordinatorHarness {
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
        harness.alarmScheduler.schedule(
            snapshot.sessionId,
            snapshot.revision,
            SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS,
        )
        harness.clock.now = nowEpochMillis
        return harness
    }

    private companion object {
        const val TEN_MINUTES = 10 * 60 * 1_000L
        const val TWENTY_MINUTES = 20 * 60 * 1_000L
    }
}

private fun LoadResult.stateOrNull(): SessionStateDto? = (this as? LoadResult.Present)?.snapshot?.state

private fun LoadResult.healthOrNull(): SessionHealthDto? = (this as? LoadResult.Present)?.snapshot?.health
