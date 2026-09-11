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
    fun activationRequiresScheduleAlarmAndApplyBlocking() {
        assertThat(PhaseCompletion.requiredKindsFor(SessionEventKindDto.ACTIVATION_REQUESTED))
            .containsExactly(SessionEffectKindDto.SCHEDULE_ALARM, SessionEffectKindDto.APPLY_BLOCKING)
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
