package com.niumi.system.audio

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
 * Complément instrumenté de `DataStoreAlarmSoundPreferences` (étape 27) : DataStore écrit et relit
 * réellement, y compris l'encodage du volume constant par l'absence de clé. Nécessite un appareil
 * ou un émulateur (`connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class DataStoreAlarmSoundPreferencesInstrumentedTest {
    private lateinit var preferences: DataStoreAlarmSoundPreferences

    @Before
    fun setUp() =
        runTest {
            preferences =
                DataStoreAlarmSoundPreferences(
                    Provider { ApplicationProvider.getApplicationContext<Context>() },
                    AlwaysUnlocked,
                )
            // Le DataStore est partagé par le processus de test : repartir des défauts (même
            // patron que DataStoreAppSelectionStoreInstrumentedTest).
            preferences.write(AlarmSoundSettings())
        }

    @Test
    fun theDefaultsAreReturnedAfterBeingWritten() =
        runTest {
            assertThat(preferences.read())
                .isEqualTo(AlarmSoundSettings(NiumiRingtones.DEFAULT_KEY, VolumeRampDurations.DEFAULT_SECONDS))
        }

    @Test
    fun writeThenReadReturnsTheSameSettings() =
        runTest {
            preferences.write(AlarmSoundSettings("niumi_oiseaux", 300))

            assertThat(preferences.read()).isEqualTo(AlarmSoundSettings("niumi_oiseaux", 300))
        }

    @Test
    fun aConstantVolumeRoundTripsAsNull() =
        runTest {
            preferences.write(AlarmSoundSettings("niumi_bell", 60))

            preferences.write(AlarmSoundSettings("niumi_bell", null))

            assertThat(preferences.read()).isEqualTo(AlarmSoundSettings("niumi_bell", null))
        }

    @Test
    fun writeOverwritesThePreviousSettings() =
        runTest {
            preferences.write(AlarmSoundSettings("niumi_oiseaux", 300))

            preferences.write(AlarmSoundSettings("niumi_piano", 30))

            assertThat(preferences.read()).isEqualTo(AlarmSoundSettings("niumi_piano", 30))
        }
}

/** Ces tests portent sur le `DataStore`, pas sur la garde de déverrouillage (étape 19). */
private object AlwaysUnlocked : UnlockState {
    override val isUserUnlocked: Boolean = true
}
