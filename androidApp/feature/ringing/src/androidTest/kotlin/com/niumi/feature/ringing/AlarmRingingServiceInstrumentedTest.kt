package com.niumi.feature.ringing

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * SPEC_ANDROID §10.2 : le service atteint réellement le premier plan, et n'expose aucun binding
 * exploitable pour l'arrêter. Nécessite un appareil ou un émulateur.
 *
 * `ServiceTestRule` est volontairement écartée : elle démarre le service via
 * `bindServiceAndWait()` et échoue sur un `TimeoutException` dès que `onBind()` renvoie `null`
 * — c'est-à-dire exactement dans le cas que la spec impose. Le plan MVP la suggérait ; elle est
 * structurellement incompatible avec un service sans binder (constaté sur appareil, voir
 * `ETAPE-03.md`). On démarre donc le service comme le fait la production, et on observe
 * `onNullBinding` pour prouver l'absence de binder.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlarmRingingServiceInstrumentedTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun serviceReachesForegroundState() {
        context.startForegroundService(ringingIntent())

        val service = waitForRingingService()

        assertThat(service).isNotNull()
        assertThat(service?.foreground).isTrue()
    }

    @Test
    fun bindingExposesNoBinder() {
        context.startForegroundService(ringingIntent())
        waitForRingingService()

        val nullBindingLatch = CountDownLatch(1)
        var connectedBinder: IBinder? = null
        val connection =
            object : ServiceConnection {
                override fun onServiceConnected(
                    name: ComponentName?,
                    binder: IBinder?,
                ) {
                    connectedBinder = binder
                }

                override fun onServiceDisconnected(name: ComponentName?) = Unit

                override fun onNullBinding(name: ComponentName?) {
                    nullBindingLatch.countDown()
                }
            }

        val bindRequested = context.bindService(ringingIntent(), connection, 0)
        val gotNullBinding = nullBindingLatch.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        context.unbindService(connection)

        assertThat(bindRequested).isTrue()
        assertThat(gotNullBinding).isTrue()
        assertThat(connectedBinder).isNull()
    }

    private fun ringingIntent(): Intent =
        Intent(context, AlarmRingingService::class.java)
            .putExtra(AlarmReceiver.EXTRA_SESSION_ID, "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b")
            .putExtra(AlarmReceiver.EXTRA_REVISION, 1L)

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
        const val SERVICE_START_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 100L
        const val BIND_TIMEOUT_SECONDS = 5L
    }
}
