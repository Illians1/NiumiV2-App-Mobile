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
 * `setupCompleted` (étape 45) est gardé hors Room au même titre que les deux accusés de réception.
 * Nécessite un appareil ou un émulateur
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

    /** Étape 45 : la mise en route se termine par « Continuer » de l'écran 4. */
    @Test
    fun markingTheSetupCompletedIsReadBack() =
        runTest {
            preferences.markSetupCompleted()

            assertThat(preferences.isSetupCompleted()).isTrue()
        }
}

/** Étape 36 : la fenêtre système des notifications a été proposée au moins une fois. */
@RunWith(AndroidJUnit4::class)
class NotificationPermissionRequestedPreferenceTest {
    @Test
    fun markingTheRequestIsReadBack() =
        runTest {
            val preferences =
                DataStoreSetupPreferences(
                    Provider { ApplicationProvider.getApplicationContext<Context>() },
                    AlwaysUnlocked,
                )

            preferences.markNotificationPermissionRequested()

            assertThat(preferences.isNotificationPermissionRequested()).isTrue()
        }
}

/** Ces tests portent sur le `DataStore`, pas sur la garde de déverrouillage (étape 19). */
private object AlwaysUnlocked : UnlockState {
    override val isUserUnlocked: Boolean = true
}
