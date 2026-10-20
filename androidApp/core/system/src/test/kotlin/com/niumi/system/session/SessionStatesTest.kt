package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.fakes.SessionDtoFixtures
import org.junit.Test

/**
 * Règle clair/sombre de l'app (`design-plan/VALEURS-DE-DESIGN.md` §1, étape 33) : sombre du
 * coucher jusqu'au scan, clair sinon, jamais d'après le réglage du téléphone. Le coucher, c'est
 * l'instant où le blocage est appliqué (`blockingAppliedAtEpochMillis`), pas l'activation.
 */
class SessionStatesTest {
    private fun inState(state: SessionStateDto): SessionSnapshotDto = SessionDtoFixtures.snapshotInState(state)

    @Test
    fun noSessionIsLight() {
        val none: SessionSnapshotDto? = null

        assertThat(none.isSessionDark()).isFalse()
    }

    /** Armée, blocage différé pas encore appliqué : l'accueil « Réveil prévu » est clair. */
    @Test
    fun anArmedSessionWaitingForBedtimeIsLight() {
        val pending = SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = null)

        assertThat(pending.isSessionDark()).isFalse()
    }

    /** Armée, blocage appliqué (coucher passé, ou blocage immédiat) : la session est en cours. */
    @Test
    fun anArmedSessionWhoseBlockingIsAppliedIsDark() {
        val applied = SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = 1_500L)

        assertThat(applied.isSessionDark()).isTrue()
    }

    @Test
    fun ringingAndWaitingForTheScanAreDark() {
        listOf(SessionStateDto.RINGING, SessionStateDto.AWAITING_NFC, SessionStateDto.TRIGGERED_AWAITING_NFC)
            .forEach { state -> assertWithMessage(state.name).that(inState(state).isSessionDark()).isTrue() }
    }

    /**
     * La libération est en cours : le passage au clair se fait à la fin de la session (VALEURS §1),
     * pas au scan. Les deux cibles connues aujourd'hui (`COMPLETED`, `CANCELLED`) restent sombres ;
     * l'étape 43 ajoute `DISARMED`, clair.
     */
    @Test
    fun releasingTowardsCompletedOrCancelledIsDark() {
        ReleaseTargetDto.entries.forEach { target ->
            val releasing = inState(SessionStateDto.RELEASING).copy(releaseTarget = target)
            assertWithMessage(target.name).that(releasing.isSessionDark()).isTrue()
        }
    }

    @Test
    fun finalStatesAreLight() {
        SESSION_FINAL_STATES.forEach { state ->
            assertWithMessage(state.name).that(inState(state).isSessionDark()).isFalse()
        }
    }

    /**
     * Une seule règle pour le thème et le service de garde du blocage (SPEC_ANDROID §12.5) : le
     * thème est sombre exactement quand le blocage est en vigueur, sur tous les états, blocage
     * différé appliqué ou non.
     */
    @Test
    fun theDarkThemeFollowsExactlyTheBlockingInForce() {
        val snapshots =
            SessionStateDto.entries.map(::inState) +
                SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = null) +
                SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = 1_500L)
        val none: SessionSnapshotDto? = null

        assertThat(none.isBlockingInForce()).isFalse()
        snapshots.forEach { snapshot ->
            assertWithMessage(
                snapshot.state.name,
            ).that(snapshot.isSessionDark()).isEqualTo(snapshot.isBlockingInForce())
        }
    }

    /** `PREPARING` n'a encore rien bloqué : l'app est claire. */
    @Test
    fun aSessionStillPreparingIsLight() {
        assertThat(inState(SessionStateDto.PREPARING).isSessionDark()).isFalse()
    }

    /**
     * Garde d'exhaustivité : un état ajouté à `SessionStateDto` sans être classé ici fait échouer
     * ce test plutôt que de choisir un thème au hasard.
     */
    @Test
    fun everyStateIsClassified() {
        val dark =
            setOf(
                SessionStateDto.RINGING,
                SessionStateDto.AWAITING_NFC,
                SessionStateDto.TRIGGERED_AWAITING_NFC,
                SessionStateDto.RELEASING,
            )
        val light = SESSION_FINAL_STATES + SessionStateDto.PREPARING
        val dependsOnBlocking = setOf(SessionStateDto.ARMED)

        assertThat(dark + light + dependsOnBlocking).containsExactlyElementsIn(SessionStateDto.entries)
        dark.forEach { assertWithMessage(it.name).that(inState(it).isSessionDark()).isTrue() }
        light.forEach { assertWithMessage(it.name).that(inState(it).isSessionDark()).isFalse() }
    }
}
