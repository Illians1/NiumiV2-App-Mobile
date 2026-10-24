package com.niumi.feature.setup.accessibility

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Contenu imposé par SPEC_ANDROID §12.3 : les cinq points à expliquer avant d'ouvrir les
 * réglages d'accessibilité, en tutoiement. Depuis l'étape 36, la page s'ouvre par « Autoriser » sur
 * la tuile « Blocage des applications », dans une fenêtre centrée (décision du 2026-10-21), et porte
 * l'apostrophe typographique (D13).
 */
class AccessibilityConsentTextsTest {
    @Test
    fun explainsThatNiumiObservesTheDisplayedApplicationName() {
        assertThat(AccessibilityConsentTexts.points)
            .contains("Niumi observe le nom de l’application affichée.")
    }

    @Test
    fun explainsThatThisInformationOnlySendsBlockedApplicationsHome() {
        assertThat(AccessibilityConsentTexts.points)
            .contains("Cette information sert uniquement à renvoyer les applications choisies vers l’accueil.")
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
    fun explainsThatNiumiCannotWorkWithoutThisPermission() {
        // Texte de Mehdi du 2026-10-22, à la place de « Une session ne peut pas être activée… ».
        assertThat(AccessibilityConsentTexts.points)
            .contains("Sans cette autorisation, Niumi ne peut pas fonctionner.")
    }

    /**
     * Le chemin dans les réglages, sans libellé d'écran : les constructeurs les renomment, et un
     * libellé faux perdrait l'utilisateur (décision de Mehdi du 2026-10-22).
     */
    @Test
    fun explainsWhatToDoInTheSettings() {
        assertThat(AccessibilityConsentTexts.HOW_TO).isEqualTo("Dans les réglages, trouve Niumi et active-le.")
    }

    @Test
    fun exposesExactlyFivePoints() {
        assertThat(AccessibilityConsentTexts.points).hasSize(5)
    }

    @Test
    fun buttonLabelMatchesTheSpec() {
        assertThat(AccessibilityConsentTexts.OPEN_SETTINGS_BUTTON_LABEL)
            .isEqualTo("Ouvrir les réglages d’accessibilité")
        assertThat(AccessibilityConsentTexts.TITLE).isEqualTo("Autoriser le blocage des applications")
    }

    /**
     * D13. La description des réglages en est exclue : elle doit rester égale à la ressource XML
     * que lit Android, et n'est pas affichée par Niumi.
     */
    @Test
    fun noDisplayedTextCarriesAStraightApostrophe() {
        val displayed =
            AccessibilityConsentTexts.points +
                AccessibilityConsentTexts.TITLE +
                AccessibilityConsentTexts.HOW_TO +
                AccessibilityConsentTexts.OPEN_SETTINGS_BUTTON_LABEL
        assertThat(displayed.filter { it.contains('\'') }).isEmpty()
    }

    @Test
    fun settingsDescriptionMatchesTheAccessibilityServiceXmlDescription() {
        // Doit rester identique à niumi_accessibility_service_description
        // (:feature:session, res/values/strings.xml) : voir le commentaire des deux fichiers.
        assertThat(AccessibilityConsentTexts.SETTINGS_DESCRIPTION).isEqualTo(
            "Observe le nom de l'application affichée pour ramener les applications bloquées " +
                "à l'accueil tant qu'un réveil Niumi est programmé. Ne lit ni le contenu des écrans ni les saisies.",
        )
    }
}
