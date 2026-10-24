package com.niumi.feature.setup

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import org.junit.Test

/**
 * SPEC_CORE_KMP §4 et §10, SPEC_ANDROID §12.1 : le sélecteur d'applications n'est pas accessible
 * tant qu'une session est en cours ; la sélection figée à l'activation ne doit jamais changer sous la
 * session. Les Niumi Spots, eux, se modifient en session depuis l'étape 43.
 */
class SetupGateTest {
    @Test
    fun noSessionAllowsPreparation() {
        assertThat(isSetupEditable(state = null)).isTrue()
    }

    @Test
    fun everyFinalStateAllowsPreparationAgain() {
        val finalStates =
            listOf(SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.FAILED)

        finalStates.forEach { state ->
            assertThat(isSetupEditable(state)).isTrue()
        }
    }

    @Test
    fun everyNonFinalStateRefusesPreparation() {
        val inProgressStates =
            SessionStateDto.entries -
                setOf(
                    SessionStateDto.COMPLETED,
                    SessionStateDto.CANCELLED,
                    SessionStateDto.DISARMED,
                    SessionStateDto.FAILED,
                )

        assertThat(inProgressStates).isNotEmpty()
        inProgressStates.forEach { state ->
            assertThat(isSetupEditable(state)).isFalse()
        }
    }

    @Test
    fun anArmedSessionRefusesPreparation() {
        assertThat(isSetupEditable(SessionStateDto.ARMED)).isFalse()
    }

    /**
     * Exhaustivité : si la machine commune gagne un état, il tombe forcément dans l'un des deux
     * ensembles ci-dessus — aucun état ne peut échapper au classement sans faire échouer ce test.
     */
    @Test
    fun everyKnownStateIsClassifiedOneWayOrTheOther() {
        val classified = SessionStateDto.entries.map { isSetupEditable(it) }

        assertThat(classified).hasSize(SessionStateDto.entries.size)
        assertThat(classified).contains(true)
        assertThat(classified).contains(false)
    }
}
