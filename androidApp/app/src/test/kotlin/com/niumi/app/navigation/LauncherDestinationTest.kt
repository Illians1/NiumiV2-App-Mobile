package com.niumi.app.navigation

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionStateDto
import org.junit.Test

/**
 * SPEC_ANDROID §10.4 : « Ouvrir Niumi depuis le lanceur pendant une session active mène toujours
 * à l'écran correspondant à l'état ». Pendant la sonnerie, cet écran est l'écran 8 et non
 * l'écran 7 : le Reader Mode NFC n'existe que dans `AlarmActivity` (§11.2), et l'écran 7 n'offre
 * donc aucun moyen de terminer la session.
 */
class LauncherDestinationTest {
    private val scanStates =
        listOf(
            SessionStateDto.RINGING,
            SessionStateDto.AWAITING_NFC,
            SessionStateDto.TRIGGERED_AWAITING_NFC,
            SessionStateDto.RELEASING,
        )

    @Test
    fun everyScanStateLeadsToTheAlarmScreen() {
        scanStates.forEach { state ->
            assertThat(launcherDestinationFor(state, releaseTarget = null, onboardingAcknowledged = true))
                .isEqualTo(LauncherDestination.AlarmScreen)
        }
    }

    /** Avant la sonnerie, l'écran 7 reste le bon écran : rien à y scanner encore. */
    @Test
    fun theStatesBeforeRingingKeepTheActiveSessionScreen() {
        listOf(SessionStateDto.PREPARING, SessionStateDto.ARMED).forEach { state ->
            assertThat(launcherDestinationFor(state, releaseTarget = null, onboardingAcknowledged = true))
                .isEqualTo(LauncherDestination.InApp(NiumiRoute.ActiveSession))
        }
    }

    @Test
    fun withoutASessionTheSetupJourneyIsUnchanged() {
        assertThat(launcherDestinationFor(state = null, releaseTarget = null, onboardingAcknowledged = true))
            .isEqualTo(LauncherDestination.InApp(NiumiRoute.Permissions))
        assertThat(launcherDestinationFor(state = null, releaseTarget = null, onboardingAcknowledged = false))
            .isEqualTo(LauncherDestination.InApp(NiumiRoute.Presentation))
    }

    @Test
    fun everyFinalStateFallsBackToTheSetupJourney() {
        listOf(SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.FAILED)
            .forEach { state ->
                assertThat(launcherDestinationFor(state, releaseTarget = null, onboardingAcknowledged = true))
                    .isEqualTo(LauncherDestination.InApp(NiumiRoute.Permissions))
            }
    }

    /**
     * Étape 43 (contrat 1.5) : un désarmement passe par `RELEASING` vers `DISARMED`, avant le coucher.
     * Rien n'est à scanner : l'écran de réveil n'a pas à s'ouvrir, à la différence de `RELEASING`
     * vers `COMPLETED` ou `CANCELLED`, qui suit un scan ou une sortie de secours.
     */
    @Test
    fun releasingTowardsDisarmedNeverOpensTheAlarmScreen() {
        assertThat(requiresAlarmScreen(SessionStateDto.RELEASING, ReleaseTargetDto.DISARMED)).isFalse()
        assertThat(
            launcherDestinationFor(SessionStateDto.RELEASING, ReleaseTargetDto.DISARMED, onboardingAcknowledged = true),
        ).isEqualTo(
            LauncherDestination.InApp(homeDestinationFor(SessionStateDto.RELEASING, onboardingAcknowledged = true)),
        )
        listOf(ReleaseTargetDto.COMPLETED, ReleaseTargetDto.CANCELLED).forEach { target ->
            assertThat(requiresAlarmScreen(SessionStateDto.RELEASING, target)).isTrue()
        }
    }

    /** Un état ajouté à la machine commune doit être classé explicitement. */
    @Test
    fun everyStateOfTheCommonMachineIsClassified() {
        val classified =
            SessionStateDto.entries.associateWith {
                launcherDestinationFor(it, releaseTarget = null, onboardingAcknowledged = true)
            }

        assertThat(classified).hasSize(SessionStateDto.entries.size)
        assertThat(classified.filterValues { it == LauncherDestination.AlarmScreen }.keys)
            .containsExactlyElementsIn(scanStates)
    }

    /**
     * [requiresAlarmScreen] est la règle globale que `MainActivity` applique à chaque `onResume`,
     * quelle que soit la destination du `NavHost` — la poser sur le seul accueil était le défaut
     * mesuré sur appareil à l'étape 17.
     */
    @Test
    fun requiresAlarmScreenAgreesWithTheLauncherDestination() {
        (SessionStateDto.entries.map { it as SessionStateDto? } + null).forEach { state ->
            val expected =
                launcherDestinationFor(state, releaseTarget = null, onboardingAcknowledged = true) ==
                    LauncherDestination.AlarmScreen
            assertThat(requiresAlarmScreen(state, releaseTarget = null)).isEqualTo(expected)
        }
    }

    /** La règle d'écran 7 n'est pas réécrite ici : elle reste dans [homeDestinationFor]. */
    @Test
    fun theInAppRouteAlwaysMatchesTheHomeDestination() {
        SessionStateDto.entries.forEach { state ->
            val launcher = launcherDestinationFor(state, releaseTarget = null, onboardingAcknowledged = true)
            if (launcher is LauncherDestination.InApp) {
                assertThat(launcher.route).isEqualTo(homeDestinationFor(state, onboardingAcknowledged = true))
            }
        }
    }
}
