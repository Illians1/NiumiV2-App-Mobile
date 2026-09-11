package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.StoredDecision
import com.niumi.system.common.OperationResult
import com.niumi.system.session.fakes.CallJournal
import com.niumi.system.session.fakes.InMemoryPersistenceGateway
import com.niumi.system.session.fakes.SessionDtoFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** [EffectDispatcher] : ordre d'exécution, marquage de l'outbox, collecte des incidents. */
class EffectDispatcherTest {
    private fun effect(
        kind: SessionEffectKindDto,
        ordinal: Int,
    ) = PendingEffect(
        effectId = "${SessionDtoFixtures.SESSION_ID}:1:$kind:$ordinal",
        sessionId = SessionDtoFixtures.SESSION_ID,
        revision = 1,
        kind = kind,
        ordinal = ordinal,
        payloadJson = null,
        status = EffectStatus.PENDING,
        lastError = null,
    )

    /** L'outbox n'existe que pour les effets déjà `commit()`és : `markEffect` sur un `effectId`
     * inconnu ne fait rien (comportement volontaire, voir `RoomSessionStoreEffectsTest`). */
    private suspend fun seed(
        gateway: InMemoryPersistenceGateway,
        effects: List<PendingEffect>,
    ) {
        gateway.commit(
            StoredDecision(
                snapshot = SessionDtoFixtures.releasingSnapshot(),
                receipt = EventReceipt("seed", SessionDtoFixtures.SESSION_ID, "hash", 1, 900L),
                effects = effects,
                androidExtras = SessionDtoFixtures.extras(),
            ),
        )
    }

    @Test
    fun executesEffectsInTheOrderReceivedAndMarksEachOutcome() =
        runTest {
            val journal = CallJournal()
            val gateway = InMemoryPersistenceGateway(journal)
            val executionOrder = mutableListOf<SessionEffectKindDto>()
            val executors =
                mapOf<SessionEffectKindDto, EffectExecutor>(
                    SessionEffectKindDto.SCHEDULE_ALARM to
                        EffectExecutor { _, _, _ ->
                            executionOrder += SessionEffectKindDto.SCHEDULE_ALARM
                            ExecutionOutcome(OperationResult.Success)
                        },
                    SessionEffectKindDto.APPLY_BLOCKING to
                        EffectExecutor { _, _, _ ->
                            executionOrder += SessionEffectKindDto.APPLY_BLOCKING
                            ExecutionOutcome(OperationResult.AlreadySatisfied)
                        },
                )
            val dispatcher = EffectDispatcher(executors, gateway)
            val effects =
                listOf(effect(SessionEffectKindDto.SCHEDULE_ALARM, 0), effect(SessionEffectKindDto.APPLY_BLOCKING, 1))
            seed(gateway, effects)

            val result =
                dispatcher.execute(effects, SessionDtoFixtures.releasingSnapshot(), SessionDtoFixtures.extras())

            assertThat(
                executionOrder,
            ).containsExactly(SessionEffectKindDto.SCHEDULE_ALARM, SessionEffectKindDto.APPLY_BLOCKING)
                .inOrder()
            assertThat(result.outcomes.succeeded(SessionEffectKindDto.SCHEDULE_ALARM)).isTrue()
            assertThat(result.outcomes.succeeded(SessionEffectKindDto.APPLY_BLOCKING)).isTrue()
            // Les deux effets ont réussi (Success et AlreadySatisfied comptent tous deux comme
            // succès) : aucun ne reste PENDING ni FAILED, donc `pendingEffects` (rejouables) est vide.
            assertThat(gateway.pendingEffects(SessionDtoFixtures.SESSION_ID)).isEmpty()
        }

    @Test
    fun failureIsMarkedFailedInTheOutboxWithItsCode() =
        runTest {
            val journal = CallJournal()
            val gateway = InMemoryPersistenceGateway(journal)
            val executors =
                mapOf<SessionEffectKindDto, EffectExecutor>(
                    SessionEffectKindDto.SCHEDULE_ALARM to
                        EffectExecutor {
                            _,
                            _,
                            _,
                            ->
                            ExecutionOutcome(OperationResult.Failure("ANDROID_ALARM_SCHEDULE_FAILED"))
                        },
                )
            val dispatcher = EffectDispatcher(executors, gateway)
            val effects = listOf(effect(SessionEffectKindDto.SCHEDULE_ALARM, 0))
            seed(gateway, effects)

            dispatcher.execute(effects, SessionDtoFixtures.releasingSnapshot(), SessionDtoFixtures.extras())

            val replayed = gateway.pendingEffects(SessionDtoFixtures.SESSION_ID)
            assertThat(replayed).hasSize(1)
            assertThat(replayed.single().status).isEqualTo(EffectStatus.FAILED)
            assertThat(replayed.single().lastError).isEqualTo("ANDROID_ALARM_SCHEDULE_FAILED")
        }

    @Test
    fun collectsIncidentsReportedByExecutorsAlongsideTheirResult() =
        runTest {
            val journal = CallJournal()
            val gateway = InMemoryPersistenceGateway(journal)
            val incident =
                SessionIncidentDto(
                    code = IncidentCodes.BLOCKING_PERMISSION_REVOKED,
                    severity = IncidentSeverityDto.CRITICAL,
                    occurredAtEpochMillis = 1_000L,
                    platform = PlatformDto.ANDROID,
                )
            val executors =
                mapOf<SessionEffectKindDto, EffectExecutor>(
                    SessionEffectKindDto.REMOVE_BLOCKING to
                        EffectExecutor { _, _, _ -> ExecutionOutcome(OperationResult.AlreadySatisfied, incident) },
                )
            val dispatcher = EffectDispatcher(executors, gateway)
            val effects = listOf(effect(SessionEffectKindDto.REMOVE_BLOCKING, 0))
            seed(gateway, effects)

            val result =
                dispatcher.execute(effects, SessionDtoFixtures.releasingSnapshot(), SessionDtoFixtures.extras())

            assertThat(result.incidents).containsExactly(incident)
        }
}
