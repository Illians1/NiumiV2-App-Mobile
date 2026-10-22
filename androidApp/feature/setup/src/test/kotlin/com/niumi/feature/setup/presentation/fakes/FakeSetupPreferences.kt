package com.niumi.feature.setup.presentation.fakes

import com.niumi.system.setup.SetupPreferences

/** Préférences de mise en route en mémoire ; compte les acquittements de la présentation (écrans 1 et 2). */
class FakeSetupPreferences(
    private var onboardingAcknowledged: Boolean = true,
    private var lastWakeTimeIsoValue: String? = null,
    private var lastBlockingStartTimeIsoValue: String? = null,
) : SetupPreferences {
    var acknowledgeCallCount = 0
        private set

    override suspend fun isOnboardingAcknowledged(): Boolean = onboardingAcknowledged

    override suspend fun acknowledgeOnboarding() {
        acknowledgeCallCount++
        onboardingAcknowledged = true
    }

    override suspend fun lastWakeTimeIso(): String? = lastWakeTimeIsoValue

    override suspend fun lastBlockingStartTimeIso(): String? = lastBlockingStartTimeIsoValue

    override suspend fun setLastBlockingStartTimeIso(value: String?) {
        lastBlockingStartTimeIsoValue = value
    }

    override suspend fun setLastWakeTimeIso(value: String) {
        lastWakeTimeIsoValue = value
    }

    var notificationRequested = false

    override suspend fun isNotificationPermissionRequested(): Boolean = notificationRequested

    override suspend fun markNotificationPermissionRequested() {
        notificationRequested = true
    }
}
