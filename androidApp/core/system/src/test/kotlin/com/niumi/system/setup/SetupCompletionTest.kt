package com.niumi.system.setup

import com.google.common.truth.Truth.assertThat
import com.niumi.system.plan.fakes.InMemoryAppSelectionStore
import com.niumi.system.readiness.fakes.FakePairedBoxStore
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Fin de la mise en route (étape 45, décision de Mehdi du 2026-10-25) : la préférence écrite par
 * « Continuer » de l'écran 4 ; à défaut, une installation antérieure qui a déjà un Niumi Spot et des
 * applications choisies est considérée comme configurée.
 */
class SetupCompletionTest {
    private val preferences = RecordingSetupPreferences()

    @Test
    fun theWrittenPreferenceCompletesTheSetup() =
        runTest {
            preferences.setupCompleted = true
            val completion =
                SetupCompletion(preferences, FakePairedBoxStore(emptyList()), InMemoryAppSelectionStore(emptyList()))

            assertThat(completion.isCompleted()).isTrue()
        }

    @Test
    fun anEarlierInstallWithASpotAndAppsIsConsideredConfigured() =
        runTest {
            val completion = SetupCompletion(preferences, FakePairedBoxStore(), InMemoryAppSelectionStore())

            assertThat(completion.isCompleted()).isTrue()
        }

    @Test
    fun withoutAppsTheSetupIsNotCompleted() =
        runTest {
            val completion = SetupCompletion(preferences, FakePairedBoxStore(), InMemoryAppSelectionStore(emptyList()))

            assertThat(completion.isCompleted()).isFalse()
        }

    @Test
    fun withoutANiumiSpotTheSetupIsNotCompleted() =
        runTest {
            val completion = SetupCompletion(preferences, FakePairedBoxStore(emptyList()), InMemoryAppSelectionStore())

            assertThat(completion.isCompleted()).isFalse()
        }
}

private class RecordingSetupPreferences : SetupPreferences {
    var setupCompleted = false

    override suspend fun isOnboardingAcknowledged(): Boolean = true

    override suspend fun acknowledgeOnboarding() = Unit

    override suspend fun isSetupCompleted(): Boolean = setupCompleted

    override suspend fun markSetupCompleted() {
        setupCompleted = true
    }

    override suspend fun isNotificationPermissionRequested(): Boolean = false

    override suspend fun markNotificationPermissionRequested() = Unit
}
