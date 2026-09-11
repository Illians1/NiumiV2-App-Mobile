package com.niumi.feature.setup

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import org.junit.Test

/**
 * SPEC_CORE_KMP §2 point 11 et §10, SPEC_ANDROID §11.1 et §12.1 : ni l'association du boîtier ni
 * le sélecteur d'applications ne sont accessibles tant qu'une session est en cours. Le credential
 * et la sélection figés à l'activation ne doivent jamais pouvoir changer sous la session.
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
