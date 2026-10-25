package com.niumi.feature.setup.presentation.fakes

import com.niumi.system.setup.SetupPreferences

/** Préférences de mise en route en mémoire ; compte les acquittements de la présentation (écrans 1 et 2). */
class FakeSetupPreferences(
    private var onboardingAcknowledged: Boolean = true,
) : SetupPreferences {
    var acknowledgeCallCount = 0
        private set

    override suspend fun isOnboardingAcknowledged(): Boolean = onboardingAcknowledged

    override suspend fun acknowledgeOnboarding() {
        acknowledgeCallCount++
        onboardingAcknowledged = true
    }

    var setupCompleted = false

    override suspend fun isSetupCompleted(): Boolean = setupCompleted

    override suspend fun markSetupCompleted() {
        setupCompleted = true
    }

    var notificationRequested = false

    override suspend fun isNotificationPermissionRequested(): Boolean = notificationRequested

    override suspend fun markNotificationPermissionRequested() {
        notificationRequested = true
    }
}
