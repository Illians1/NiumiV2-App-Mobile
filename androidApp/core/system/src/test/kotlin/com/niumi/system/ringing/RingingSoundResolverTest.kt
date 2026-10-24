package com.niumi.system.ringing

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.audio.AlarmSound
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.audio.VolumeRamp
import com.niumi.system.session.LoadResult
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.SessionDtoFixtures.OTHER_SESSION_ID
import com.niumi.system.session.fakes.SessionDtoFixtures.SESSION_ID
import org.junit.Test

/** SPEC_ANDROID §10.2, Lot 7 : la table « ce qui sonne », et un réveil qui ne se tait jamais. */
class RingingSoundResolverTest {
    private fun present(
        ringtoneKey: String = "niumi_oiseaux",
        vibrationEnabled: Boolean = true,
        volumeRampSeconds: Int? = null,
        ringingAt: Long? = T0,
    ) = LoadResult.Present(
        snapshot = SessionDtoFixtures.snapshotInState(SessionStateDto.RINGING).copy(ringingAtEpochMillis = ringingAt),
        extras =
            SessionDtoFixtures.extras(
                ringtoneKey = ringtoneKey,
                vibrationEnabled = vibrationEnabled,
                volumeRampSeconds = volumeRampSeconds,
            ),
        pendingEffects = emptyList(),
    )

    private val safeDefault = AlarmSound(NiumiRingtones.DEFAULT_KEY, vibrationEnabled = true, volumeRamp = null)

    @Test
    fun theSessionRingtoneAndRampAreKeptAndTheRampStartsWhenTheSessionStartedRinging() {
        val resolved = RingingSoundResolver.resolve(present(volumeRampSeconds = 300), SESSION_ID, NOW)

        assertThat(resolved.sound).isEqualTo(AlarmSound("niumi_oiseaux", true, VolumeRamp(300_000, T0)))
        assertThat(resolved.fallback).isFalse()
    }

    @Test
    fun aRampDurationFromBeforeTheStep28TighteningIsSanitizedToTheClosestValue() {
        // Cas limite 5 du plan de refonte : une session armée avant l'étape 28 (30 s ou 1 min, hors
        // de la liste fermée depuis) doit tout de même sonner avec une montée, jamais muette.
        val resolved = RingingSoundResolver.resolve(present(volumeRampSeconds = 60), SESSION_ID, NOW)

        assertThat(resolved.sound.volumeRamp).isEqualTo(VolumeRamp(120_000, T0))
    }

    @Test
    fun aRingingInstantNotYetWrittenStartsTheRampNow() {
        val resolved = RingingSoundResolver.resolve(present(volumeRampSeconds = 120, ringingAt = null), SESSION_ID, NOW)

        assertThat(resolved.sound.volumeRamp).isEqualTo(VolumeRamp(120_000, NOW))
    }

    @Test
    fun noRampMeansConstantVolume() {
        val resolved = RingingSoundResolver.resolve(present(volumeRampSeconds = null), SESSION_ID, NOW)

        assertThat(resolved.sound.volumeRamp).isNull()
    }

    @Test
    fun theSessionVibrationSettingIsHonoured() {
        val resolved = RingingSoundResolver.resolve(present(vibrationEnabled = false), SESSION_ID, NOW)

        assertThat(resolved.sound.vibrationEnabled).isFalse()
    }

    @Test
    fun aKeyOutsideTheCatalogFallsBackToTheDefaultAndKeepsTheSessionRamp() {
        val resolved =
            RingingSoundResolver.resolve(
                present(ringtoneKey = NiumiRingtones.LEGACY_KEY, volumeRampSeconds = 300),
                SESSION_ID,
                NOW,
            )

        assertThat(resolved.sound).isEqualTo(AlarmSound(NiumiRingtones.DEFAULT_KEY, true, VolumeRamp(300_000, T0)))
        assertThat(resolved.fallback).isTrue()
    }

    @Test
    fun noSessionStillRingsTheDefaultAtConstantVolume() {
        val resolved = RingingSoundResolver.resolve(LoadResult.Absent, SESSION_ID, NOW)

        assertThat(resolved).isEqualTo(ResolvedAlarmSound(safeDefault, fallback = false))
    }

    @Test
    fun anUnreadableSessionStillRingsTheDefaultAtConstantVolume() {
        val resolved = RingingSoundResolver.resolve(LoadResult.Unreadable("boom"), SESSION_ID, NOW)

        assertThat(resolved).isEqualTo(ResolvedAlarmSound(safeDefault, fallback = false))
    }

    @Test
    fun anotherSessionIsNotTrustedForItsSound() {
        val resolved = RingingSoundResolver.resolve(present(volumeRampSeconds = 300), OTHER_SESSION_ID, NOW)

        assertThat(resolved).isEqualTo(ResolvedAlarmSound(safeDefault, fallback = false))
    }

    private companion object {
        const val T0 = 1_900_000_000_000L
        const val NOW = T0 + 45_000
    }
}
