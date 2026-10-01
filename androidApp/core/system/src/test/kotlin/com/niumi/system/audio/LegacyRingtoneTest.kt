package com.niumi.system.audio

import com.google.common.truth.Truth.assertThat
import com.niumi.database.migration.LegacyRingtone
import org.junit.Test

/**
 * `LegacyRingtone` vit dans `:core:database`, qui ne voit pas le catalogue (SPEC_ANDROID §6). Ce test
 * garde les deux d'accord : la clé réécrite par `MIGRATION_3_4` et `DirectBootMapper` est celle que le
 * catalogue a retirée, et sa remplaçante en fait partie.
 */
class LegacyRingtoneTest {
    @Test
    fun theMigratedKeyIsTheOneTheCatalogRetired() {
        assertThat(LegacyRingtone.KEY).isEqualTo(NiumiRingtones.LEGACY_KEY)
        assertThat(NiumiRingtones.byKey(LegacyRingtone.KEY)).isNull()
    }

    @Test
    fun theReplacementIsTheDefaultRingtone() {
        assertThat(LegacyRingtone.REPLACEMENT_KEY).isEqualTo(NiumiRingtones.DEFAULT_KEY)
        assertThat(NiumiRingtones.byKey(LegacyRingtone.REPLACEMENT_KEY)).isNotNull()
    }

    @Test
    fun onlyTheLegacyKeyIsRewritten() {
        assertThat(LegacyRingtone.migrate("niumi_alarm")).isEqualTo("niumi_piano")
        assertThat(LegacyRingtone.migrate("niumi_bell")).isEqualTo("niumi_bell")
    }
}
