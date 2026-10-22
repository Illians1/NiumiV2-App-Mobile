package com.niumi.feature.setup.permissions

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * « Autoriser » sur la tuile « Notifications » (SPEC_ANDROID §13, étape 36) : la fenêtre système
 * tant qu'Android peut la montrer, les réglages de notification de Niumi sinon — jamais un bouton
 * sans effet. Relecture de l'étape 36 : la décision se prend **avant** de lancer la fenêtre, d'après
 * une demande déjà faite persistée, et non d'après le seul résultat d'un lancement.
 */
class NotificationRequestTest {
    @Test
    fun theFirstRequestShowsTheSystemDialog() {
        assertThat(notificationRequestFor(ANDROID_13, requestedBefore = false, showRationale = false))
            .isEqualTo(NotificationRequest.SYSTEM_DIALOG)
    }

    @Test
    fun aFirstRefusalLeavesTheDialogOneMoreChance() {
        assertThat(notificationRequestFor(ANDROID_13, requestedBefore = true, showRationale = true))
            .isEqualTo(NotificationRequest.SYSTEM_DIALOG)
    }

    @Test
    fun aDialogAndroidNoLongerShowsLeadsToTheSettings() {
        // Deux refus, ou des notifications coupées dans les réglages après une première demande.
        assertThat(notificationRequestFor(ANDROID_13, requestedBefore = true, showRationale = false))
            .isEqualTo(NotificationRequest.APP_SETTINGS)
    }

    @Test
    fun beforeAndroid13ThereIsNoDialogOnlyTheSettings() {
        assertThat(notificationRequestFor(ANDROID_12L, requestedBefore = false, showRationale = false))
            .isEqualTo(NotificationRequest.APP_SETTINGS)
    }

    private companion object {
        const val ANDROID_13 = 33
        const val ANDROID_12L = 32
    }
}
