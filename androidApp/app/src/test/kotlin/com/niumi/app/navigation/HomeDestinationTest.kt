package com.niumi.app.navigation

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import org.junit.Test

/**
 * SPEC_ANDROID §10.4 : « Ouvrir Niumi depuis le lanceur pendant une session active mène toujours
 * à l'écran correspondant à l'état, jamais à l'accueil : c'est la seconde garantie d'accès au
 * scan. » La règle est isolée dans une fonction pure pour être prouvée sans rendu Compose.
 */
class HomeDestinationTest {
    @Test
    fun everyNonFinalStateLeadsToTheActiveSessionScreen() {
        val nonFinalStates =
            SessionStateDto.entries.filterNot {
                it == SessionStateDto.COMPLETED ||
                    it == SessionStateDto.CANCELLED ||
                    it == SessionStateDto.FAILED
            }

        // Exhaustivité : un état ajouté à la machine commune doit être classé explicitement.
        assertThat(nonFinalStates).hasSize(SessionStateDto.entries.size - 3)
        nonFinalStates.forEach { state ->
            assertThat(homeDestinationFor(state, onboardingAcknowledged = true))
                .isEqualTo(NiumiRoute.ActiveSession)
            assertThat(homeDestinationFor(state, onboardingAcknowledged = false))
                .isEqualTo(NiumiRoute.ActiveSession)
        }
    }

    @Test
    fun everyFinalStateFallsBackToTheSetupJourney() {
        listOf(SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.FAILED)
            .forEach { state ->
                assertThat(homeDestinationFor(state, onboardingAcknowledged = true))
                    .isEqualTo(NiumiRoute.Readiness)
            }
    }

    @Test
    fun noSessionAndNoAcknowledgementLeadsToOnboarding() {
        assertThat(homeDestinationFor(state = null, onboardingAcknowledged = false))
            .isEqualTo(NiumiRoute.Onboarding)
    }

    @Test
    fun noSessionWithAnAcknowledgementLeadsStraightToTheDiagnostic() {
        assertThat(homeDestinationFor(state = null, onboardingAcknowledged = true))
            .isEqualTo(NiumiRoute.Readiness)
    }

    @Test
    fun anUnacknowledgedOnboardingNeverSkipsAheadOfADiagnostic() {
        assertThat(homeDestinationFor(SessionStateDto.COMPLETED, onboardingAcknowledged = false))
            .isEqualTo(NiumiRoute.Onboarding)
    }
}
