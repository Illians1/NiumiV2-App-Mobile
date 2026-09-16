package com.niumi.app

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.BlockingScheduleDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.core.interop.isBlockingPending
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.database.blocking.BlockedPackagesRead
import com.niumi.database.blocking.BlockedPackagesSource
import com.niumi.database.blocking.BlockedPackagesState
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.alarm.BlockingStartScheduler
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
 * SPEC_ANDROID §19.2 (Lot 6) : « réception de l'intent explicite de début du blocage par
 * `BlockingStartReceiver` ». Prouve la chaîne réelle `Intent` → receveur → `BlockingStartHandler` →
 * coordinateur → `APPLY_BLOCKING` sur la projection, sur le graphe Hilt complet de `:app`.
 *
 * L'intent est envoyé **directement** plutôt qu'en attendant l'alarme : la programmation elle-même
 * est prouvée en JVM (`BlockingStartPendingIntentSpecsTest`, `BlockingStartExecutorsTest`), et ce qui
 * ne l'est pas, c'est que le receveur déclaré au manifeste soit réellement atteint et que ses extras
 * survivent au passage par `Intent`.
 *
 * La session est écrite **directement en base**, comme `AlarmChainInstrumentedTest` et pour la même
 * raison (§19.2) : toute instrumentation débranche le service d'accessibilité, `APPLY_BLOCKING`
 * échouerait à l'activation et l'état `ARMED` ne serait jamais atteint. Ici, `APPLY_BLOCKING`
 * s'exécute sur la **projection**, que le service lit — la projection est donc observable même quand
 * le service ne tourne pas.
 *
 * Nécessite un appareil ou un émulateur : voir « Validation sur appareil réel » dans `CLAUDE.md`.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BlockingStartReceiverInstrumentedTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var gateway: SessionPersistenceGateway

    @Inject
    lateinit var blockingStartScheduler: BlockingStartScheduler

    @Inject
    lateinit var blockedPackagesSource: BlockedPackagesSource

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
    fun anExplicitBlockingStartIntentAppliesTheBlockingOfADeferredSession() {
        val startsAt = clock.nowEpochMillis() - 1_000L
        armDeferredSessionInDatabase(startsAt)
        assertThat(readProjection()).isEqualTo(BlockedPackagesState.Inactive)

        context.sendBroadcast(
            Intent()
                .setClassName(context, BLOCKING_START_RECEIVER)
                .putExtra("sessionId", SESSION_ID)
                .putExtra("revision", REVISION),
        )

        // L'attente porte sur le **dernier maillon**, l'exécution de l'effet, et non sur le snapshot :
        // le coordinateur persiste la décision **avant** d'exécuter le moindre effet
        // (SPEC_CORE_KMP §6). Attendre `isBlockingPending == false` sortait donc dès le commit, et la
        // lecture du journal courait contre `APPLY_BLOCKING` — course perdue dès que l'appareil est
        // un peu chargé, gagnée quand ce test tourne seul. Défaut trouvé sur appareil le 2026-09-16.
        assertThat(awaitTechnicalEvent(TechnicalEventType.BLOCKING_STARTED)).isTrue()

        val snapshot = (runBlocking { gateway.load() } as LoadResult.Present).snapshot
        assertThat(snapshot.state).isEqualTo(SessionStateDto.ARMED)
        assertThat(snapshot.isBlockingPending).isFalse()
        assertThat(snapshot.blockingAppliedAtEpochMillis).isNotNull()
        assertThat(readProjection()).isEqualTo(
            BlockedPackagesState.Active(SESSION_ID, setOf(BlockedPackage(BLOCKED_PACKAGE, "Exemple"))),
        )
        val journal = runBlocking { technicalEventLog.recent() }.map { it.type }
        assertThat(journal).containsAtLeast(
            TechnicalEventType.BLOCKING_START_RECEIVED,
            TechnicalEventType.BLOCKING_STARTED,
            TechnicalEventType.BLOCK_APPLIED,
        )
        // Tous les effets de la décision ont abouti : rien ne reste à rejouer.
        assertThat(runBlocking { gateway.pendingEffects(SESSION_ID) }).isEmpty()
    }

    private fun armDeferredSessionInDatabase(startsAtEpochMillis: Long) {
        runBlocking {
            gateway.commit(
                StoredDecision(
                    snapshot = deferredArmedSnapshot(startsAtEpochMillis),
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
                            blockedPackages = listOf(BlockedPackage(BLOCKED_PACKAGE, "Exemple")),
                        ),
                ),
            )
        }
    }

    /** `blockingAppliedAtEpochMillis` nul : le blocage est demandé, jamais encore appliqué. */
    private fun deferredArmedSnapshot(startsAtEpochMillis: Long) =
        SessionSnapshotDto(
            schemaVersion = 2,
            revision = REVISION,
            sessionId = SESSION_ID,
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
     * Le broadcast est asynchrone et le receveur ouvre `goAsync()` : l'événement attendu est celui de
     * l'**exécution** de l'effet, dernier maillon de la chaîne. Le journal est partagé par tous les
     * tests du processus, d'où la recherche d'un type plutôt qu'une comparaison de liste.
     */
    private fun awaitTechnicalEvent(type: TechnicalEventType): Boolean {
        val deadline = System.currentTimeMillis() + CHAIN_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (runBlocking { technicalEventLog.recent() }.any { it.type == type }) return true
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    @After
    fun tearDown() {
        blockingStartScheduler.cancel(SESSION_ID)
        runBlocking { gateway.clearActive(SESSION_ID) }
        directBootStore.clear()
    }

    private companion object {
        const val SESSION_ID = "4f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"
        const val EVENT_ID = "4f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6c"
        const val BOX_ID = "4f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6d"
        const val BLOCKED_PACKAGE = "com.example.app"
        const val REVISION = 2L
        const val TRIGGER_DELAY_MS = 600_000L
        const val CHAIN_TIMEOUT_MS = 20_000L
        const val POLL_INTERVAL_MS = 200L
        const val BLOCKING_START_RECEIVER = "com.niumi.system.blocking.BlockingStartReceiver"
    }
}
