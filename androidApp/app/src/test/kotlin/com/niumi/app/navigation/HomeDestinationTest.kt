package com.niumi.app.navigation

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.SESSION_FINAL_STATES
import org.junit.Test

/**
 * Destination de l'ouverture de Niumi (étape 45 ; `design-plan/SPECIFICATION.md` §3) : l'accueil
 * dès que la mise en route est finie, et toujours pendant une session (seconde garantie d'accès à
 * l'annulation et au scan, SPEC_ANDROID §10.4) ; sinon la mise en route, là où elle en est. Un
 * stockage illisible mène au diagnostic avant tout (§15, étape 20).
 */
class HomeDestinationTest {
    @Test
    fun aFinishedSetupOpensTheHome() {
        assertThat(homeDestinationFor(state = null, onboardingAcknowledged = true, setupCompleted = true))
            .isEqualTo(NiumiRoute.Home)
    }

    @Test
    fun everyNonFinalStateOpensTheHomeEvenBeforeTheEndOfTheSetup() {
        SessionStateDto.entries.filterNot { it in SESSION_FINAL_STATES }.forEach { state ->
            assertThat(homeDestinationFor(state, onboardingAcknowledged = false, setupCompleted = false))
                .isEqualTo(NiumiRoute.Home)
        }
    }

    @Test
    fun anUnfinishedSetupResumesAtThePermissionsOnceThePresentationIsRead() {
        assertThat(homeDestinationFor(state = null, onboardingAcknowledged = true, setupCompleted = false))
            .isEqualTo(NiumiRoute.Permissions)
        assertThat(homeDestinationFor(SessionStateDto.CANCELLED, onboardingAcknowledged = true, setupCompleted = false))
            .isEqualTo(NiumiRoute.Permissions)
    }

    @Test
    fun anUnreadPresentationComesFirst() {
        assertThat(homeDestinationFor(state = null, onboardingAcknowledged = false, setupCompleted = false))
            .isEqualTo(NiumiRoute.Presentation)
    }

    @Test
    fun anUnreadableStorageSendsTheUserToTheIncidentDiagnosticBeforeAnythingElse() {
        listOf(null, SessionStateDto.ARMED).forEach { state ->
            assertThat(
                homeDestinationFor(
                    state,
                    onboardingAcknowledged = true,
                    setupCompleted = true,
                    storageUnreadable = true,
                ),
            ).isEqualTo(NiumiRoute.IncidentDiagnostic)
        }
    }
}
