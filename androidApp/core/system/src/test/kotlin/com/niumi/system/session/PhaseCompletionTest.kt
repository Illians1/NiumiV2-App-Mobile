package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.database.EffectStatus
import com.niumi.database.PendingEffect
import com.niumi.system.common.OperationResult
import com.niumi.system.session.fakes.SessionDtoFixtures
import org.junit.Test

/**
 * Effets requis par phase (SPEC_CORE_KMP §6, dernier alinéa). `STOP_RINGING` requis pour
 * `RELEASE_SUCCEEDED` — décision validée le 2026-09-10, corrige SPEC_ANDROID §11.3.
 */
class PhaseCompletionTest {
    private fun effect(kind: SessionEffectKindDto) =
        PendingEffect(
            effectId = "${SessionDtoFixtures.SESSION_ID}:1:$kind:0",
            sessionId = SessionDtoFixtures.SESSION_ID,
            revision = 1,
            kind = kind,
            ordinal = 0,
            payloadJson = null,
            status = EffectStatus.PENDING,
            lastError = null,
        )

    private fun outcomes(vararg pairs: Pair<SessionEffectKindDto, OperationResult>) =
        EffectOutcomes(pairs.map { (kind, result) -> EffectOutcome(effect(kind), result) })

    @Test
    fun activationRequiresTheAlarmAndWhicheverBlockingEffectTheDecisionProduced() {
        assertThat(PhaseCompletion.requiredKindsFor(SessionEventKindDto.ACTIVATION_REQUESTED))
            .containsExactly(
                SessionEffectKindDto.SCHEDULE_ALARM,
                SessionEffectKindDto.APPLY_BLOCKING,
                SessionEffectKindDto.SCHEDULE_BLOCKING_START,
            )
    }

    @Test
    fun releaseRequiresCancelAlarmStopRingingAndRemoveBlocking() {
        assertThat(PhaseCompletion.requiredKindsFor(SessionEventKindDto.VALID_NFC_SCANNED))
            .containsExactly(
                SessionEffectKindDto.CANCEL_ALARM,
                SessionEffectKindDto.STOP_RINGING,
                SessionEffectKindDto.REMOVE_BLOCKING,
            )
    }

    @Test
    fun otherEventKindsHaveNoRequiredEffects() {
        assertThat(PhaseCompletion.requiredKindsFor(SessionEventKindDto.INCIDENT_REPORTED)).isEmpty()
    }

    @Test
    fun activationIsSatisfiedWhenBothRequiredEffectsSucceed() {
        val result =
            outcomes(
                SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT to OperationResult.Success,
                SessionEffectKindDto.SCHEDULE_ALARM to OperationResult.Success,
                SessionEffectKindDto.APPLY_BLOCKING to OperationResult.AlreadySatisfied,
            )

        assertThat(PhaseCompletion.isSatisfied(SessionEventKindDto.ACTIVATION_REQUESTED, result)).isTrue()
    }

    @Test
    fun activationIsNotSatisfiedWhenScheduleAlarmFails() {
        val result =
            outcomes(
                SessionEffectKindDto.SCHEDULE_ALARM to OperationResult.Failure("ANDROID_ALARM_SCHEDULE_FAILED"),
                SessionEffectKindDto.APPLY_BLOCKING to OperationResult.Success,
            )

        assertThat(PhaseCompletion.isSatisfied(SessionEventKindDto.ACTIVATION_REQUESTED, result)).isFalse()
        assertThat(PhaseCompletion.firstFailureCode(SessionEventKindDto.ACTIVATION_REQUESTED, result))
            .isEqualTo("ANDROID_ALARM_SCHEDULE_FAILED")
    }

    /**
     * Non-régression du contrat 1.2 : une activation **immédiate** ne produit jamais
     * `SCHEDULE_BLOCKING_START`, et l'ajouter à la table des effets requis ne doit pas la bloquer.
     * `EffectOutcomes.succeeded` est vacuement vrai pour un kind non produit — c'est ce qui rend
     * l'intersection inutile (Lot 6).
     */
    @Test
    fun anImmediateActivationIsSatisfiedWithoutAnyScheduledBlockingStart() {
        val result =
            outcomes(
                SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT to OperationResult.Success,
                SessionEffectKindDto.SCHEDULE_ALARM to OperationResult.Success,
                SessionEffectKindDto.APPLY_BLOCKING to OperationResult.Success,
            )

        assertThat(PhaseCompletion.isSatisfied(SessionEventKindDto.ACTIVATION_REQUESTED, result)).isTrue()
    }

    /** Symétriquement, une activation différée ne produit pas `APPLY_BLOCKING` et reste satisfaite. */
    @Test
    fun aDeferredActivationIsSatisfiedWithoutAnyAppliedBlocking() {
        val result =
            outcomes(
                SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT to OperationResult.Success,
                SessionEffectKindDto.SCHEDULE_ALARM to OperationResult.Success,
                SessionEffectKindDto.SCHEDULE_BLOCKING_START to OperationResult.Success,
            )

        assertThat(PhaseCompletion.isSatisfied(SessionEventKindDto.ACTIVATION_REQUESTED, result)).isTrue()
    }

    /**
     * SPEC_ANDROID §18 : « un échec de `SCHEDULE_BLOCKING_START` pendant `PREPARING` fait échouer
     * l'activation comme un échec de `SCHEDULE_ALARM` ». Une session armée dont le blocage ne
     * commencerait jamais serait un engagement que Niumi ne tiendrait pas.
     */
    @Test
    fun aDeferredActivationFailsWhenTheBlockingStartCannotBeScheduled() {
        val result =
            outcomes(
                SessionEffectKindDto.SCHEDULE_ALARM to OperationResult.Success,
                SessionEffectKindDto.SCHEDULE_BLOCKING_START to OperationResult.Failure("ANDROID_EXACT_ALARM_DENIED"),
            )

        assertThat(PhaseCompletion.isSatisfied(SessionEventKindDto.ACTIVATION_REQUESTED, result)).isFalse()
        assertThat(PhaseCompletion.firstFailureCode(SessionEventKindDto.ACTIVATION_REQUESTED, result))
            .isEqualTo("ANDROID_EXACT_ALARM_DENIED")
    }

    /** `CANCEL_BLOCKING_START` est best-effort : son échec ne retient jamais `RELEASING` (§6). */
    @Test
    fun releaseIsSatisfiedEvenWhenCancellingTheBlockingStartFails() {
        val result =
            outcomes(
                SessionEffectKindDto.CANCEL_ALARM to OperationResult.Success,
                SessionEffectKindDto.CANCEL_BLOCKING_START to OperationResult.Failure("ANDROID_CANCEL_FAILED"),
                SessionEffectKindDto.STOP_RINGING to OperationResult.Success,
                SessionEffectKindDto.REMOVE_BLOCKING to OperationResult.Success,
            )

        assertThat(PhaseCompletion.isSatisfied(SessionEventKindDto.VALID_NFC_SCANNED, result)).isTrue()
    }

    @Test
    fun releaseIsNotSatisfiedWhenStopRingingAloneFails() {
        val result =
            outcomes(
                SessionEffectKindDto.CANCEL_ALARM to OperationResult.Success,
                SessionEffectKindDto.STOP_RINGING to OperationResult.Failure("ANDROID_STOP_RINGING_FAILED"),
                SessionEffectKindDto.REMOVE_BLOCKING to OperationResult.Success,
            )

        assertThat(PhaseCompletion.isSatisfied(SessionEventKindDto.VALID_NFC_SCANNED, result)).isFalse()
    }
}
