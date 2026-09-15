package com.niumi.app

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.database.directboot.DirectBootStore
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.alarm.RingingWatchdog
import com.niumi.system.alarm.RingingWatchdogReceiver
import com.niumi.system.alarm.RingingWatchdogSpecs
import com.niumi.system.common.Clock
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
     */
    @After
    fun tearDown() {
        alarmScheduler.cancel(SESSION_ID)
        ringingWatchdog.disarm(SESSION_ID)
        context.stopService(Intent(context, ringingServiceClass()))
        runBlocking { gateway.clearActive(SESSION_ID) }
        directBootStore.clear()
    }

    private companion object {
        const val SESSION_ID = "5b7c1e2a-6f3d-4a8b-9c0d-1e2f3a4b5c6d"
        const val BOX_ID = "5b7c1e2a-6f3d-4a8b-9c0d-1e2f3a4b5c6f"
        const val REVISION = 2L
        const val CHAIN_TIMEOUT_MS = 20_000L
        const val ABSENCE_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 200L
    }
}
