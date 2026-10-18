package com.niumi.feature.session.incident

import com.google.common.truth.Truth.assertThat
import com.niumi.system.readiness.AndroidIncidentCodes
import org.junit.Test

/** Libellés partagés des écrans 7 et 12 (SPEC_ANDROID §15). */
class IncidentTextsTest {
    /**
     * Lot 9 : un incident « plein écran » d'une version antérieure peut encore être en base. Il
     * reste affiché en clair, marqué historique : le réglage n'a plus d'effet sur Niumi.
     */
    @Test
    fun theHistoricalFullScreenIncidentKeepsAPlainLabel() {
        assertThat(IncidentTexts.label(AndroidIncidentCodes.FULL_SCREEN_REVOKED))
            .isEqualTo("L'autorisation plein écran avait été retirée (ancienne version de Niumi, sans objet depuis).")
    }

    /** §15 : un code inconnu reste affiché tel quel plutôt que masqué. */
    @Test
    fun anUnknownCodeIsShownAsIs() {
        assertThat(IncidentTexts.label("ANDROID_SOMETHING_NOBODY_MAPPED")).isEqualTo("ANDROID_SOMETHING_NOBODY_MAPPED")
    }
}
