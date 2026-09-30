package com.niumi.system.session.executors

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.database.BlockedPackage
import com.niumi.database.EffectStatus
import com.niumi.database.PendingEffect
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult
import com.niumi.system.session.fakes.FakeBlockingController
import com.niumi.system.session.fakes.FakeTechnicalEventLog
import com.niumi.system.session.fakes.SessionDtoFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * [ApplyBlockingExecutor] n'avait aucun test direct avant l'étape 25 : il n'était exercé que par le
 * harnais du coordinateur, dont le contrôleur de blocage répond toujours `Success`. Or sur appareil,
 * quand le service d'accessibilité tourne, la publication du snapshot rafraîchit la projection
 * **avant** l'exécuteur, qui reçoit `AlreadySatisfied` : c'est ce chemin qui perdait
 * `BLOCKING_STARTED` (mesuré le 2026-09-24, trois sessions différées sur trois).
 */
class ApplyBlockingExecutorTest {
    private val blockingController = FakeBlockingController()
    private val technicalEventLog = FakeTechnicalEventLog()
    private val executor = ApplyBlockingExecutor(blockingController, technicalEventLog)

    private val deferredApplied =
        SessionDtoFixtures.deferredArmedSnapshot(
            blockingAppliedAtEpochMillis = SessionDtoFixtures.BLOCKING_STARTS_AT_EPOCH_MILLIS,
        )
    private val immediate = SessionDtoFixtures.deferredArmedSnapshot(startsAtEpochMillis = null)
    private val extras =
        SessionDtoFixtures.extras(
            blockedPackages =
                listOf(
                    BlockedPackage("com.example.app", "Exemple"),
                    BlockedPackage("com.example.other", "Autre"),
                ),
        )

    /** Le défaut vu sur appareil : la projection était déjà active quand l'effet s'est exécuté. */
    @Test
    fun aDeferredBlockingAlreadyActiveInTheProjectionIsStillJournaled() =
        runTest {
            blockingController.applyResult = OperationResult.AlreadySatisfied

            val outcome = executor.execute(applyBlocking(), deferredApplied, extras)

            assertThat(outcome.result).isEqualTo(OperationResult.AlreadySatisfied)
            assertThat(technicalEventLog.logged)
                .containsExactly(
                    TechnicalEventType.BLOCKING_STARTED,
                    TechnicalEventType.BLOCK_APPLIED,
                    TechnicalEventType.BLOCK_APPLIED,
                ).inOrder()
        }

    @Test
    fun aDeferredBlockingAppliedByTheExecutorIsJournaled() =
        runTest {
            executor.execute(applyBlocking(), deferredApplied, extras)

            assertThat(technicalEventLog.logged)
                .containsExactly(
                    TechnicalEventType.BLOCKING_STARTED,
                    TechnicalEventType.BLOCK_APPLIED,
                    TechnicalEventType.BLOCK_APPLIED,
                ).inOrder()
        }

    /** `BLOCKING_STARTED` dit « commencé à l'heure choisie » : jamais pour un blocage immédiat. */
    @Test
    fun anImmediateBlockingNeverJournalsBlockingStarted() =
        runTest {
            listOf(OperationResult.Success, OperationResult.AlreadySatisfied).forEach { result ->
                technicalEventLog.logged.clear()
                blockingController.applyResult = result

                executor.execute(applyBlocking(), immediate, extras)

                assertThat(technicalEventLog.logged)
                    .containsExactly(TechnicalEventType.BLOCK_APPLIED, TechnicalEventType.BLOCK_APPLIED)
            }
        }

    @Test
    fun aFailedBlockingJournalsNothingAndReportsTheFailure() =
        runTest {
            val failure = OperationResult.Failure("BLOCKING_FAILED")
            blockingController.applyResult = failure

            val outcome = executor.execute(applyBlocking(), deferredApplied, extras)

            assertThat(outcome.result).isEqualTo(failure)
            assertThat(technicalEventLog.logged).isEmpty()
        }

    private fun applyBlocking() =
        PendingEffect(
            effectId = "${SessionDtoFixtures.SESSION_ID}:3:APPLY_BLOCKING:1",
            sessionId = SessionDtoFixtures.SESSION_ID,
            revision = 3,
            kind = SessionEffectKindDto.APPLY_BLOCKING,
            ordinal = 1,
            payloadJson = null,
            status = EffectStatus.PENDING,
            lastError = null,
        )
}
