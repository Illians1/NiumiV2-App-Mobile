package com.niumi.feature.ringing

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.alarm.AlarmPendingIntentSpecs
import com.niumi.system.intent.AndroidPendingIntentFactory
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
 * SPEC_ANDROID §10.1, après l'étape 17 : `AlarmReceiver` ne démarre plus le service lui-même. Il
 * délègue à `AlarmTriggerHandler`, qui refuse de déclencher quand aucune session n'est persistée —
 * un `PendingIntent` survivant à une session effacée ne doit réveiller personne. Ce qui reste
 * observable est le journal technique : `ALARM_RECEIVED` est écrit dans **toutes** les branches
 * (§17), avant toute décision.
 *
 * La chaîne complète, jusqu'à `RINGING` et au service au premier plan, est couverte par
 * `AlarmChainInstrumentedTest` (`:app`), seul module dont le graphe Dagger est complet.
 *
 * `@HiltAndroidTest` + [HiltAndroidRule] sont obligatoires : `HiltTestApplication` ne construit
 * son composant que lorsque cette règle s'exécute, et `AlarmReceiver` (`@AndroidEntryPoint`) le
 * réclame dès que le système l'instancie. Nécessite un appareil ou un émulateur.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlarmReceiverInstrumentedTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var pendingIntentFactory: AndroidPendingIntentFactory

    @Inject
    lateinit var technicalEventLog: TechnicalEventLog

    @Inject
    lateinit var gateway: SessionPersistenceGateway

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        hiltRule.inject()
        // Précondition explicite plutôt que silencieuse : une session laissée par un autre test
        // ferait sonner l'appareil et rendrait l'assertion suivante trompeuse.
        assertThat(runBlocking { gateway.load() }).isEqualTo(LoadResult.Absent)
    }

    @Test
    fun aBroadcastWithoutASessionIsLoggedAndStartsNothing() {
        val intent =
            Intent(context, AlarmReceiver::class.java)
                .putExtra(AlarmReceiver.EXTRA_SESSION_ID, SESSION_ID)
                .putExtra(AlarmReceiver.EXTRA_REVISION, 1L)

        context.sendBroadcast(intent)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(waitForAlarmReceivedInJournal()).isTrue()
        assertThat(ringingService()).isNull()
    }

    /**
     * Le test précédent construit son `Intent` à la main : il ne traverse donc pas la fabrique
     * qui produit réellement le `PendingIntent` remis à `AlarmManager`. C'est exactement dans
     * cette couture que `revision`, écrite comme chaîne et relue comme `Long`, faisait rejeter
     * la commande en silence — alarme déclenchée, aucune sonnerie. Ce test déclenche le vrai
     * `PendingIntent`, comme le ferait `AlarmManager` à l'heure du réveil.
     */
    @Test
    fun theRealAlarmPendingIntentReachesTheReceiver() {
        val pendingIntent =
            pendingIntentFactory.create(AlarmPendingIntentSpecs.alarm(SESSION_ID, revision = 1L))

        pendingIntent.send()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(waitForAlarmReceivedInJournal()).isTrue()
    }

    /** `AlarmTriggerHandler` travaille dans `goAsync()` : l'écriture n'est pas immédiate. */
    private fun waitForAlarmReceivedInJournal(): Boolean {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val seen =
                runBlocking { technicalEventLog.recent() }
                    .any { it.type == TechnicalEventType.ALARM_RECEIVED }
            if (seen) return true
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    /**
     * `getRunningServices` ne retourne que les services de l'appelant depuis l'API 26 — ce qui
     * suffit ici et évite toute dépendance à une permission.
     */
    private fun ringingService(): ActivityManager.RunningServiceInfo? {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager
            .getRunningServices(Int.MAX_VALUE)
            .firstOrNull { it.service.className == AlarmRingingService::class.java.name }
    }

    @After
    fun tearDown() {
        context.stopService(Intent(context, AlarmRingingService::class.java))
    }

    private companion object {
        const val SESSION_ID = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"
        const val TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 100L
    }
}
