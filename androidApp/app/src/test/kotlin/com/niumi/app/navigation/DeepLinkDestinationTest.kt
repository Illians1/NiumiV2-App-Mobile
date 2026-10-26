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

    /**
     * Écran 11, atteint depuis `AlarmActivity` (§15, étape 46) : la route porte la session terminée,
     * différente chaque matin, pour que la navigation reparte même si `MainActivity` est restée
     * vivante depuis la veille.
     */
    @Test
    fun aCompletedSessionOpensTheSuccessScreenOfThatSession() {
        assertThat(deepLinkDestinationFor(NiumiDeepLink.DESTINATION_SESSION_COMPLETED, SESSION_ID))
            .isEqualTo(NiumiRoute.Success(SESSION_ID))
    }

    /** Sans identifiant, rien à relire : l'accueil. */
    @Test
    fun aCompletedSessionWithoutItsIdentifierOpensTheDefaultDestination() {
        assertThat(deepLinkDestinationFor(NiumiDeepLink.DESTINATION_SESSION_COMPLETED, null)).isNull()
    }

    /** L'extra « session annulée » d'avant l'étape 46, resté dans un `Intent` : l'accueil. */
    @Test
    fun theFormerCancelledExtraOpensTheDefaultDestination() {
        assertThat(deepLinkDestinationFor("session_cancelled")).isNull()
    }

    /**
     * Relecture de l'étape 46 : une activité recréée (changement de configuration, ou mort du
     * processus puis réouverture depuis les récents) restaure sa pile de navigation ; relire
     * l'`Intent` d'origine rouvrirait, des heures plus tard, l'écran 11 d'un réveil déjà vu.
     */
    @Test
    fun theLaunchIntentIsReadOnlyOnAFreshStartNotOnARecreation() {
        assertThat(readsLaunchDeepLink(restoredState = false, launchedFromHistory = false)).isTrue()
        assertThat(readsLaunchDeepLink(restoredState = true, launchedFromHistory = false)).isFalse()
        assertThat(readsLaunchDeepLink(restoredState = false, launchedFromHistory = true)).isFalse()
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

    private companion object {
        const val SESSION_ID = "11111111-1111-1111-1111-111111111111"
    }
}
