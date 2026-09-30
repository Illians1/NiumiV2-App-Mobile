package com.niumi.app

import android.app.ActivityManager
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
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.common.Clock
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionPersistenceGateway
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Chaîne complète du réveil (SPEC_ANDROID §10.1, §10.2) : une alarme exacte réelle déclenche
 * `AlarmReceiver`, qui dispatche `ALARM_FIRED`, ce qui écrit `RINGING` et exécute `START_RINGING`.
 *
 * Placé dans `:app` et non `:feature:ringing` : injecter `AlarmTriggerHandler` tire le
 * coordinateur, donc les exécuteurs de blocage et la surveillance de §13.1, dont les liaisons
 * vivent dans `:feature:session`. `:app` est le seul module dont le graphe Dagger est complet, donc
 * le seul où cette chaîne s'exerce sans aucune doublure.
 *
 * La session est écrite **directement en base** plutôt qu'activée par `ACTIVATION_REQUESTED` :
 * §19.2 rappelle que toute instrumentation débranche le service d'accessibilité, `APPLY_BLOCKING`
 * échouerait et la phase produirait `ACTIVATION_FAILED` — l'état `ARMED` ne serait jamais atteint.
 *
 * Nécessite un appareil ou un émulateur : voir « Validation sur appareil réel » dans `CLAUDE.md`.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlarmChainInstrumentedTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var gateway: SessionPersistenceGateway

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var technicalEventLog: TechnicalEventLog

    @Inject
    lateinit var directBootStore: DirectBootStore

    @Inject
    lateinit var clock: Clock

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        hiltRule.inject()
        assertThat(runBlocking { gateway.load() }).isEqualTo(LoadResult.Absent)
    }

    @Test
    fun aRealExactAlarmDrivesTheSessionToRinging() {
        val triggerAt = clock.nowEpochMillis() + TRIGGER_DELAY_MS
        armSessionInDatabase(triggerAt)
        assertThat(alarmScheduler.schedule(SESSION_ID, REVISION, triggerAt).javaClass.simpleName)
            .isNotEqualTo("Failure")

        assertThat(awaitState(SessionStateDto.RINGING)).isTrue()
        assertThat(awaitForegroundRingingService()).isNotNull()

        // `RINGING_STARTED` est écrit par le service une fois le son réellement démarré, donc après
        // son passage au premier plan : il faut l'attendre. Un seul pour une sonnerie — l'exécuteur
        // `START_RINGING` l'écrivait aussi jusqu'au 2026-09-29 (SPEC_ANDROID §17). Seule la session du
        // test compte : une installation par-dessus conserve le journal des sessions précédentes
        // (mesuré le 29/09 : 5 débuts de sonnerie hérités, lus avant celui du test).
        val journal = awaitJournalContaining(TechnicalEventType.RINGING_STARTED)
        assertThat(journal).contains(TechnicalEventType.ALARM_RECEIVED)
        assertThat(journal.count { it == TechnicalEventType.RINGING_STARTED }).isEqualTo(1)
    }

    /** `effects` vide : la session est déjà armée, il n'y a aucun effet en attente à rejouer. */
    private fun armSessionInDatabase(triggerAt: Long) {
        runBlocking {
            gateway.commit(
                StoredDecision(
                    snapshot = armedSnapshot(triggerAt),
                    receipt =
                        EventReceipt(
                            eventId = EVENT_ID,
                            sessionId = SESSION_ID,
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
                            blockedPackages = listOf(BlockedPackage("com.example.app", "Exemple")),
                        ),
                ),
            )
        }
    }

    private fun armedSnapshot(triggerAt: Long) =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = REVISION,
            sessionId = SESSION_ID,
            wakeSchedule =
                WakeScheduleDto(
                    localDateIso = "2026-09-13",
                    localTimeIso = "07:00",
                    zoneIdAtActivation = "Europe/Paris",
                    triggerAtEpochMillis = triggerAt,
                ),
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

    private fun awaitState(expected: SessionStateDto): Boolean {
        val deadline = System.currentTimeMillis() + CHAIN_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val loaded = runBlocking { gateway.load() }
            if (loaded is LoadResult.Present && loaded.snapshot.state == expected) return true
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    private fun awaitJournalContaining(expected: TechnicalEventType): List<TechnicalEventType> {
        val deadline = System.currentTimeMillis() + CHAIN_TIMEOUT_MS
        var journal = emptyList<TechnicalEventType>()
        while (System.currentTimeMillis() < deadline) {
            journal =
                runBlocking { technicalEventLog.recent() }
                    .filter { it.sessionId == SESSION_ID }
                    .map { it.type }
            if (expected in journal) return journal
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return journal
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
     * Le snapshot Direct Boot est effacé explicitement : laissé en place, il porterait encore une
     * session avec son `triggerAtEpochMillis` et un redémarrage du téléphone reprogrammerait une
     * alarme (leçon de `ETAPE-16.md`).
     */
    @After
    fun tearDown() {
        alarmScheduler.cancel(SESSION_ID)
        context.stopService(Intent().setClassName(context, RINGING_SERVICE))
        runBlocking { gateway.clearActive(SESSION_ID) }
        directBootStore.clear()
    }

    private companion object {
        const val SESSION_ID = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"
        const val EVENT_ID = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6c"
        const val BOX_ID = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6d"
        const val REVISION = 2L
        const val TRIGGER_DELAY_MS = 5_000L
        const val CHAIN_TIMEOUT_MS = 20_000L
        const val POLL_INTERVAL_MS = 200L
        const val RINGING_SERVICE = "com.niumi.feature.ringing.AlarmRingingService"
    }
}
