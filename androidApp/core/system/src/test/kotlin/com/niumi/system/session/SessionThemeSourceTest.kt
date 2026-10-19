package com.niumi.system.session

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.StoredDecision
import com.niumi.system.common.OperationResult
import com.niumi.system.session.fakes.SessionDtoFixtures
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Passerelle qui ne sait que charger : le thème ne doit jamais rien écrire. */
private class LoadOnlyGateway(
    var result: LoadResult = LoadResult.Absent,
) : SessionPersistenceGateway {
    var loads = 0
    var failure: Exception? = null
    var neverAnswers = false

    override suspend fun load(): LoadResult {
        loads++
        failure?.let { throw it }
        if (neverAnswers) awaitCancellation()
        return result
    }

    override suspend fun commit(decision: StoredDecision) = error("COMMIT_FROM_THEME")

    override suspend fun receipt(eventId: String): EventReceipt? = error("RECEIPT_FROM_THEME")

    override suspend fun pendingEffects(sessionId: String): List<PendingEffect> = error("EFFECTS_FROM_THEME")

    override suspend fun markEffect(
        effectId: String,
        status: EffectStatus,
        error: String?,
    ) = error("MARK_EFFECT_FROM_THEME")

    override suspend fun clearActive(sessionId: String) = error("CLEAR_ACTIVE_FROM_THEME")

    override suspend fun recordIncident(
        sessionId: String,
        incident: SessionIncidentDto,
    ): OperationResult = error("RECORD_INCIDENT_FROM_THEME")

    override suspend fun updateAlarmSound(
        sessionId: String,
        ringtoneKey: String,
        volumeRampSeconds: Int?,
    ): AlarmSoundUpdateResult = error("UPDATE_ALARM_SOUND_FROM_THEME")
}

/**
 * Thème au démarrage (plan de refonte, point de vigilance 9) : dans un processus neuf, le publisher
 * vaut `null` avant toute lecture. Le thème ne doit pas en conclure « aucune session, donc clair » :
 * il lit la persistance une fois, puis suit le publisher.
 */
class SessionThemeSourceTest {
    private val gateway = LoadOnlyGateway()
    private val publisher = SessionSnapshotPublisher()
    private val source = SessionThemeSource(gateway, publisher)

    private fun present(state: SessionStateDto) =
        LoadResult.Present(SessionDtoFixtures.snapshotInState(state), SessionDtoFixtures.extras(), emptyList())

    @Test
    fun aFreshProcessWithoutSessionIsLight() =
        runTest {
            gateway.result = LoadResult.Absent

            assertThat(source.dark.first()).isFalse()
        }

    @Test
    fun aFreshProcessReadsThePersistedSessionBeforeChoosing() =
        runTest {
            gateway.result =
                LoadResult.Present(
                    SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = 1_500L),
                    SessionDtoFixtures.extras(),
                    emptyList(),
                )

            assertThat(source.dark.first()).isTrue()
            assertThat(gateway.loads).isEqualTo(1)
        }

    @Test
    fun anAlreadyPublishedSnapshotIsUsedWithoutReadingThePersistence() =
        runTest {
            gateway.result = LoadResult.Absent
            publisher.publish(SessionDtoFixtures.snapshotInState(SessionStateDto.RINGING))

            assertThat(source.dark.first()).isTrue()
            assertThat(gateway.loads).isEqualTo(0)
        }

    /** SPEC_CORE_KMP §13 : illisible n'est pas « aucune session », mais aucun état sombre n'est prouvé. */
    @Test
    fun anUnreadablePersistenceIsLight() =
        runTest {
            gateway.result = LoadResult.Unreadable("ROOM_CORRUPTED")

            assertThat(source.dark.first()).isFalse()
        }

    /**
     * Une exception que la passerelle ne convertit pas (migration Room, état inattendu) ne doit
     * pas tuer l'app avant son premier écran : comme une persistance illisible, elle se lit clair.
     */
    @Test
    fun anUnexpectedPersistenceFailureIsLightInsteadOfCrashing() =
        runTest {
            gateway.failure = IllegalStateException("ROOM_MIGRATION_FAILED")

            assertThat(source.dark.first()).isFalse()
        }

    /**
     * Décision de Mehdi du 2026-10-19 : l'écran de démarrage attend la session au plus trois
     * secondes, puis l'app s'ouvre en clair. Garde-fou jamais atteint en usage normal.
     */
    @Test
    fun aPersistenceThatNeverAnswersOpensLightAfterThreeSeconds() =
        runTest {
            gateway.neverAnswers = true
            var emitted: Boolean? = null
            val collector = launch { emitted = source.dark.first() }

            advanceTimeBy(2_999)
            assertThat(emitted).isNull()

            advanceTimeBy(2)
            assertThat(emitted).isFalse()
            collector.join()
        }

    @Test
    fun theThemeFollowsTheSnapshotsPublishedAfterwards() =
        runTest {
            gateway.result =
                present(SessionStateDto.ARMED).copy(
                    snapshot = SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = null),
                )

            source.dark.test {
                assertThat(awaitItem()).isFalse()
                publisher.publish(SessionDtoFixtures.deferredArmedSnapshot(blockingAppliedAtEpochMillis = 1_500L))
                assertThat(awaitItem()).isTrue()
                publisher.publish(SessionDtoFixtures.snapshotInState(SessionStateDto.RINGING))
                publisher.publish(SessionDtoFixtures.snapshotInState(SessionStateDto.COMPLETED))
                assertThat(awaitItem()).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(gateway.loads).isEqualTo(1)
        }
}
