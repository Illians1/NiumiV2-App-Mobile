package com.niumi.app

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.BlockingScheduleDto
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.core.interop.isBlockingPending
import com.niumi.core.schedule.TriggerDelayPolicy
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.database.blocking.BlockedPackagesRead
import com.niumi.database.blocking.BlockedPackagesSource
import com.niumi.database.blocking.BlockedPackagesState
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.incident.SessionIncidentsReader
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.alarm.BlockingStartScheduler
import com.niumi.system.alarm.RingingWatchdog
import com.niumi.system.alarm.RingingWatchdogReceiver
import com.niumi.system.alarm.RingingWatchdogSpecs
import com.niumi.system.common.Clock
import com.niumi.system.notification.SessionWarningNotifier
import com.niumi.system.session.LoadResult
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import javax.inject.Inject

/**
 * Mort de processus pendant `RINGING` (SPEC_ANDROID §10.2, §20 ; étape 20). Le plan d'origine
 * demandait `ServiceTestRule` : écarté, même raison que `AlarmRingingServiceInstrumentedTest`
 * (`bindServiceAndWait()` échoue sur un service sans binder). On rejoue à la place ce que la
 * production fait réellement : une session `RINGING` en base, un publisher vidé — le processus
 * recréé n'a encore rien lu —, puis `reconcile(PROCESS_START)`.
 *
 * Session écrite **directement en base**, comme `AlarmChainInstrumentedTest` : toute
 * instrumentation débranche le service d'accessibilité (§19.2), et faire passer la session par
 * `ACTIVATION_REQUESTED` échouerait sur `APPLY_BLOCKING`.
 *
 * **Lot 6 (étape 25).** Deux cas de plus couvrent la reprise d'un blocage différé dont l'instant
 * de début est tombé pendant que le processus était mort (SPEC_ANDROID §12.4, §20). Ils exigent que
 * le contrôle d'accessibilité réponde vrai, sans quoi `reconcileArmed` sort avant de les atteindre :
 * voir [TestBlockingBindingsModule], qui explique pourquoi c'est une doublure et non le réglage
 * système.
 *
 * Nécessite un appareil ou un émulateur : voir « Validation sur appareil réel » dans `CLAUDE.md`.
 * Ce que ces tests ne prouvent pas : que le système *délivre* réellement l'alarme de secours en
 * Doze après une mort de processus — seul le protocole manuel le montre, et seulement en deçà du
 * quota Doze de neuf minutes (§4.2).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProcessDeathInstrumentedTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var coordinator: SessionCoordinator

    @Inject
    lateinit var gateway: SessionPersistenceGateway

    @Inject
    lateinit var snapshotPublisher: SessionSnapshotPublisher

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var ringingWatchdog: RingingWatchdog

    @Inject
    lateinit var blockingStartScheduler: BlockingStartScheduler

    @Inject
    lateinit var blockedPackagesSource: BlockedPackagesSource

    @Inject
    lateinit var incidentsReader: SessionIncidentsReader

    @Inject
    lateinit var technicalEventLog: TechnicalEventLog

    @Inject
    lateinit var warningNotifier: SessionWarningNotifier

    @Inject
    lateinit var directBootStore: DirectBootStore

    @Inject
    lateinit var clock: Clock

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        hiltRule.inject()
        assertThat(runBlocking { gateway.load() }).isEqualTo(LoadResult.Absent)
        // Le processus recréé n'a encore rien publié : c'est précisément ce que le réconciliateur
        // doit rattraper (`SessionReconciler.reconcile`, §9.2).
        snapshotPublisher.publish(null)
    }

    @Test
    fun aRingingSessionInRoomGetsItsSoundBackAtProcessStart() {
        seedSession(SessionStateDto.RINGING)

        runBlocking { coordinator.reconcile(ReconcileReason.PROCESS_START) }

        assertThat(awaitForegroundRingingService()).isNotNull()
    }

    /**
     * L'entrée en `RINGING` arme le watchdog avec un `PendingIntent` dont le code de requête
     * n'est jamais celui du réveil pour la même session (`RingingWatchdogSpecsTest` le prouve en
     * JVM) ; ici, c'est sa présence **réelle** sur l'appareil qui est vérifiée.
     */
    @Test
    fun enteringRingingArmsAWatchdogPendingIntent() {
        seedSession(SessionStateDto.RINGING)

        runBlocking { coordinator.reconcile(ReconcileReason.PROCESS_START) }
        awaitForegroundRingingService()

        assertThat(watchdogPendingIntentOrNull()).isNotNull()
    }

    /**
     * Une session qui a quitté `RINGING` ne doit plus réveiller le processus pour rien : la
     * réconciliation désarme le watchdog au même titre qu'elle applique toute autre politique.
     */
    @Test
    fun leavingRingingDisarmsTheWatchdog() {
        seedSession(SessionStateDto.RINGING)
        runBlocking { coordinator.reconcile(ReconcileReason.PROCESS_START) }
        awaitForegroundRingingService()
        assertThat(watchdogPendingIntentOrNull()).isNotNull()

        seedSession(SessionStateDto.AWAITING_NFC, revision = REVISION + 1)
        runBlocking { coordinator.reconcile(ReconcileReason.PROCESS_START) }

        assertThat(watchdogPendingIntentOrNull()).isNull()
    }

    /**
     * Preuve de bout en bout de la chaîne, sans dépendre du système pour délivrer l'alarme de
     * secours : le tic est simulé par un broadcast explicite, exactement l'intent que
     * `AndroidRingingWatchdog.arm` programme.
     */
    @Test
    fun aWatchdogBroadcastResumesTheSound() {
        seedSession(SessionStateDto.RINGING)
        runBlocking { coordinator.reconcile(ReconcileReason.PROCESS_START) }
        assertThat(awaitForegroundRingingService()).isNotNull()
        context.stopService(Intent(context, ringingServiceClass()))
        assertThat(awaitForegroundRingingServiceAbsence()).isTrue()

        context.sendBroadcast(Intent(context, RingingWatchdogReceiver::class.java))

        assertThat(awaitForegroundRingingService()).isNotNull()
    }

    /**
     * SPEC_ANDROID §12.4 (« Réconciliation ») et §20, ligne « redémarrage après l'heure de début et
     * avant le réveil » : un blocage différé dont l'instant est dépassé pendant que le processus
     * était mort s'applique à la première réconciliation, **avant** la politique de retard du
     * réveil. Retard court, donc aucun incident : c'est la moitié négative de la règle, celle qui
     * attrape une régression de seuil appliquée à une vraie horloge et à une vraie base.
     */
    @Test
    fun aDeferredBlockingWhoseStartIsElapsedIsAppliedAtProcessStart() {
        FakeAccessibilityServiceStatus.enabled = true
        seedDeferredArmedSession(SESSION_ID_ON_TIME, clock.nowEpochMillis() - SHORT_DELAY_MS)
        assertThat(readProjection()).isEqualTo(BlockedPackagesState.Inactive)

        runBlocking { coordinator.reconcile(ReconcileReason.PROCESS_START) }

        assertThat(awaitTechnicalEvent(TechnicalEventType.BLOCKING_STARTED, SESSION_ID_ON_TIME)).isTrue()
        val snapshot = (runBlocking { gateway.load() } as LoadResult.Present).snapshot
        assertThat(snapshot.state).isEqualTo(SessionStateDto.ARMED)
        assertThat(snapshot.isBlockingPending).isFalse()
        assertThat(snapshot.blockingAppliedAtEpochMillis).isNotNull()
        assertThat(readProjection()).isEqualTo(
            BlockedPackagesState.Active(SESSION_ID_ON_TIME, setOf(BlockedPackage(BLOCKED_PACKAGE, "Exemple"))),
        )
        assertThat(runBlocking { incidentsReader.incidents(SESSION_ID_ON_TIME) }.map { it.code })
            .doesNotContain(IncidentCodes.MISSED_BLOCKING_START_WINDOW)
        assertThat(runBlocking { gateway.pendingEffects(SESSION_ID_ON_TIME) }).isEmpty()
    }

    /**
     * Même chemin, au-delà de la fenêtre de grâce : « un blocage ne se manque pas, il s'applique en
     * retard » (SPEC_CORE_KMP §8.3). L'incident est `WARNING` et non `DEGRADED`, contrairement à
     * `MISSED_TRIGGER_WINDOW` — c'est la ligne qui vaut ce test.
     *
     * `health` n'est volontairement pas vérifiée ici : la surveillance de §13.1 évalue aussi le
     * volume d'alarme et le mode Ne pas déranger, qui dépendent de l'appareil de campagne. Que
     * `WARNING` ne dégrade pas la santé est une règle de domaine, prouvée en JVM par
     * `SessionReconcilerBlockingStartTest`.
     */
    @Test
    fun aDeferredBlockingMissedByMoreThanFifteenMinutesIsAppliedAndReported() {
        FakeAccessibilityServiceStatus.enabled = true
        val startsAt = clock.nowEpochMillis() - TriggerDelayPolicy.GRACE_WINDOW_MILLIS - SHORT_DELAY_MS
        seedDeferredArmedSession(SESSION_ID_MISSED, startsAt)

        runBlocking { coordinator.reconcile(ReconcileReason.PROCESS_START) }

        assertThat(
            awaitTechnicalEvent(TechnicalEventType.MISSED_BLOCKING_START_WINDOW, SESSION_ID_MISSED),
        ).isTrue()
        val snapshot = (runBlocking { gateway.load() } as LoadResult.Present).snapshot
        assertThat(snapshot.state).isEqualTo(SessionStateDto.ARMED)
        assertThat(snapshot.isBlockingPending).isFalse()
        assertThat(readProjection()).isEqualTo(
            BlockedPackagesState.Active(SESSION_ID_MISSED, setOf(BlockedPackage(BLOCKED_PACKAGE, "Exemple"))),
        )
        val incident =
            runBlocking { incidentsReader.incidents(SESSION_ID_MISSED) }
                .single { it.code == IncidentCodes.MISSED_BLOCKING_START_WINDOW }
        assertThat(incident.severity).isEqualTo(IncidentSeverityDto.WARNING)
    }

    private fun watchdogPendingIntentOrNull(): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            RingingWatchdogSpecs.pendingIntent(SESSION_ID).requestCode,
            Intent(context, RingingWatchdogReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )

    private fun ringingServiceClass(): Class<*> = Class.forName("com.niumi.feature.ringing.AlarmRingingService")

    /**
     * `eventId` tiré au hasard à chaque appel, jamais constant : les reçus sont le registre
     * d'idempotence (SPEC_CORE_KMP §12) et **survivent volontairement à `clearActive`**, donc à la
     * fin d'un test comme d'une campagne entière. Un identifiant figé faisait échouer toute session
     * semée après la première, sur `UNIQUE constraint failed: session_event_receipt.eventId` —
     * même défaut que celui rencontré à l'étape 19 sur `RoomDirectBootMergeTest`.
     */
    private fun seedSession(
        state: SessionStateDto,
        revision: Long = REVISION,
    ) {
        runBlocking {
            gateway.commit(
                StoredDecision(
                    snapshot = sessionSnapshot(state, revision),
                    receipt =
                        EventReceipt(
                            eventId = UUID.randomUUID().toString(),
                            sessionId = SESSION_ID,
                            payloadSha256Hex = "b".repeat(64),
                            appliedRevision = revision,
                            receivedAtEpochMillis = clock.nowEpochMillis(),
                        ),
                    effects = emptyList(),
                    androidExtras =
                        AndroidSessionExtras(
                            boxId = BOX_ID,
                            boxTokenSha256Hex = "a".repeat(64),
                            ringtoneKey = "niumi_alarm",
                            vibrationEnabled = false,
                            blockedPackages = listOf(BlockedPackage("com.example.app", "Exemple")),
                        ),
                ),
            )
        }
    }

    private fun sessionSnapshot(
        state: SessionStateDto,
        revision: Long,
    ) = SessionSnapshotDto(
        schemaVersion = 1,
        revision = revision,
        sessionId = SESSION_ID,
        wakeSchedule =
            WakeScheduleDto(
                localDateIso = "2026-09-13",
                localTimeIso = "07:00",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = clock.nowEpochMillis() - 1_000L,
            ),
        state = state,
        releaseTarget = null,
        health = SessionHealthDto.HEALTHY,
        createdAtEpochMillis = clock.nowEpochMillis(),
        armedAtEpochMillis = clock.nowEpochMillis(),
        ringingAtEpochMillis = if (state == SessionStateDto.RINGING) clock.nowEpochMillis() else null,
        alarmSoundStoppedAtEpochMillis = null,
        triggerElapsedAtEpochMillis = if (state == SessionStateDto.AWAITING_NFC) clock.nowEpochMillis() else null,
        nfcVerifiedAtEpochMillis = null,
        releasingAtEpochMillis = null,
        completedAtEpochMillis = null,
        cancelledAtEpochMillis = null,
        failureCode = null,
    )

    /**
     * Session différée **en attente** : `blockingAppliedAtEpochMillis` nul, instant de début
     * dépassé, heure de réveil encore à venir — sans quoi la même passe produirait aussi
     * `TRIGGER_ELAPSED` et le blocage serait appliqué par le repli du moteur (SPEC_CORE_KMP §5.1),
     * qui prouverait le filet et non la fonctionnalité (point de vigilance 13 du plan).
     *
     * `sessionId` reçu en paramètre, et distinct d'un cas à l'autre : ni les incidents ni les reçus
     * ne sont effacés par `clearActive`, et l'ordre d'exécution des méthodes de test est arbitraire.
     */
    private fun seedDeferredArmedSession(
        sessionId: String,
        startsAtEpochMillis: Long,
    ) {
        runBlocking {
            gateway.commit(
                StoredDecision(
                    snapshot = deferredArmedSnapshot(sessionId, startsAtEpochMillis),
                    receipt =
                        EventReceipt(
                            eventId = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            payloadSha256Hex = "b".repeat(64),
                            appliedRevision = REVISION,
                            receivedAtEpochMillis = clock.nowEpochMillis(),
                        ),
                    effects = emptyList(),
                    androidExtras =
                        AndroidSessionExtras(
                            boxId = BOX_ID,
                            boxTokenSha256Hex = "a".repeat(64),
                            ringtoneKey = "niumi_alarm",
                            vibrationEnabled = false,
                            blockedPackages = listOf(BlockedPackage(BLOCKED_PACKAGE, "Exemple")),
                        ),
                ),
            )
        }
    }

    private fun deferredArmedSnapshot(
        sessionId: String,
        startsAtEpochMillis: Long,
    ) = SessionSnapshotDto(
        schemaVersion = 2,
        revision = REVISION,
        sessionId = sessionId,
        wakeSchedule =
            WakeScheduleDto(
                localDateIso = "2026-09-17",
                localTimeIso = "07:00",
                zoneIdAtActivation = "Europe/Paris",
                triggerAtEpochMillis = clock.nowEpochMillis() + TRIGGER_DELAY_MS,
            ),
        blockingSchedule =
            BlockingScheduleDto(
                localDateIso = "2026-09-16",
                localTimeIso = "22:30",
                startsAtEpochMillis = startsAtEpochMillis,
            ),
        blockingAppliedAtEpochMillis = null,
        state = SessionStateDto.ARMED,
        releaseTarget = null,
        health = SessionHealthDto.HEALTHY,
        createdAtEpochMillis = clock.nowEpochMillis(),
        armedAtEpochMillis = clock.nowEpochMillis(),
        ringingAtEpochMillis = null,
        alarmSoundStoppedAtEpochMillis = null,
        triggerElapsedAtEpochMillis = null,
        nfcVerifiedAtEpochMillis = null,
        releasingAtEpochMillis = null,
        completedAtEpochMillis = null,
        cancelledAtEpochMillis = null,
        failureCode = null,
    )

    private fun readProjection(): BlockedPackagesState {
        val read = runBlocking { blockedPackagesSource.read() }
        assertThat(read).isInstanceOf(BlockedPackagesRead.Resolved::class.java)
        return (read as BlockedPackagesRead.Resolved).state
    }

    /**
     * Seul le journal technique demande une attente : le coordinateur exécute ses effets **dans**
     * le mutex, donc snapshot, projection et incidents sont lisibles dès que `reconcile` rend la
     * main, alors que `TechnicalEventLog.log()` poste son écriture sur un scope. Filtré par
     * `sessionId` : le journal est partagé par tout le processus d'instrumentation, et les deux cas
     * différés écrivent le même type d'événement.
     */
    private fun awaitTechnicalEvent(
        type: TechnicalEventType,
        sessionId: String,
    ): Boolean {
        val deadline = System.currentTimeMillis() + CHAIN_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val found =
                runBlocking { technicalEventLog.recent() }
                    .any { it.type == type && it.sessionId == sessionId }
            if (found) return true
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    private fun awaitForegroundRingingService(): ActivityManager.RunningServiceInfo? {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val deadline = System.currentTimeMillis() + CHAIN_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val service =
                activityManager
                    .getRunningServices(Int.MAX_VALUE)
                    .firstOrNull { it.service.className.endsWith("AlarmRingingService") }
            if (service != null && service.foreground) return service
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return null
    }

    /**
     * `stopService()` retire le service du premier plan quasi immédiatement : un court sondage
     * suffit, pas besoin du même délai que pour attendre son démarrage.
     */
    private fun awaitForegroundRingingServiceAbsence(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val deadline = System.currentTimeMillis() + ABSENCE_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val stillForeground =
                activityManager
                    .getRunningServices(Int.MAX_VALUE)
                    .any { it.service.className.endsWith("AlarmRingingService") && it.foreground }
            if (!stillForeground) return true
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    /**
     * Le watchdog est désarmé explicitement : une passe de réconciliation a pu l'armer pour de bon
     * sur l'appareil, et une alarme laissée en place tirerait toutes les 60 s bien après la
     * campagne — jusque dans le protocole manuel qui suit.
     *
     * Même raison pour `alarmScheduler.cancel` sur les sessions différées, et ce n'est pas
     * défensif : `reconcileArmed` enchaîne sur la politique de retard du réveil, dont l'instant
     * n'est pas atteint et dont aucune alarme n'existe — la passe **pose une vraie alarme exacte**
     * à l'horizon du réveil semé. Le drapeau d'accessibilité est rendu à sa valeur par défaut :
     * l'objet est partagé par tout le processus d'instrumentation.
     */
    @After
    fun tearDown() {
        FakeAccessibilityServiceStatus.enabled = false
        warningNotifier.clearAll()
        context.stopService(Intent(context, ringingServiceClass()))
        SESSION_IDS.forEach { sessionId ->
            alarmScheduler.cancel(sessionId)
            blockingStartScheduler.cancel(sessionId)
            ringingWatchdog.disarm(sessionId)
            runBlocking { gateway.clearActive(sessionId) }
        }
        directBootStore.clear()
    }

    private companion object {
        const val SESSION_ID = "5b7c1e2a-6f3d-4a8b-9c0d-1e2f3a4b5c6d"
        const val SESSION_ID_ON_TIME = "5b7c1e2a-6f3d-4a8b-9c0d-1e2f3a4b5c7d"
        const val SESSION_ID_MISSED = "5b7c1e2a-6f3d-4a8b-9c0d-1e2f3a4b5c8d"
        const val BOX_ID = "5b7c1e2a-6f3d-4a8b-9c0d-1e2f3a4b5c6f"
        const val BLOCKED_PACKAGE = "com.example.app"
        const val REVISION = 2L
        const val TRIGGER_DELAY_MS = 600_000L
        const val SHORT_DELAY_MS = 60_000L
        const val CHAIN_TIMEOUT_MS = 20_000L
        const val ABSENCE_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 200L

        val SESSION_IDS = listOf(SESSION_ID, SESSION_ID_ON_TIME, SESSION_ID_MISSED)
    }
}
