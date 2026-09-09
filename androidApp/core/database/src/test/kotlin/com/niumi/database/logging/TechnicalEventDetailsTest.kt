package com.niumi.database.logging

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Filtre de confidentialité de `detailsJson` (SPEC_ANDROID §16, §17) : `packageName` n'est retenu
 * que pour `BLOCK_APPLIED`, `errorCode` pour tout autre type. Toute autre clé — texte
 * d'accessibilité, hash de token, identifiant matériel — n'atteint jamais la base, quel que soit
 * ce qu'un appelant a construit.
 */
class TechnicalEventDetailsTest {
    @Test
    fun packageNameBuildsAMinimalJsonObject() {
        assertThat(TechnicalEventDetails.packageName("com.example.app"))
            .isEqualTo("""{"packageName":"com.example.app"}""")
    }

    @Test
    fun errorCodeBuildsAMinimalJsonObject() {
        assertThat(TechnicalEventDetails.errorCode("ANDROID_AUDIO_START_FAILED"))
            .isEqualTo("""{"errorCode":"ANDROID_AUDIO_START_FAILED"}""")
    }

    @Test
    fun packageNameIsKeptForBlockApplied() {
        val raw = TechnicalEventDetails.packageName("com.example.app")

        assertThat(TechnicalEventDetails.sanitize(TechnicalEventType.BLOCK_APPLIED, raw)).isEqualTo(raw)
    }

    @Test
    fun packageNameIsDroppedForAnyOtherType() {
        val raw = TechnicalEventDetails.packageName("com.example.app")

        assertThat(TechnicalEventDetails.sanitize(TechnicalEventType.ALARM_RECEIVED, raw)).isNull()
    }

    @Test
    fun errorCodeIsKeptForNonBlockAppliedTypes() {
        val raw = TechnicalEventDetails.errorCode("ANDROID_AUDIO_START_FAILED")

        assertThat(TechnicalEventDetails.sanitize(TechnicalEventType.AUDIO_START_FAILED, raw)).isEqualTo(raw)
    }

    @Test
    fun errorCodeIsDroppedForBlockApplied() {
        val raw = TechnicalEventDetails.errorCode("SOME_CODE")

        assertThat(TechnicalEventDetails.sanitize(TechnicalEventType.BLOCK_APPLIED, raw)).isNull()
    }

    @Test
    fun nullDetailsJsonStaysNull() {
        assertThat(TechnicalEventDetails.sanitize(TechnicalEventType.BLOCK_APPLIED, null)).isNull()
    }

    @Test
    fun anUnlistedKeyIsNeverWrittenEvenAlongsideAnAllowedOne() {
        val malicious = """{"packageName":"com.example.app","accessibilityText":"secret text"}"""

        val sanitized = TechnicalEventDetails.sanitize(TechnicalEventType.BLOCK_APPLIED, malicious)

        assertThat(sanitized).isEqualTo("""{"packageName":"com.example.app"}""")
        assertThat(sanitized).doesNotContain("accessibilityText")
        assertThat(sanitized).doesNotContain("secret text")
    }

    @Test
    fun aTokenHashUnderAnyKeyNameIsNeverWritten() {
        val malicious = """{"tokenSha256Hex":"${"a".repeat(64)}"}"""

        assertThat(TechnicalEventDetails.sanitize(TechnicalEventType.ALARM_RECEIVED, malicious)).isNull()
    }

    @Test
    fun malformedJsonNeverThrowsAndResultsInNull() {
        assertThat(TechnicalEventDetails.sanitize(TechnicalEventType.BLOCK_APPLIED, "not json")).isNull()
    }
}
