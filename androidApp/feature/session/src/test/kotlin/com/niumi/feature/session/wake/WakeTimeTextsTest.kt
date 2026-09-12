package com.niumi.feature.session.wake

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Tutoiement partout (SPEC_ANDROID §15) : aucun texte de cet écran ne dit « vous » ou « votre ». */
class WakeTimeTextsTest {
    private val allTexts =
        listOf(
            WakeTimeTexts.TITLE,
            WakeTimeTexts.CONTINUE_BUTTON_LABEL,
            WakeTimeTexts.INVALID_TIME_MESSAGE,
            WakeTimeTexts.UNKNOWN_ZONE_MESSAGE,
            WakeTimeTexts.SESSION_IN_PROGRESS_MESSAGE,
        )

    @Test
    fun noTextUsesVouvoiement() {
        allTexts.forEach { text ->
            assertThat(text.lowercase()).doesNotContain("vous")
            assertThat(text.lowercase()).doesNotContain("votre")
        }
    }

    @Test
    fun labelsAreNotEmpty() {
        allTexts.forEach { text -> assertThat(text).isNotEmpty() }
    }
}
