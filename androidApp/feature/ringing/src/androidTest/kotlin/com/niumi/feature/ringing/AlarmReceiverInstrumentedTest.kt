package com.niumi.feature.ringing

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.niumi.system.alarm.AlarmPendingIntentSpecs
import com.niumi.system.intent.AndroidPendingIntentFactory
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * SPEC_ANDROID §10.1 : `AlarmReceiver` reçoit un broadcast explicite avec des extras valides et
 * démarre `AlarmRingingService`, qui passe au premier plan. Nécessite un appareil ou un
 * émulateur : voir « Validation sur appareil réel » dans `CLAUDE.md`.
 *
 * `@HiltAndroidTest` + [HiltAndroidRule] sont obligatoires même si le test n'injecte rien
 * lui-même : `HiltTestApplication` ne construit son composant que lorsque cette règle s'exécute,
 * et `AlarmReceiver` (`@AndroidEntryPoint`) le réclame dès que le système l'instancie.
 *
 * On observe l'état réel du service (`RunningServiceInfo.foreground`) plutôt que la présence de
 * la notification dans le volet : cette dernière dépend de `POST_NOTIFICATIONS`, permission
 * d'exécution que certaines surcouches (HyperOS) refusent d'accorder à un APK de test, alors
 * que le comportement à vérifier — le service démarre et atteint le premier plan — n'en dépend
 * pas. Le contenu de la notification est couvert ailleurs (`RingingNotificationSpecsTest` en JVM,
 * `RingingNotificationFactoryInstrumentedTest` sur la vraie `Notification`).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlarmReceiverInstrumentedTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var pendingIntentFactory: AndroidPendingIntentFactory

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun receiverStartsTheRingingServiceInForeground() {
        val intent =
            Intent(context, AlarmReceiver::class.java)
                .putExtra(AlarmReceiver.EXTRA_SESSION_ID, SESSION_ID)
                .putExtra(AlarmReceiver.EXTRA_REVISION, 1L)

        context.sendBroadcast(intent)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(waitForRingingService()).isNotNull()
    }

    /**
     * Le test précédent construit son `Intent` à la main : il ne traverse donc pas la fabrique
     * qui produit réellement le `PendingIntent` remis à `AlarmManager`. C'est exactement dans
     * cette couture que `revision`, écrite comme chaîne et relue comme `Long`, faisait rejeter
     * la commande en silence — alarme déclenchée, aucune sonnerie. Ce test déclenche le vrai
     * `PendingIntent`, comme le ferait `AlarmManager` à l'heure du réveil.
     */
    @Test
    fun realAlarmPendingIntentStartsTheRingingService() {
        val pendingIntent =
            pendingIntentFactory.create(AlarmPendingIntentSpecs.alarm(SESSION_ID, revision = 1L))

        pendingIntent.send()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(waitForRingingService()).isNotNull()
    }

    /**
     * `getRunningServices` ne retourne que les services de l'appelant depuis l'API 26 — ce qui
     * suffit ici et évite toute dépendance à une permission.
     */
    private fun waitForRingingService(): ActivityManager.RunningServiceInfo? {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val deadline = System.currentTimeMillis() + SERVICE_START_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val service =
                activityManager
                    .getRunningServices(Int.MAX_VALUE)
                    .firstOrNull { it.service.className == AlarmRingingService::class.java.name }
            if (service != null && service.foreground) return service
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return null
    }

    @After
    fun tearDown() {
        context.stopService(Intent(context, AlarmRingingService::class.java))
    }

    private companion object {
        const val SESSION_ID = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"
        const val SERVICE_START_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 100L
    }
}
