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

    /**
     * Le nettoyage était absent de `@Before` : un test héritait de l'annulation encore en vol du
     * précédent, ce qui faisait échouer tantôt l'un tantôt l'autre (voir [awaitNotifications]).
     * `clearWithoutPresentIsAlreadySatisfied` en dépend directement, son verdict étant lu sur
     * l'état du système.
     */
    @Before
    fun setUp() {
        AndroidNotificationChannelRegistrar(context).registerAll()
        clearAndWait()
    }

    @After
    fun tearDown() {
        clearAndWait()
    }

    private fun clearAndWait() {
        notifier.clear(sessionId)
        awaitNotifications(
            description = "plus aucune notification d'attente de scan",
            read = ::awaitingScanNotification,
        ) { it == null }
    }

    @Test
    fun presentPublishesAnOngoingAlarmCategoryNotificationWithNoAction() {
        notifier.present(sessionId)

        val posted =
            awaitNotifications(
                description = "la notification d'attente de scan publiée",
                read = ::awaitingScanNotification,
            ) { it != null }
        assertThat(posted).isNotNull()
        assertThat(posted!!.category).isEqualTo(Notification.CATEGORY_ALARM)
        assertThat(posted.actions).isNull()
        assertThat(posted.flags and Notification.FLAG_ONGOING_EVENT).isNotEqualTo(0)
    }

    @Test
    fun presentThenClearRemovesTheNotification() {
        notifier.present(sessionId)
        awaitNotifications(
            description = "la notification publiée avant de la retirer",
            read = ::awaitingScanNotification,
        ) { it != null }

        notifier.clear(sessionId)

        val remaining =
            awaitNotifications(
                description = "la notification retirée",
                read = ::awaitingScanNotification,
            ) { it == null }
        assertThat(remaining).isNull()
    }

    @Test
    fun clearWithoutPresentIsAlreadySatisfied() {
        val result = notifier.clear(sessionId)

        assertThat(result).isEqualTo(OperationResult.AlreadySatisfied)
    }

    /**
     * Le résumé de groupe fabriqué par Android au-delà de trois notifications sans groupe explicite
     * (`FLAG_GROUP_SUMMARY`, même canal) est exclu : Niumi n'en publie jamais, et le compter
     * ferait passer pour une notification d'attente de scan ce qui n'en est pas une. Même raison
     * que dans `AndroidSessionWarningNotifierInstrumentedTest`.
     */
    private fun awaitingScanNotification(): Notification? =
        notificationManager.activeNotifications
            .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
            .firstOrNull { it.notification.channelId == NiumiNotificationChannels.sessionAwaitingScan.id }
            ?.notification
}
