package com.niumi.system.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * `sanitized()` est la seule fonction qui décide ce qu'une valeur lue (préférence ou extras de
 * session) signifie réellement, sur le patron de `RingingSoundResolver` : une clé ou une durée
 * invalide ne doit jamais se propager telle quelle (SPEC_ANDROID §15, Lot 7, étape 27).
 */
class AlarmSoundSettingsTest {
    @Test
    fun aKnownRingtoneAndRampAreKeptAsIs() {
        val settings = AlarmSoundSettings("niumi_oiseaux", 300).sanitized()

        assertThat(settings).isEqualTo(AlarmSoundSettings("niumi_oiseaux", 300))
    }

    @Test
    fun aNullRampMeansConstantVolumeAndIsKept() {
        val settings = AlarmSoundSettings("niumi_bell", null).sanitized()

        assertThat(settings).isEqualTo(AlarmSoundSettings("niumi_bell", null))
    }

    @Test
    fun anUnknownRingtoneFallsBackToTheDefaultKey() {
        val settings = AlarmSoundSettings("niumi_alarm", 60).sanitized()

        assertThat(settings.ringtoneKey).isEqualTo(NiumiRingtones.DEFAULT_KEY)
        assertThat(settings.volumeRampSeconds).isEqualTo(60)
    }

    @Test
    fun aRampDurationOutsideTheClosedListFallsBackToTheDefaultDuration() {
        val settings = AlarmSoundSettings("niumi_piano", 45).sanitized()

        assertThat(settings.volumeRampSeconds).isEqualTo(VolumeRampDurations.DEFAULT_SECONDS)
    }

    @Test
    fun theDefaultsAreAlreadySanitized() {
        val settings = AlarmSoundSettings().sanitized()

        assertThat(
            settings,
        ).isEqualTo(AlarmSoundSettings(NiumiRingtones.DEFAULT_KEY, VolumeRampDurations.DEFAULT_SECONDS))
    }

    /**
     * Mesuré sur appareil le 2026-10-02 (étape 27) : une préférence jamais écrite affichait
     * « Piano · volume constant » au lieu du vrai défaut « Piano · volume progressif sur 2 min ».
     * `ringtoneKey` absent du `DataStore` ne signifie pas « clé inconnue » (ce que [sanitized]
     * corrigerait déjà vers le défaut) mais « rien n'a jamais été écrit » : les deux ne peuvent pas
     * produire le même résultat, puisque seul le second doit redonner la montée par défaut plutôt
     * que le volume constant.
     */
    @Test
    fun decodingAnAbsentRingtoneKeyGivesTheFullDefaultsRatherThanAConstantVolume() {
        val decoded = decodeAlarmSoundSettings(ringtoneKey = null, volumeRampSeconds = null)

        assertThat(
            decoded,
        ).isEqualTo(AlarmSoundSettings(NiumiRingtones.DEFAULT_KEY, VolumeRampDurations.DEFAULT_SECONDS))
    }

    @Test
    fun decodingAPresentRingtoneKeyWithoutARampMeansConstantVolume() {
        val decoded = decodeAlarmSoundSettings(ringtoneKey = "niumi_bell", volumeRampSeconds = null)

        assertThat(decoded).isEqualTo(AlarmSoundSettings("niumi_bell", null))
    }

    @Test
    fun decodingAPresentRingtoneKeyWithARampKeepsBoth() {
        val decoded = decodeAlarmSoundSettings(ringtoneKey = "niumi_oiseaux", volumeRampSeconds = 300)

        assertThat(decoded).isEqualTo(AlarmSoundSettings("niumi_oiseaux", 300))
    }

    @Test
    fun decodingSanitizesAnUnknownRingtoneKey() {
        val decoded = decodeAlarmSoundSettings(ringtoneKey = "niumi_alarm", volumeRampSeconds = 60)

        assertThat(decoded.ringtoneKey).isEqualTo(NiumiRingtones.DEFAULT_KEY)
    }
}
