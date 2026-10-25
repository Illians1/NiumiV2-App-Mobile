package com.niumi.feature.session.transitional

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Page transitoire des réglages (étapes 45 à 48, décision de Mehdi du 2026-10-25). */
class TransitionalSettingsTextsTest {
    @Test
    fun theThreeLinksAreNamedWordForWord() {
        assertThat(TransitionalSettingsTexts.TITLE).isEqualTo("Réglages")
        assertThat(TransitionalSettingsTexts.NIUMI_SPOTS).isEqualTo("Niumi Spots")
        assertThat(TransitionalSettingsTexts.DIAGNOSTIC).isEqualTo("Diagnostic")
        // Libellé cité par `PRIVACY_POLICY.md` : le changer ici ferait mentir un document Play.
        assertThat(TransitionalSettingsTexts.HELP).isEqualTo("Aide et limites")
    }
}
