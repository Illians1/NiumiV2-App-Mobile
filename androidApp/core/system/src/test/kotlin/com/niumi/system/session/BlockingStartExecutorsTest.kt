package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.database.EffectStatus
import com.niumi.database.PendingEffect
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult
import com.niumi.system.session.executors.CancelBlockingStartExecutor
import com.niumi.system.session.executors.ScheduleBlockingStartExecutor
import com.niumi.system.session.fakes.FakeBlockingStartScheduler
import com.niumi.system.session.fakes.FakeTechnicalEventLog
import com.niumi.system.session.fakes.SessionDtoFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Les deux exécuteurs de l'alarme de début (SPEC_CORE_KMP §6 ; SPEC_ANDROID §12.4, §17, §18).
 * `SCHEDULE_BLOCKING_START` est requis pour `ACTIVATION_SUCCEEDED` d'une session différée ;
 * `CANCEL_BLOCKING_START` est best-effort.
 */
class BlockingStartExecutorsTest {
    private val scheduler = FakeBlockingStartScheduler()
    private val technicalEventLog = FakeTechnicalEventLog()
    private val extras = SessionDtoFixtures.extras()

    private fun effect(kind: SessionEffectKindDto) =
        PendingEffect(
            effectId = "${SessionDtoFixtures.SESSION_ID}:2:$kind:0",
            sessionId = SessionDtoFixtures.SESSION_ID,
            revision = 2,
            kind = kind,
            ordinal = 0,
            payloadJson = null,
            status = EffectStatus.PENDING,
            lastError = null,
        )

    /** L'instant programmé est l'instant **contractuel** du snapshot, jamais un calcul local (§8.3). */
    @Test
    fun schedulingUsesTheContractualStartInstantAndLogsIt() =
        runTest {
            val snapshot = SessionDtoFixtures.deferredArmedSnapshot()

            val outcome =
                ScheduleBlockingStartExecutor(scheduler, technicalEventLog)
                    .execute(effect(SessionEffectKindDto.SCHEDULE_BLOCKING_START), snapshot, extras)

            assertThat(outcome.result).isEqualTo(OperationResult.Success)
            assertThat(scheduler.lastScheduledAtEpochMillis)
                .isEqualTo(SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS)
            assertThat(scheduler.isScheduled(SessionDtoFixtures.SESSION_ID)).isTrue()
            assertThat(technicalEventLog.entries)
                .containsExactly(TechnicalEventType.BLOCKING_SCHEDULED to SessionDtoFixtures.SESSION_ID)
        }

    /** Même patron que `ScheduleAlarmExecutor` : rien n'est journalisé quand rien n'a été programmé. */
    @Test
    fun aFailedSchedulingIsReportedAndNotLogged() =
        runTest {
            scheduler.scheduleResult = OperationResult.Failure("ANDROID_EXACT_ALARM_DENIED")

            val outcome =
                ScheduleBlockingStartExecutor(scheduler, technicalEventLog)
                    .execute(
                        effect(SessionEffectKindDto.SCHEDULE_BLOCKING_START),
                        SessionDtoFixtures.deferredArmedSnapshot(),
                        extras,
                    )

            assertThat((outcome.result as OperationResult.Failure).code).isEqualTo("ANDROID_EXACT_ALARM_DENIED")
            assertThat(technicalEventLog.entries).isEmpty()
        }

    /**
     * Cas impossible par construction — le moteur ne produit cet effet que pour un blocage différé —
     * mais écrit tout de même : `!!` est interdit par detekt, et un échec typé vaut mieux qu'une
     * exception qui ferait tomber la phase d'activation sans code exploitable.
     */
    @Test
    fun anImmediateScheduleFailsWithADedicatedCodeRatherThanThrowing() =
        runTest {
            val immediate = SessionDtoFixtures.deferredArmedSnapshot(startsAtEpochMillis = null)

            val outcome =
                ScheduleBlockingStartExecutor(scheduler, technicalEventLog)
                    .execute(effect(SessionEffectKindDto.SCHEDULE_BLOCKING_START), immediate, extras)

            assertThat((outcome.result as OperationResult.Failure).code).isEqualTo("BLOCKING_START_WITHOUT_SCHEDULE")
            assertThat(scheduler.scheduleCount).isEqualTo(0)
        }

    @Test
    fun cancellingRemovesTheScheduledStart() =
        runTest {
            val snapshot = SessionDtoFixtures.deferredArmedSnapshot()
            scheduler.schedule(
                snapshot.sessionId,
                snapshot.revision,
                SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS,
            )

            val outcome =
                CancelBlockingStartExecutor(scheduler)
                    .execute(effect(SessionEffectKindDto.CANCEL_BLOCKING_START), snapshot, extras)

            assertThat(outcome.result).isEqualTo(OperationResult.Success)
            assertThat(scheduler.isScheduled(snapshot.sessionId)).isFalse()
        }

    /**
     * Annulation idempotente (§12.4) : sans alarme posée, l'effet est `AlreadySatisfied` et non un
     * échec — un rejeu d'outbox ne doit jamais faire échouer une libération pour cette raison.
     */
    @Test
    fun cancellingWithoutAScheduledStartIsAlreadySatisfied() =
        runTest {
            scheduler.cancelResult = OperationResult.AlreadySatisfied

            val outcome =
                CancelBlockingStartExecutor(scheduler)
                    .execute(
                        effect(SessionEffectKindDto.CANCEL_BLOCKING_START),
                        SessionDtoFixtures.deferredArmedSnapshot(),
                        extras,
                    )

            assertThat(outcome.result).isEqualTo(OperationResult.AlreadySatisfied)
        }
}
