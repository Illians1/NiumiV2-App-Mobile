package com.niumi.feature.ringing

import com.google.common.truth.Truth.assertThat
import com.niumi.system.audio.NiumiRingtones
import org.junit.Test

class RingtoneResourcesTest {
    @Test
    fun everyCatalogKeyResolvesToADistinctResource() {
        val ids = NiumiRingtones.ALL.map { RingtoneResources.resourceId(it.key) }

        assertThat(ids).doesNotContain(null)
        assertThat(ids).doesNotContain(0)
        assertThat(ids.toSet()).hasSize(NiumiRingtones.ALL.size)
    }

    @Test
    fun keysOutsideTheCatalogResolveToNothing() {
        assertThat(RingtoneResources.resourceId(NiumiRingtones.LEGACY_KEY)).isNull()
        assertThat(RingtoneResources.resourceId("")).isNull()
        assertThat(RingtoneResources.resourceId("niumi_default")).isNull()
    }
}
