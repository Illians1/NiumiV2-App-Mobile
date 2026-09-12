package com.niumi.feature.session.wake.fakes

import com.niumi.system.common.Clock
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.setup.SetupPreferences

/** Fakes locaux : ceux de `:core:system/src/test` ne sont pas exportés (aucun `testFixtures` dans ce dépôt). */
class FakeClock(
    var now: Long,
) : Clock {
    override fun nowEpochMillis(): Long = now
}

class FakeTimeZoneProvider(
    var zoneId: String,
) : TimeZoneProvider {
    override fun currentZoneId(): String = zoneId
}

class FakeSetupPreferences(
    private var lastWakeTimeIsoValue: String? = null,
) : SetupPreferences {
    private var onboardingAcknowledged = true
    private var batteryExemptionConfirmed = true

    override suspend fun isOnboardingAcknowledged(): Boolean = onboardingAcknowledged

    override suspend fun acknowledgeOnboarding() {
        onboardingAcknowledged = true
    }

    override suspend fun isBatteryExemptionConfirmed(): Boolean = batteryExemptionConfirmed

    override suspend fun setBatteryExemptionConfirmed(confirmed: Boolean) {
        batteryExemptionConfirmed = confirmed
    }

    override suspend fun lastWakeTimeIso(): String? = lastWakeTimeIsoValue

    override suspend fun setLastWakeTimeIso(value: String) {
        lastWakeTimeIsoValue = value
    }
}
