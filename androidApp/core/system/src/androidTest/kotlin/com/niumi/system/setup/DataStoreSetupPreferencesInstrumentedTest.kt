package com.niumi.system.setup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * Complément instrumenté de `DataStoreSetupPreferences` : DataStore écrit et relit réellement.
 * `lastWakeTimeIso` (étape 14) mémorise la dernière heure choisie sur l'écran 5, hors Room au même
 * titre que les deux accusés de réception. Nécessite un appareil ou un émulateur
 * (`connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class DataStoreSetupPreferencesInstrumentedTest {
    private lateinit var preferences: DataStoreSetupPreferences

    @Before
    fun setUp() {
        preferences =
            DataStoreSetupPreferences(Provider { ApplicationProvider.getApplicationContext<Context>() }, AlwaysUnlocked)
    }

    @Test
    fun lastWakeTimeIsoIsAbsentBeforeAnyWrite() =
        runTest {
            assertThat(preferences.lastWakeTimeIso()).isNull()
        }

    @Test
    fun setLastWakeTimeIsoThenLastWakeTimeIsoReturnsTheSameValue() =
        runTest {
            preferences.setLastWakeTimeIso("07:00")

            assertThat(preferences.lastWakeTimeIso()).isEqualTo("07:00")
        }

    @Test
    fun setLastWakeTimeIsoOverwritesThePreviousValue() =
        runTest {
            preferences.setLastWakeTimeIso("07:00")

            preferences.setLastWakeTimeIso("06:30")

            assertThat(preferences.lastWakeTimeIso()).isEqualTo("06:30")
        }
}

/** Ces tests portent sur le `DataStore`, pas sur la garde de déverrouillage (étape 19). */
private object AlwaysUnlocked : UnlockState {
    override val isUserUnlocked: Boolean = true
}
