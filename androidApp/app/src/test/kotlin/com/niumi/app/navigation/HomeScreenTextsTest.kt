package com.niumi.app.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §15 : « ne jamais afficher un faux état de fiabilité ».
 *
 * **Défaut mesuré sur appareil le 2026-09-15 (étape 20).** Room rendue illisible, l'accueil
 * affichait « Aucune session » alors qu'une session était armée, l'alarme programmée et le blocage
 * en place. Le signal existait pourtant — `StorageIntegrityState` était bien alimenté — mais il
 * n'avait été câblé que dans [HomeUiState.destination], qui n'est lue qu'**au clic** du bouton
 * principal : rien ne l'affichait jamais. Ces deux fonctions pures portent désormais la règle, et
 * ce test la verrouille sans rendu Compose.
 */
class HomeScreenTextsTest {
    @Test
    fun anUnreadableStorageNeverClaimsThereIsNoSession() {
        val state = HomeUiState(hasActiveSession = false, storageUnreadable = true)

        assertThat(homeTitleFor(state)).isEqualTo("État illisible")
        assertThat(homeTitleFor(state)).isNotEqualTo("Aucune session")
    }

    /** Même quand une session est connue : sans lecture, l'accueil ne promet rien sur son compte. */
    @Test
    fun anUnreadableStorageTakesPrecedenceOverAKnownSession() {
        val state = HomeUiState(hasActiveSession = true, storageUnreadable = true)

        assertThat(homeTitleFor(state)).isEqualTo("État illisible")
        assertThat(primaryLabelFor(state)).isEqualTo(DIAGNOSTIC_BUTTON_LABEL)
    }

    @Test
    fun aReadableStorageKeepsTheOrdinaryTitles() {
        assertThat(homeTitleFor(HomeUiState(hasActiveSession = true))).isEqualTo("Une session est en cours.")
        assertThat(homeTitleFor(HomeUiState(hasActiveSession = false))).isEqualTo("Aucune session")
        assertThat(primaryLabelFor(HomeUiState(hasActiveSession = true))).isEqualTo(VIEW_SESSION_BUTTON_LABEL)
    }
}
