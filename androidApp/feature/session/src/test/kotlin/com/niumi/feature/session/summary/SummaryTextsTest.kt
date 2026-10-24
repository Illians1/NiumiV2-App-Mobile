package com.niumi.feature.session.summary

import com.google.common.truth.Truth.assertThat
import com.niumi.core.diagnostics.ActivationReasonCode
import com.niumi.core.interop.ActivationReasonDto
import com.niumi.feature.session.wake.WakeTimeTexts
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessMessages
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
            SummaryTexts.BLOCKING_TITLE,
            SummaryTexts.BLOCKING_IMMEDIATE_LABEL,
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
        assertThat(SummaryTexts.COMMITMENT_REMINDER)
            .isEqualTo("Seul le scan d'un de tes Niumi Spots terminera la session.")
    }

    /** §15 (Lot 8) : la ligne liste plusieurs Niumi Spots. */
    @Test
    fun theBoxLineIsTitledInThePlural() {
        assertThat(SummaryTexts.BOX_TITLE).isEqualTo("Niumi Spots associés")
    }

    @Test
    fun theNoPairedBoxRefusalAsksForAtLeastOneBox() {
        assertThat(SummaryTexts.blockingReason(ActivationReasonCode.NO_PAIRED_BOX))
            .isEqualTo("Aucun Niumi Spot n'est associé. Associe au moins un Niumi Spot avant d'activer ta session.")
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

    /** Lot 6 : SPEC_ANDROID §15, « Écran 6 ». */
    @Test
    fun theBlockingLineUsesTheWordingOfTheSpecification() {
        assertThat(SummaryTexts.BLOCKING_TITLE).isEqualTo("Blocage des applications")
        assertThat(SummaryTexts.BLOCKING_IMMEDIATE_LABEL).isEqualTo("Dès l'activation")
    }

    /**
     * §15 impose la même phrase aux écrans 5 et 6 pour le même fait : une seule chaîne existe, et
     * ce test interdit qu'une seconde copie apparaisse ici.
     */
    @Test
    fun theBlockingStartRefusalIsWordForWordTheOneOfScreenFive() {
        assertThat(SummaryTexts.blockingReason(ActivationReasonCode.BLOCKING_START_NOT_BEFORE_TRIGGER))
            .isEqualTo(WakeTimeTexts.BLOCKING_NOT_BEFORE_TRIGGER_MESSAGE)
    }

    @Test
    fun theThreeJourneyCausesAreNamed() {
        assertThat(SummaryTexts.blockingReason(ActivationReasonCode.TRIGGER_NOT_IN_FUTURE)).contains("heure")
        assertThat(SummaryTexts.blockingReason(ActivationReasonCode.NO_PAIRED_BOX)).contains("Niumi Spot")
        assertThat(SummaryTexts.blockingReason(ActivationReasonCode.INVALID_APP_SELECTION)).contains("applications")
    }

    /**
     * Étape 36 : le diagnostic de la mise en route n'existe plus. Un contrôle d'appareil qui refuse
     * l'activation est nommé par son message de §13, qui dit aussi quoi faire ; le repli ne renvoie
     * plus vers un écran disparu.
     */
    @Test
    fun aDeviceCheckRefusalNamesTheCheckWithItsOwnMessage() {
        val reason =
            ActivationReasonDto(ActivationReasonCode.READINESS_BLOCKING_FOR_ALARM, ReadinessCheckId.ALARM_VOLUME.name)

        assertThat(SummaryTexts.refusal(reason)).isEqualTo(ReadinessMessages.forCheck(ReadinessCheckId.ALARM_VOLUME))
    }

    @Test
    fun aRefusalWithoutACheckKeepsItsCommonWording() {
        val reason = ActivationReasonDto(ActivationReasonCode.NO_PAIRED_BOX, null)

        assertThat(
            SummaryTexts.refusal(reason),
        ).isEqualTo(SummaryTexts.blockingReason(ActivationReasonCode.NO_PAIRED_BOX))
    }

    @Test
    fun theGenericRefusalNoLongerPointsToTheRemovedDiagnosis() {
        assertThat(SummaryTexts.blockingReason(ActivationReasonCode.READINESS_BLOCKING_FOR_ALARM))
            .isEqualTo("Ton appareil n’est pas prêt.")
    }
}
