package com.niumi.feature.setup.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Contenu imposé avant la première activation. SPEC_ANDROID §3 (dernier point) exige que
 * l'absence de tout secours logiciel soit expliquée avant la première session ; §4.2, §4.3 et
 * §4.4 fixent les trois autres limites ; §13 et §13.1 ajoutent la fragilité de l'exemption
 * d'énergie et le caractère non immédiat de l'avertissement.
 */
class OnboardingTextsTest {
    @Test
    fun explainsThatAForceStopRemovesTheScheduledAlarm() {
        assertThat(OnboardingTexts.limits).contains(
            "Un arrêt forcé depuis les réglages supprime le réveil programmé : Niumi ne sonnera pas.",
        )
    }

    @Test
    fun explainsThatScanningOnALockedScreenIsNotGuaranteed() {
        assertThat(OnboardingTexts.limits).contains(
            "Le scan sur écran verrouillé n'est pas garanti : ton téléphone peut exiger un " +
                "déverrouillage avant de lire le boîtier.",
        )
    }

    @Test
    fun explainsThatTheAccessibilityServiceCanBeDisabledAtAnyTime() {
        assertThat(OnboardingTexts.limits).contains(
            "Le service d'accessibilité peut être désactivé à tout moment dans les réglages " +
                "Android, ce qui arrête le blocage.",
        )
    }

    @Test
    fun explainsThatThereIsNoSoftwareFallbackDuringASession() {
        assertThat(OnboardingTexts.limits).contains(
            "Il n'existe aucun secours logiciel pendant une session : ni code, ni délai, ni " +
                "bouton « Arrêter quand même ». Sans ton boîtier, il te reste l'arrêt forcé ou " +
                "l'extinction du téléphone.",
        )
    }

    @Test
    fun warnsThatAnUpdateMayResetTheBatteryExemption() {
        assertThat(OnboardingTexts.limits).contains(
            "Une mise à jour de Niumi peut réinitialiser l'exemption d'énergie. Le diagnostic la " +
                "revérifie avant chaque session.",
        )
    }

    @Test
    fun warnsThatTheDegradationWarningIsNeverGuaranteedImmediate() {
        assertThat(OnboardingTexts.limits).contains(
            "Si un réglage casse ton réveil après l'activation, Niumi t'avertit au plus tôt, " +
                "jamais immédiatement : le système peut l'avoir arrêté entre-temps.",
        )
    }

    @Test
    fun exposesExactlySixLimits() {
        assertThat(OnboardingTexts.limits).hasSize(6)
    }

    @Test
    fun noLimitIsBlank() {
        OnboardingTexts.limits.forEach { limit -> assertThat(limit.isBlank()).isFalse() }
    }

    @Test
    fun exposesTheTitleTheAcknowledgementAndTheContinueLabel() {
        assertThat(OnboardingTexts.TITLE).isEqualTo("Avant ta première session")
        assertThat(OnboardingTexts.ACKNOWLEDGEMENT_LABEL).isEqualTo("J'ai compris")
        assertThat(OnboardingTexts.CONTINUE_BUTTON_LABEL).isEqualTo("Continuer")
    }
}
