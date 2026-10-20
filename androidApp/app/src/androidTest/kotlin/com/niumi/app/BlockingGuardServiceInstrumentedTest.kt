package com.niumi.app

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.feature.session.blocking.guard.BlockingGuardService
import com.niumi.system.blocking.guard.BlockingGuardCommand
import com.niumi.system.blocking.guard.BlockingGuardController
import com.niumi.system.blocking.guard.BlockingGuardPhase
import com.niumi.system.common.OperationResult
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
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Service de garde du blocage (SPEC_ANDROID §12.5) : il atteint réellement le premier plan avec sa
 * notification, s'arrête sur demande, et s'arrête seul quand il est relancé sans commande alors
 * qu'aucune session n'existe.
 *
 * Placé dans `:app` : le service injecte la persistance, et seul ce module a un graphe Dagger
 * complet (même raison qu'`AlarmChainInstrumentedTest`). Nécessite un appareil ou un émulateur.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BlockingGuardServiceInstrumentedTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

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
        assertThat(title).startsWith("Blocage en cours jusqu'à ")
        assertThat(title).endsWith("7:00")

        assertThat(controller.release()).isEqualTo(OperationResult.Success)
        assertThat(awaitGuard { it == null }).isTrue()
        assertThat(controller.release()).isEqualTo(OperationResult.AlreadySatisfied)
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

    private fun awaitNotificationTitle(): String? {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            notificationManager.activeNotifications
                .firstOrNull { it.id == BlockingGuardService.NOTIFICATION_ID }
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
