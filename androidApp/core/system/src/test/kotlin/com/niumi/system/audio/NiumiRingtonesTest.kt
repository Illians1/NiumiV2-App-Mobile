package com.niumi.system.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NiumiRingtonesTest {
    @Test
    fun catalogListsTheFourRingtonesInDisplayOrder() {
        assertThat(NiumiRingtones.ALL.map { it.key to it.label })
            .containsExactly(
                "niumi_bell" to "Cloche",
                "niumi_energique" to "Énergique",
                "niumi_oiseaux" to "Oiseaux",
                "niumi_piano" to "Piano",
            ).inOrder()
    }

    @Test
    fun defaultIsPianoAndBelongsToTheCatalog() {
        assertThat(NiumiRingtones.DEFAULT_KEY).isEqualTo("niumi_piano")
        assertThat(NiumiRingtones.byKey(NiumiRingtones.DEFAULT_KEY)?.label).isEqualTo("Piano")
    }

    @Test
    fun theLegacyKeyIsNotInTheCatalog() {
        assertThat(NiumiRingtones.LEGACY_KEY).isEqualTo("niumi_alarm")
        assertThat(NiumiRingtones.byKey(NiumiRingtones.LEGACY_KEY)).isNull()
        assertThat(NiumiRingtones.byKey("")).isNull()
    }
}
