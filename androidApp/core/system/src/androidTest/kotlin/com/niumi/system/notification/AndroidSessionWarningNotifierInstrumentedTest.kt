package com.niumi.system.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.niumi.system.common.OperationResult
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.intent.NiumiComponentResolver
import com.niumi.system.readiness.MonitoredReadinessChecks
import com.niumi.system.readiness.ReadinessCheckId
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * SPEC_ANDROID §13.1 : la vraie `Notification` d'avertissement n'est ni `ongoing`, ni porteuse
 * d'un plein écran, ni d'une action, et deux contrôles cassés produisent deux notifications
 * distinctes. Complément instrumenté de `SessionWarningNotificationSpecsTest`, qui ne couvre que
 * le spec pur. Nécessite un appareil ou un émulateur (`connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class AndroidSessionWarningNotifierInstrumentedTest {
    @get:Rule
    val notificationPermission: TestRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            TestRule { base, _ -> base }
        }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val notifier =
        AndroidSessionWarningNotifier(
            context = context,
            pendingIntentFactory =
                AndroidPendingIntentFactory(
                    context,
                    NiumiComponentResolver {
                        ComponentName(context, AndroidSessionWarningNotifierInstrumentedTest::class.java)
                    },
                ),
            iconResolver = { android.R.drawable.ic_dialog_alert },
        )

    @Before
    fun setUp() {
        AndroidNotificationChannelRegistrar(context).registerAll()
        notifier.clearAll()
    }

    @After
    fun tearDown() {
        notifier.clearAll()
    }

    @Test
    fun theWarningChannelIsActuallyCreatedOnTheDevice() {
        val channel = notificationManager.getNotificationChannel(NiumiNotificationChannels.sessionWarning.id)

        assertThat(channel).isNotNull()
        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)
        assertThat(channel.sound).isNull()
        assertThat(channel.shouldVibrate()).isFalse()
    }

    @Test
    fun aWarningIsDismissibleAndCarriesNeitherFullScreenIntentNorAction() {
        notifier.present(ReadinessCheckId.ALARM_VOLUME)

        val posted = warningNotifications().single()
        assertThat(posted.actions).isNull()
        assertThat(posted.fullScreenIntent).isNull()
        assertThat(posted.flags and Notification.FLAG_ONGOING_EVENT).isEqualTo(0)
        assertThat(posted.category).isEqualTo(Notification.CATEGORY_ERROR)
    }

    @Test
    fun twoBrokenControlsProduceTwoDistinctNotifications() {
        notifier.present(ReadinessCheckId.ALARM_VOLUME)
        notifier.present(ReadinessCheckId.NOTIFICATIONS)

        assertThat(warningNotifications()).hasSize(2)
    }

    @Test
    fun clearRemovesOnlyItsOwnWarning() {
        notifier.present(ReadinessCheckId.ALARM_VOLUME)
        notifier.present(ReadinessCheckId.NOTIFICATIONS)

        notifier.clear(ReadinessCheckId.ALARM_VOLUME)

        assertThat(warningNotifications()).hasSize(1)
        assertThat(activeWarningIds())
            .containsExactly(SessionWarningNotificationSpecs.notificationId(ReadinessCheckId.NOTIFICATIONS))
    }

    @Test
    fun clearAllWithdrawsEveryWarning() {
        MonitoredReadinessChecks.incidentCodes.keys.forEach { notifier.present(it) }

        notifier.clearAll()

        assertThat(warningNotifications()).isEmpty()
    }

    @Test
    fun clearingAWarningThatWasNeverShownIsAlreadySatisfied() {
        assertThat(notifier.clear(ReadinessCheckId.EXACT_ALARM)).isEqualTo(OperationResult.AlreadySatisfied)
    }

    private fun warningNotifications(): List<Notification> =
        notificationManager.activeNotifications
            .filter { it.notification.channelId == NiumiNotificationChannels.sessionWarning.id }
            .map { it.notification }

    private fun activeWarningIds(): List<Int> =
        notificationManager.activeNotifications
            .filter { it.notification.channelId == NiumiNotificationChannels.sessionWarning.id }
            .map { it.id }
}
