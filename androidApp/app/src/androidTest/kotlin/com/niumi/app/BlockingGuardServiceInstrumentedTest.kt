package com.niumi.app

import android.Manifest
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.niumi.feature.session.blocking.guard.BlockingGuardService
import com.niumi.system.blocking.guard.BlockingGuardCommand
import com.niumi.system.blocking.guard.BlockingGuardController
import com.niumi.system.blocking.guard.BlockingGuardPhase
import com.niumi.system.common.OperationResult
import com.niumi.system.notification.NiumiNotificationIds
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionPersistenceGateway
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Service de garde du blocage (SPEC_ANDROID §12.5) : il atteint réellement le premier plan avec sa
 * notification, reprend celle de l'alarme, de l'alarme au scan (2026-10-22), s'arrête sur demande,
 * et s'arrête seul quand il est relancé sans commande alors qu'aucune session n'existe.
 *
 * Placé dans `:app` : le service injecte la persistance, et seul ce module a un graphe Dagger
 * complet (même raison qu'`AlarmChainInstrumentedTest`). Nécessite un appareil ou un émulateur.
 *
 * La permission des notifications est accordée par le test lui-même (2026-10-22) : sans elle,
 * Android 13 et plus fait tourner le service sans afficher sa notification, et le titre lu valait
 * `null` dès que l'app venait d'être réinstallée par une suite précédente (échec relevé aux étapes
 * 34, 35 et 38, reproduit en retirant la permission).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BlockingGuardServiceInstrumentedTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val notificationPermission: TestRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            TestRule { base, _ -> base }
        }

    @Inject
    lateinit var controller: BlockingGuardController

    @Inject
    lateinit var gateway: SessionPersistenceGateway

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notificationManager by lazy { context.getSystemService(NotificationManager::class.java) }

    @Before
    fun setUp() {
        hiltRule.inject()
        assertThat(runBlocking { gateway.load() }).isEqualTo(LoadResult.Absent)
    }

    @Test
    fun theGuardReachesTheForegroundWithItsNotificationThenStops() {
        assertThat(controller.keep(COMMAND)).isEqualTo(OperationResult.Success)

        assertThat(awaitGuard { it?.foreground == true }).isTrue()
        val title = awaitNotificationTitle()
        assertThat(title).startsWith("Réveil prévu à ")
        assertThat(title).endsWith("7:00")

        assertThat(controller.release()).isEqualTo(OperationResult.Success)
        assertThat(awaitGuard { it == null }).isTrue()
        assertThat(controller.release()).isEqualTo(OperationResult.AlreadySatisfied)
    }

    /**
     * De l'alarme au scan, la garde reprend la notification de l'alarme sous son identifiant, et
     * la sienne disparaît (§12.5, décisions du 2026-10-22) : une seule notification reste.
     */
    @Test
    fun fromTheAlarmTheGuardTakesOverTheAlarmNotification() {
        assertThat(controller.keep(COMMAND)).isEqualTo(OperationResult.Success)
        assertThat(awaitGuard { it?.foreground == true }).isTrue()
        assertThat(awaitNotificationTitle()).startsWith("Réveil prévu à ")

        assertThat(keepIn(BlockingGuardPhase.RINGING)).isEqualTo(OperationResult.Success)
        assertThat(awaitNotificationTitle(NiumiNotificationIds.RINGING)).isEqualTo("Alarme Niumi en cours")
        assertThat(awaitAbsent(BlockingGuardService.NOTIFICATION_ID)).isTrue()

        assertThat(keepIn(BlockingGuardPhase.AWAITING_SCAN)).isEqualTo(OperationResult.Success)
        assertThat(awaitNotificationTitle(NiumiNotificationIds.SCAN_REQUEST)).isEqualTo("Ton réveil Niumi est passé")
        assertThat(awaitAbsent(NiumiNotificationIds.RINGING)).isTrue()

        // Après le scan, rien ne change jusqu'à l'arrêt : pas de texte furtif.
        assertThat(keepIn(BlockingGuardPhase.RELEASING)).isEqualTo(OperationResult.Success)
        Thread.sleep(SETTLE_MS)
        assertThat(niumiNotificationIds()).containsExactly(NiumiNotificationIds.SCAN_REQUEST)

        assertThat(controller.release()).isEqualTo(OperationResult.Success)
        assertThat(awaitGuard { it == null }).isTrue()
        assertThat(awaitAbsent(NiumiNotificationIds.SCAN_REQUEST)).isTrue()
    }

    /**
     * Relancé par le système sans commande, sans session en base : il passe au premier plan (sinon
     * `startForegroundService` lèverait), relit la persistance, et s'arrête seul (§12.5).
     */
    @Test
    fun aGuardStartedWithoutCommandStopsWhenNoSessionExists() {
        context.startForegroundService(Intent(context, BlockingGuardService::class.java))

        Thread.sleep(SETTLE_MS)
        assertThat(awaitGuard { it == null }).isTrue()
    }

    private fun awaitGuard(condition: (ActivityManager.RunningServiceInfo?) -> Boolean): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val service =
                activityManager
                    .getRunningServices(Int.MAX_VALUE)
                    .firstOrNull { it.service.className == BlockingGuardService::class.java.name }
            if (condition(service)) return true
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    private fun keepIn(phase: BlockingGuardPhase): OperationResult = controller.keep(COMMAND.copy(phase = phase))

    private fun niumiNotificationIds(): List<Int> =
        notificationManager.activeNotifications
            .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .map { it.id }

    private fun awaitAbsent(id: Int): Boolean {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (id !in niumiNotificationIds()) return true
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    private fun awaitNotificationTitle(id: Int = BlockingGuardService.NOTIFICATION_ID): String? {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            notificationManager.activeNotifications
                .firstOrNull { it.id == id }
                ?.let {
                    return it.notification.extras
                        .getCharSequence("android.title")
                        ?.toString()
                }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return null
    }

    @After
    fun tearDown() {
        context.stopService(Intent(context, BlockingGuardService::class.java))
    }

    private companion object {
        val COMMAND =
            BlockingGuardCommand(
                sessionId = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b",
                phase = BlockingGuardPhase.BLOCKING,
                wakeAtEpochMillis =
                    ZonedDateTime
                        .of(
                            2026,
                            10,
                            20,
                            7,
                            0,
                            0,
                            0,
                            ZoneId.of("Europe/Paris"),
                        ).toInstant()
                        .toEpochMilli(),
                blockingStartsAtEpochMillis = null,
                zoneId = "Europe/Paris",
            )
        const val TIMEOUT_MS = 10_000L
        const val SETTLE_MS = 1_000L
        const val POLL_INTERVAL_MS = 200L
    }
}
