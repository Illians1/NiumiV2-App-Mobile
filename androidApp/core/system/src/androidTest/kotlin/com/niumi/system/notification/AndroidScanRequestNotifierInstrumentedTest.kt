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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * SPEC_ANDROID §10.5 : la vraie `Notification` produite par [AndroidScanRequestNotifier] est
 * `CATEGORY_ALARM`, `ongoing`, et sans aucune action — complément instrumenté du garde-fou déjà
 * couvert en JVM par `ScanRequestNotificationSpecsTest` sur le spec pur. Nécessite un appareil ou
 * un émulateur (`connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class AndroidScanRequestNotifierInstrumentedTest {
    /**
     * Sans `POST_NOTIFICATIONS` accordée (Android 13+), `notify()` est sans effet et
     * `activeNotifications` reste vide — l'échec ressemblerait à un défaut du notifier alors qu'il
     * ne s'agit que de la condition d'exécution. La permission n'existe pas avant l'API 33 : la
     * règle est neutralisée en dessous plutôt que de tenter un `pm grant` qui échouerait.
     */
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
        AndroidScanRequestNotifier(
            context = context,
            pendingIntentFactory =
                AndroidPendingIntentFactory(
                    context,
                    NiumiComponentResolver {
                        ComponentName(
                            context,
                            AndroidScanRequestNotifierInstrumentedTest::class.java,
                        )
                    },
                ),
            // L'icône réelle vit dans :core:designsystem, hors du graphe de :core:system : une
            // icône de plateforme suffit ici, seule compte la présence d'un identifiant valide.
            iconResolver = { android.R.drawable.ic_lock_idle_alarm },
        )
    private val sessionId = "44444444-4444-4444-4444-444444444444"

    @Before
    fun setUp() {
        AndroidNotificationChannelRegistrar(context).registerAll()
    }

    @After
    fun tearDown() {
        notifier.clear(sessionId)
    }

    @Test
    fun presentPublishesAnOngoingAlarmCategoryNotificationWithNoAction() {
        notifier.present(sessionId)

        val posted = awaitingScanNotification()
        assertThat(posted).isNotNull()
        assertThat(posted!!.category).isEqualTo(Notification.CATEGORY_ALARM)
        assertThat(posted.actions).isNull()
        assertThat(posted.flags and Notification.FLAG_ONGOING_EVENT).isNotEqualTo(0)
    }

    @Test
    fun presentThenClearRemovesTheNotification() {
        notifier.present(sessionId)

        notifier.clear(sessionId)

        assertThat(awaitingScanNotification()).isNull()
    }

    @Test
    fun clearWithoutPresentIsAlreadySatisfied() {
        val result = notifier.clear(sessionId)

        assertThat(result).isEqualTo(OperationResult.AlreadySatisfied)
    }

    private fun awaitingScanNotification(): Notification? =
        notificationManager.activeNotifications
            .firstOrNull { it.notification.channelId == NiumiNotificationChannels.sessionAwaitingScan.id }
            ?.notification
}
