package com.niumi.feature.ringing.ui

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.database.EffectStatus
import com.niumi.database.PendingEffect
import org.junit.Test

/**
 * Progression du nettoyage (SPEC_ANDROID §10.4). Déduite de l'outbox seule : `pendingEffects` ne
 * rend que les effets rejouables, donc un effet absent est terminé (SPEC_CORE_KMP §6, §6.1).
 */
class ReleaseProgressTest {
    private fun effect(
        kind: SessionEffectKindDto,
        status: EffectStatus,
    ) = PendingEffect(
        effectId = "session:2:$kind:0",
        sessionId = "11111111-1111-1111-1111-111111111111",
        revision = 2,
        kind = kind,
        ordinal = 0,
        payloadJson = null,
        status = status,
        lastError = null,
    )

    private val requiredKinds =
        listOf(
            SessionEffectKindDto.CANCEL_ALARM,
            SessionEffectKindDto.STOP_RINGING,
            SessionEffectKindDto.REMOVE_BLOCKING,
        )

    /** Les trois effets **requis** de la libération, et eux seuls : les best-effort ne bloquent
     * jamais la phase, les montrer en attente ferait croire à un blocage inexistant. */
    @Test
    fun onlyTheThreeRequiredEffectsAreShown() {
        val steps = ReleaseProgress.from(emptyList())

        assertThat(steps.map { it.kind }).containsExactlyElementsIn(requiredKinds).inOrder()
        assertThat(steps.map { it.label })
            .containsExactly("Réveil annulé", "Sonnerie arrêtée", "Applications débloquées")
            .inOrder()
    }

    @Test
    fun everyPendingEffectIsStillRunning() {
        val steps = ReleaseProgress.from(requiredKinds.map { effect(it, EffectStatus.PENDING) })

        assertThat(steps.none { it.done }).isTrue()
    }

    @Test
    fun anEffectAbsentFromTheOutboxIsDone() {
        val steps =
            ReleaseProgress.from(
                requiredKinds
                    .filterNot { it == SessionEffectKindDto.CANCEL_ALARM }
                    .map { effect(it, EffectStatus.PENDING) },
            )

        assertThat(steps.single { it.kind == SessionEffectKindDto.CANCEL_ALARM }.done).isTrue()
        assertThat(steps.filter { it.kind != SessionEffectKindDto.CANCEL_ALARM }.none { it.done }).isTrue()
    }

    /** Un effet en échec sera rejoué : l'annoncer terminé serait un faux état de fiabilité (§15). */
    @Test
    fun aFailedEffectIsNotDone() {
        val steps =
            ReleaseProgress.from(listOf(effect(SessionEffectKindDto.REMOVE_BLOCKING, EffectStatus.FAILED)))

        assertThat(steps.single { it.kind == SessionEffectKindDto.REMOVE_BLOCKING }.done).isFalse()
    }

    @Test
    fun anEmptyOutboxMeansEverythingIsDone() {
        val steps = ReleaseProgress.from(emptyList())

        assertThat(steps.all { it.done }).isTrue()
    }
}
