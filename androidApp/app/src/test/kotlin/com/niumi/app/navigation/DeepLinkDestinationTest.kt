package com.niumi.app.navigation

import com.google.common.truth.Truth.assertThat
import com.niumi.system.intent.NiumiDeepLink
import org.junit.Test

/**
 * SPEC_ANDROID §13.1 : le tap d'un avertissement ouvre `MainActivity`, qui redirige vers le
 * diagnostic d'incident (écran 12, étape 16).
 */
class DeepLinkDestinationTest {
    @Test
    fun theDiagnosticExtraOpensTheIncidentDiagnostic() {
        val route = deepLinkDestinationFor(NiumiDeepLink.DESTINATION_INCIDENT_DIAGNOSTIC)

        assertThat(route).isEqualTo(NiumiRoute.IncidentDiagnostic)
    }

    @Test
    fun anAbsentExtraOpensTheDefaultDestination() {
        assertThat(deepLinkDestinationFor(null)).isNull()
    }

    /**
     * Un `PendingIntent` survit à une mise à jour de l'application : une valeur qu'une version
     * antérieure a écrite et que celle-ci ne connaît plus doit ramener à l'accueil, jamais faire
     * échouer le démarrage.
     */
    @Test
    fun anUnknownExtraOpensTheDefaultDestinationRatherThanFailing() {
        assertThat(deepLinkDestinationFor("une-destination-disparue")).isNull()
    }
}
