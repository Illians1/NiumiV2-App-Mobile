package com.niumi.system.insight

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * Complément instrumenté de `DataStoreTimeToGetUpPreferences` (étape 43) : `niumi_time_to_get_up`
 * écrit et relu réellement. Nécessite un appareil ou un émulateur (`connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class DataStoreTimeToGetUpPreferencesInstrumentedTest {
    private val preferences =
        DataStoreTimeToGetUpPreferences(
            Provider { ApplicationProvider.getApplicationContext<Context>() },
            AlwaysUnlocked,
        )

    @Test
    fun everyAllowedDurationRoundTrips() =
        runTest {
            DeclaredMinutes.ALLOWED.forEach { minutes ->
                preferences.setDeclaredMinutes(minutes)

                assertThat(preferences.declaredMinutes()).isEqualTo(minutes)
            }
        }

    @Test
    fun aDurationOutsideTheListIsRefusedAndKeepsThePreviousOne() {
        runBlocking { preferences.setDeclaredMinutes(15) }

        val thrown =
            assertThrows(IllegalArgumentException::class.java) { runBlocking { preferences.setDeclaredMinutes(7) } }

        assertThat(thrown).hasMessageThat().isEqualTo("DECLARED_MINUTES_INVALID")
        assertThat(runBlocking { preferences.declaredMinutes() }).isEqualTo(15)
    }
}

/** Ces tests portent sur le `DataStore`, pas sur la garde de déverrouillage (`DataStoreUnlockGuardTest`). */
private object AlwaysUnlocked : UnlockState {
    override val isUserUnlocked: Boolean = true
}
