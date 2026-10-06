package com.niumi.feature.session.wake.fakes

import com.niumi.system.audio.AlarmSoundPreferences
import com.niumi.system.audio.AlarmSoundSettings
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
    var lastWakeTimeIsoValue: String? = null,
    var lastBlockingStartTimeIsoValue: String? = null,
) : SetupPreferences {
    private var onboardingAcknowledged = true

    override suspend fun isOnboardingAcknowledged(): Boolean = onboardingAcknowledged

    override suspend fun acknowledgeOnboarding() {
        onboardingAcknowledged = true
    }

    override suspend fun lastWakeTimeIso(): String? = lastWakeTimeIsoValue

    override suspend fun setLastWakeTimeIso(value: String) {
        lastWakeTimeIsoValue = value
    }

    override suspend fun lastBlockingStartTimeIso(): String? = lastBlockingStartTimeIsoValue

    override suspend fun setLastBlockingStartTimeIso(value: String?) {
        lastBlockingStartTimeIsoValue = value
    }
}

class FakeAlarmSoundPreferences(
    var settings: AlarmSoundSettings = AlarmSoundSettings(),
) : AlarmSoundPreferences {
    override suspend fun read(): AlarmSoundSettings = settings

    override suspend fun write(settings: AlarmSoundSettings) {
        this.settings = settings
    }
}
