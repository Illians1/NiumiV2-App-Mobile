package com.niumi.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * Tri et filtrage de l'outbox (SPEC_CORE_KMP §6.1) : `PENDING` et `FAILED` sont rejoués — écart
 * assumé au plan MVP (`PENDING` seul), voir `ETAPE-09.md` — `SUCCEEDED` et `SATISFIED` sont
 * terminaux. `markEffect` sur un `effectId` inconnu ne lève rien.
 */
@RunWith(AndroidJUnit4::class)
class RoomSessionStoreEffectsTest {
    private lateinit var database: NiumiDatabase
    private lateinit var store: RoomSessionStore
    private val sessionId = "88888888-8888-8888-8888-888888888888"

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        store = RoomSessionStore(Provider { database }, AlwaysUnlockedState)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun seedSessionWithEffects(effects: List<PendingEffect>) {
        store.commitDecision(
            StoredDecision(
                RoomTestFixtures.preparingSnapshot(sessionId),
                RoomTestFixtures.receipt("seed-event", sessionId),
                effects,
                RoomTestFixtures.extras(),
            ),
        )
    }

    @Test
    fun pendingAndFailedEffectsAreReplayedInRevisionThenOrdinalOrder() =
        runTest {
            val effects =
                listOf(
                    RoomTestFixtures.effect(
                        sessionId,
                        revision = 1,
                        ordinal = 1,
                        kind = SessionEffectKindDto.APPLY_BLOCKING,
                    ),
                    RoomTestFixtures.effect(
                        sessionId,
                        revision = 1,
                        ordinal = 0,
                        kind = SessionEffectKindDto.SCHEDULE_ALARM,
                    ),
                )
            seedSessionWithEffects(effects)
            store.markEffect(effects[0].effectId, EffectStatus.FAILED, "boom")

            val replayable = store.pendingEffects(sessionId)

            assertThat(replayable.map { it.effectId })
                .containsExactly(effects[1].effectId, effects[0].effectId)
                .inOrder()
        }

    @Test
    fun succeededAndSatisfiedEffectsAreExcludedFromReplay() =
        runTest {
            val effects =
                listOf(
                    RoomTestFixtures.effect(
                        sessionId,
                        revision = 1,
                        ordinal = 0,
                        kind = SessionEffectKindDto.SCHEDULE_ALARM,
                    ),
                    RoomTestFixtures.effect(
                        sessionId,
                        revision = 1,
                        ordinal = 1,
                        kind = SessionEffectKindDto.APPLY_BLOCKING,
                    ),
                )
            seedSessionWithEffects(effects)
            store.markEffect(effects[0].effectId, EffectStatus.SUCCEEDED, null)
            store.markEffect(effects[1].effectId, EffectStatus.SATISFIED, null)

            assertThat(store.pendingEffects(sessionId)).isEmpty()
        }

    @Test
    fun markEffectUpdatesStatusAndLastError() =
        runTest {
            val effect = RoomTestFixtures.effect(sessionId, revision = 1, ordinal = 0)
            seedSessionWithEffects(listOf(effect))

            store.markEffect(effect.effectId, EffectStatus.FAILED, "ANDROID_ALARM_SCHEDULE_FAILED")

            val reloaded = store.pendingEffects(sessionId).single()
            assertThat(reloaded.status).isEqualTo(EffectStatus.FAILED)
            assertThat(reloaded.lastError).isEqualTo("ANDROID_ALARM_SCHEDULE_FAILED")
        }

    @Test
    fun markEffectOnAnUnknownEffectIdDoesNothingAndNeverThrows() =
        runTest {
            store.markEffect("unknown-effect-id", EffectStatus.FAILED, "irrelevant")
            // Aucune exception : c'est l'assertion elle-même.
        }

    @Test
    fun findReceiptForAnUnknownEventIdReturnsNull() =
        runTest {
            assertThat(store.findReceipt("unknown-event-id")).isNull()
        }
}
