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

    /**
     * L'attente n'est pas décorative : sans elle, un test hérite de l'annulation encore en vol du
     * précédent, et l'échec se déplace d'une exécution à l'autre (voir [awaitNotifications]).
     */
    @Before
    fun setUp() {
        AndroidNotificationChannelRegistrar(context).registerAll()
        clearAllAndWait()
    }

    @After
    fun tearDown() {
        clearAllAndWait()
    }

    private fun clearAllAndWait() {
        notifier.clearAll()
        awaitNotifications(
            description = "plus aucun avertissement actif",
            read = ::warningNotifications,
        ) { it.isEmpty() }
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

        val posted =
            awaitNotifications(
                description = "un avertissement publié",
                read = ::warningNotifications,
            ) { it.size == 1 }.single()
        assertThat(posted.actions).isNull()
        assertThat(posted.fullScreenIntent).isNull()
        assertThat(posted.flags and Notification.FLAG_ONGOING_EVENT).isEqualTo(0)
        assertThat(posted.category).isEqualTo(Notification.CATEGORY_ERROR)
    }

    @Test
    fun twoBrokenControlsProduceTwoDistinctNotifications() {
        notifier.present(ReadinessCheckId.ALARM_VOLUME)
        notifier.present(ReadinessCheckId.NOTIFICATIONS)

        val posted =
            awaitNotifications(
                description = "deux avertissements publiés",
                read = ::warningNotifications,
            ) { it.size == 2 }
        assertThat(posted).hasSize(2)
    }

    @Test
    fun clearRemovesOnlyItsOwnWarning() {
        notifier.present(ReadinessCheckId.ALARM_VOLUME)
        notifier.present(ReadinessCheckId.NOTIFICATIONS)
        awaitNotifications(
            description = "les deux avertissements publiés avant d'en retirer un",
            read = ::warningNotifications,
        ) { it.size == 2 }

        notifier.clear(ReadinessCheckId.ALARM_VOLUME)

        val remaining =
            awaitNotifications(
                description = "un seul avertissement restant",
                read = ::activeWarningIds,
            ) { it.size == 1 }
        assertThat(remaining)
            .containsExactly(SessionWarningNotificationSpecs.notificationId(ReadinessCheckId.NOTIFICATIONS))
    }

    @Test
    fun clearAllWithdrawsEveryWarning() {
        MonitoredReadinessChecks.incidentCodes.keys.forEach { notifier.present(it) }
        awaitNotifications(
            description = "tous les avertissements publiés avant de les retirer",
            read = ::warningNotifications,
        ) { it.size == MonitoredReadinessChecks.incidentCodes.size }

        notifier.clearAll()

        val remaining =
            awaitNotifications(
                description = "plus aucun avertissement actif",
                read = ::warningNotifications,
            ) { it.isEmpty() }
        assertThat(remaining).isEmpty()
    }

    @Test
    fun clearingAWarningThatWasNeverShownIsAlreadySatisfied() {
        assertThat(notifier.clear(ReadinessCheckId.EXACT_ALARM)).isEqualTo(OperationResult.AlreadySatisfied)
    }

    /**
     * Les résumés de groupe sont exclus : au-delà de trois notifications sans groupe explicite,
     * Android en fabrique un lui-même (`id = 0`, `FLAG_GROUP_SUMMARY`, **même canal**), qui survit
     * à l'annulation de ses enfants et se retrouvait compté comme un avertissement Niumi par le
     * test suivant — `[5, 0]` au lieu de `[5]`. Niumi ne publie jamais de résumé : tout ce qui
     * porte ce drapeau vient du système et n'a rien à voir avec ce qui est vérifié ici.
     *
     * C'est la vraie cause de l'intermittence mesurée à l'étape 13, et elle ne se manifestait
     * qu'après le test qui publie les six avertissements.
     */
    private fun warningStatusBarNotifications() =
        notificationManager.activeNotifications
            .filter { it.notification.channelId == NiumiNotificationChannels.sessionWarning.id }
            .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }

    private fun warningNotifications(): List<Notification> = warningStatusBarNotifications().map { it.notification }

    private fun activeWarningIds(): List<Int> = warningStatusBarNotifications().map { it.id }
}
