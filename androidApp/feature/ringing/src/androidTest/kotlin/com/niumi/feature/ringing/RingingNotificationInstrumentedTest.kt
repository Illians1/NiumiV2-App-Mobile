package com.niumi.feature.ringing

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * La notification **réellement postée par `AlarmRingingService`** pendant la sonnerie
 * (SPEC_ANDROID §10.2, §10.3, §19.1) : aucune action d'arrêt, `ongoing`, `CATEGORY_ALARM`, et un
 * `fullScreenIntent` — c'est par lui que l'écran de réveil revient, donc que le scan reste
 * atteignable (§11.2). `RingingNotificationSpecsTest` (JVM) et
 * `RingingNotificationFactoryInstrumentedTest` couvrent le spec et la fabrique ; ici, c'est
 * l'objet posté par le service en conditions réelles.
 *
 * **Ce que ce test ne couvre pas, et pourquoi aucun test ne le peut.** La republication après
 * disparition (§10.2) ne peut pas être déclenchée par instrumentation : `NotificationManager.cancel()`
 * est **sans effet** sur la notification d'un service au premier plan — mesuré sur appareil à
 * l'étape 17, la notification restait postée après 20 s d'attente. Seul l'utilisateur peut la
 * rejeter, par balayage, depuis Android 14. La décision de republication est donc couverte en JVM
 * (`RingingNotificationWatchTest`) et son câblage par le protocole manuel, pas ici.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RingingNotificationInstrumentedTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    /**
     * Sans `POST_NOTIFICATIONS`, `activeNotifications` reste vide et l'échec ressemblerait à un
     * défaut du service alors qu'il ne s'agit que de la condition d'exécution. Même motif que
     * `AndroidScanRequestNotifierInstrumentedTest` (`:core:system`).
     */
    @get:Rule(order = 1)
    val notificationPermission: TestRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            TestRule { base, _ -> base }
        }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun theRingingNotificationOffersNoStopActionAndCarriesTheFullScreenIntent() {
        context.startForegroundService(
            Intent(context, AlarmRingingService::class.java)
                .putExtra(AlarmReceiver.EXTRA_SESSION_ID, SESSION_ID)
                .putExtra(AlarmReceiver.EXTRA_REVISION, 1L),
        )

        val notification = awaitRingingNotification()

        // §10.2, §19.1 : aucune action d'arrêt dans l'intent, la notification ou le binding.
        assertThat(notification.actions).isNull()
        assertThat(notification.category).isEqualTo(Notification.CATEGORY_ALARM)
        assertThat(notification.flags and Notification.FLAG_ONGOING_EVENT).isNotEqualTo(0)
        // §10.3 : le plein écran est le seul mécanisme qui ramène l'écran de réveil.
        assertThat(notification.fullScreenIntent).isNotNull()
        assertThat(notification.contentIntent).isNotNull()
    }

    private fun awaitRingingNotification(): Notification {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val posted =
                notificationManager.activeNotifications.firstOrNull { it.id == NOTIFICATION_ID }
            if (posted != null) return posted.notification
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError("La notification de sonnerie n'a pas été postée en $TIMEOUT_MS ms.")
    }

    @After
    fun tearDown() {
        context.stopService(Intent(context, AlarmRingingService::class.java))
    }

    private companion object {
        const val SESSION_ID = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"
        const val NOTIFICATION_ID = 1
        const val TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 100L
    }
}
