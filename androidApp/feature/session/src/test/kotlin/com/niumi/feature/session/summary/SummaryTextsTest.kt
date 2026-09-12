package com.niumi.feature.session.summary

import com.google.common.truth.Truth.assertThat
import com.niumi.core.diagnostics.ActivationReasonCode
import org.junit.Test

/** Contenu imposé de l'écran 6 (SPEC_ANDROID §15) et tutoiement partout. */
class SummaryTextsTest {
    private val allTexts =
        listOf(
            SummaryTexts.TITLE,
            SummaryTexts.ACTIVATE_BUTTON_LABEL,
            SummaryTexts.CHANGE_TIME_LABEL,
            SummaryTexts.COMMITMENT_REMINDER,
            SummaryTexts.BLOCKED_APPS_TITLE,
            SummaryTexts.BOX_TITLE,
            SummaryTexts.SESSION_IN_PROGRESS_MESSAGE,
            SummaryTexts.INVALID_SCHEDULE_MESSAGE,
            SummaryTexts.REJECTED_MESSAGE,
            SummaryTexts.DUPLICATE_MESSAGE,
            SummaryTexts.activationFailed(failureCode = null),
            SummaryTexts.activationFailed(failureCode = "ANDROID_ALARM_SCHEDULE_FAILED"),
            SummaryTexts.daylightSavingShift("02:30", "03:00"),
        )

    @Test
    fun theCommitmentReminderIsStatedWordForWord() {
        assertThat(SummaryTexts.COMMITMENT_REMINDER).isEqualTo("Seul le scan du boîtier terminera la session.")
    }

    @Test
    fun theActivationButtonCarriesItsImposedLabel() {
        assertThat(SummaryTexts.ACTIVATE_BUTTON_LABEL).isEqualTo("Activer ma session")
    }

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

    @Test
    fun aFailureCodeIsNamedSoThatSupportCanUseIt() {
        assertThat(SummaryTexts.activationFailed("ANDROID_ALARM_SCHEDULE_FAILED"))
            .contains("ANDROID_ALARM_SCHEDULE_FAILED")
    }

    @Test
    fun theDaylightSavingExplanationNamesBothTimes() {
        val text = SummaryTexts.daylightSavingShift("02:30", "03:00")

        assertThat(text).contains("02:30")
        assertThat(text).contains("03:00")
    }

    @Test
    fun theThreeJourneyCausesAreNamedAndDeviceChecksPointBackToTheDiagnosis() {
        assertThat(SummaryTexts.blockingReason(ActivationReasonCode.TRIGGER_NOT_IN_FUTURE)).contains("heure")
        assertThat(SummaryTexts.blockingReason(ActivationReasonCode.NO_PAIRED_BOX)).contains("boîtier")
        assertThat(SummaryTexts.blockingReason(ActivationReasonCode.INVALID_APP_SELECTION)).contains("applications")
        assertThat(SummaryTexts.blockingReason(ActivationReasonCode.READINESS_BLOCKING_FOR_ALARM))
            .contains("diagnostic")
    }
}
