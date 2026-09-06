package com.niumi.feature.setup.accessibility

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Contenu imposé par SPEC_ANDROID §12.3 : les cinq points à expliquer avant d'ouvrir les
 * réglages d'accessibilité, en tutoiement.
 */
class AccessibilityConsentTextsTest {
    @Test
    fun explainsThatNiumiObservesTheDisplayedApplicationName() {
        assertThat(AccessibilityConsentTexts.points)
            .contains("Niumi observe le nom de l'application affichée.")
    }

    @Test
    fun explainsThatThisInformationOnlySendsBlockedApplicationsHome() {
        assertThat(AccessibilityConsentTexts.points)
            .contains("Cette information sert uniquement à renvoyer les applications choisies vers l'accueil.")
    }

    @Test
    fun explainsThatNiumiDoesNotReadScreenContentOrInput() {
        assertThat(AccessibilityConsentTexts.points)
            .contains("Niumi ne lit pas le contenu des écrans ou les saisies.")
    }

    @Test
    fun explainsThatTheServiceCanBeDisabledInAndroidSettings() {
        assertThat(AccessibilityConsentTexts.points)
            .contains("Le service peut être désactivé dans les réglages Android.")
    }

    @Test
    fun explainsThatASessionCannotBeActivatedWithoutTheService() {
        assertThat(AccessibilityConsentTexts.points)
            .contains("Une session ne peut pas être activée si le service est inactif.")
    }

    @Test
    fun exposesExactlyFivePoints() {
        assertThat(AccessibilityConsentTexts.points).hasSize(5)
    }

    @Test
    fun buttonLabelMatchesTheSpec() {
        assertThat(AccessibilityConsentTexts.OPEN_SETTINGS_BUTTON_LABEL)
            .isEqualTo("Ouvrir les réglages d'accessibilité")
    }

    @Test
    fun settingsDescriptionMatchesTheAccessibilityServiceXmlDescription() {
        // Doit rester identique à niumi_accessibility_service_description
        // (:feature:session, res/values/strings.xml) : voir le commentaire des deux fichiers.
        assertThat(AccessibilityConsentTexts.SETTINGS_DESCRIPTION).isEqualTo(
            "Observe le nom de l'application affichée pour ramener les applications bloquées " +
                "à l'accueil pendant une session Niumi. Ne lit ni le contenu des écrans ni les saisies.",
        )
    }
}
