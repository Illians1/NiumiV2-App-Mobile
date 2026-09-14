package com.niumi.database.directboot

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.SessionState
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.database.AlwaysUnlockedState
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.NiumiDatabase
import com.niumi.database.PendingEffect
import com.niumi.database.RoomSessionStore
import com.niumi.database.RoomTestFixtures
import com.niumi.database.StoredDecision
import com.niumi.database.newInMemoryDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * Transaction de fusion Direct Boot → Room (SPEC_ANDROID §9.3, dernier alinéa ; §7.3 pour la garde
 * de révision). Instrumenté parce que la fusion n'existe qu'en SQL : contraintes de clé étrangère,
 * `INSERT OR IGNORE` et absence de CASCADE ne se prouvent pas en JVM.
 */
@RunWith(AndroidJUnit4::class)
class RoomDirectBootMergeTest {
    private lateinit var database: NiumiDatabase
    private lateinit var store: RoomSessionStore
    private lateinit var merge: RoomDirectBootMerge

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        store = RoomSessionStore(Provider { database }, AlwaysUnlockedState)
        merge = RoomDirectBootMerge(AlwaysUnlockedState, Provider { database }) { NOW }
        runBlocking { seedRoomSession(revision = 1) }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun aProjectionAheadOfRoomAdvancesTheSession() =
        runTest {
            val result = merge.merge(projection(revision = 4, state = SessionState.TRIGGERED_AWAITING_NFC))

            assertThat(result).isInstanceOf(DirectBootMergeResult.Merged::class.java)
            assertThat((result as DirectBootMergeResult.Merged).sessionAdvanced).isTrue()
            val stored = requireNotNull(store.activeSession())
            assertThat(stored.snapshot.revision).isEqualTo(4L)
            assertThat(stored.snapshot.state).isEqualTo(SessionState.TRIGGERED_AWAITING_NFC)
        }

    /** §7.3 : « une révision inférieure pour cette même session est refusée ». */
    @Test
    fun aProjectionBehindRoomIsRefusedWithoutWritingAnything() =
        runTest {
            seedRoomSession(revision = 6, eventId = "second-seed-event")

            val result = merge.merge(projection(revision = 2, state = SessionState.ARMED))

            assertThat(result).isEqualTo(DirectBootMergeResult.StaleRevision)
            val stored = requireNotNull(store.activeSession())
            assertThat(stored.snapshot.revision).isEqualTo(6L)
        }

    /** Réécriture idempotente à révision égale : acceptée, sans rien faire avancer. */
    @Test
    fun aProjectionAtTheSameRevisionMergesWithoutAdvancingTheSession() =
        runTest {
            val result = merge.merge(projection(revision = 1, state = SessionState.PREPARING))

            assertThat((result as DirectBootMergeResult.Merged).sessionAdvanced).isFalse()
        }

    @Test
    fun receiptsAreInsertedOnceAndNeverDuplicated() =
        runTest {
            val fromDirectBoot = RoomTestFixtures.receipt("direct-boot-event", SESSION_ID, appliedRevision = 2)

            val first = merge.merge(projection(revision = 2, receipts = listOf(fromDirectBoot)))
            val second = merge.merge(projection(revision = 2, receipts = listOf(fromDirectBoot)))

            assertThat((first as DirectBootMergeResult.Merged).receiptsInserted).isEqualTo(1)
            assertThat((second as DirectBootMergeResult.Merged).receiptsInserted).isEqualTo(0)
            assertThat(database.receiptDao().forSession(SESSION_ID).map { it.eventId })
                .containsExactly("seed-event", "direct-boot-event")
        }

    @Test
    fun anEffectMissingFromRoomIsInserted() =
        runTest {
            val effect = pendingEffect(revision = 2, ordinal = 0, status = EffectStatus.PENDING)

            val result = merge.merge(projection(revision = 2, effects = listOf(effect)))

            assertThat((result as DirectBootMergeResult.Merged).effectsInserted).isEqualTo(1)
            assertThat(store.pendingEffects(SESSION_ID).map { it.effectId }).contains(effect.effectId)
        }

    /**
     * Le cœur de la fusion : un effet exécuté pendant la fenêtre Direct Boot ne doit pas être
     * rejoué au déverrouillage.
     */
    @Test
    fun anEffectSucceededInDirectBootAndPendingInRoomBecomesSucceeded() =
        runTest {
            val pending = pendingEffect(revision = 1, ordinal = 0, status = EffectStatus.PENDING)
            store.commitDecision(
                StoredDecision(
                    snapshot = RoomTestFixtures.preparingSnapshot(SESSION_ID, revision = 1),
                    receipt = RoomTestFixtures.receipt("effect-seed", SESSION_ID),
                    effects = listOf(pending),
                    androidExtras = RoomTestFixtures.extras(),
                ),
            )
            assertThat(store.pendingEffects(SESSION_ID).map { it.effectId }).contains(pending.effectId)

            val result =
                merge.merge(
                    projection(
                        revision = 2,
                        effects = listOf(pending.copy(status = EffectStatus.SUCCEEDED)),
                    ),
                )

            assertThat((result as DirectBootMergeResult.Merged).effectsAdvanced).isEqualTo(1)
            assertThat(store.pendingEffects(SESSION_ID).map { it.effectId }).doesNotContain(pending.effectId)
        }

    /** Jamais dans l'autre sens : une projection en retard ne réarme pas un effet terminé. */
    @Test
    fun anEffectAlreadySucceededInRoomIsNeverMadeReplayableAgain() =
        runTest {
            val effect = pendingEffect(revision = 1, ordinal = 0, status = EffectStatus.PENDING)
            store.commitDecision(
                StoredDecision(
                    snapshot = RoomTestFixtures.preparingSnapshot(SESSION_ID, revision = 1),
                    receipt = RoomTestFixtures.receipt("effect-seed", SESSION_ID),
                    effects = listOf(effect),
                    androidExtras = RoomTestFixtures.extras(),
                ),
            )
            store.markEffect(effect.effectId, EffectStatus.SUCCEEDED, error = null)

            merge.merge(projection(revision = 1, effects = listOf(effect.copy(status = EffectStatus.PENDING))))

            assertThat(store.pendingEffects(SESSION_ID)).isEmpty()
        }

    /**
     * Une session absente de Room ne peut recevoir ni reçu ni effet — ils portent une clé étrangère
     * vers `alarm_session`. Rien n'est écrit, et surtout rien n'est supprimé.
     */
    @Test
    fun anUnknownSessionIsRefusedWithoutTouchingRoom() =
        runTest {
            val result = merge.merge(projection(revision = 3, sessionId = OTHER_SESSION_ID))

            assertThat(result).isEqualTo(DirectBootMergeResult.UnknownSession)
            assertThat(database.receiptDao().forSession(OTHER_SESSION_ID)).isEmpty()
            assertThat(requireNotNull(store.activeSession()).snapshot.sessionId).isEqualTo(SESSION_ID)
        }

    /**
     * `upsert` et non `INSERT OR REPLACE` : le journal de la session, ses reçus et ses effets
     * survivent à une avance de révision. Même régression qu'à l'étape 11
     * (`RoomSessionStoreHistoryTest`).
     */
    @Test
    fun advancingTheSessionKeepsItsReceiptsAndEffects() =
        runTest {
            val effect = pendingEffect(revision = 1, ordinal = 0, status = EffectStatus.PENDING)
            store.commitDecision(
                StoredDecision(
                    snapshot = RoomTestFixtures.preparingSnapshot(SESSION_ID, revision = 1),
                    receipt = RoomTestFixtures.receipt("kept-event", SESSION_ID),
                    effects = listOf(effect),
                    androidExtras = RoomTestFixtures.extras(),
                ),
            )

            merge.merge(projection(revision = 9, state = SessionState.TRIGGERED_AWAITING_NFC))

            assertThat(database.receiptDao().forSession(SESSION_ID).map { it.eventId })
                .containsAtLeast("seed-event", "kept-event")
            assertThat(store.pendingEffects(SESSION_ID).map { it.effectId }).contains(effect.effectId)
        }

    /** §7.2 : les quatre champs figés à l'activation viennent toujours de Room. */
    @Test
    fun frozenActivationFieldsAreNeverOverwrittenByTheProjection() =
        runTest {
            val tampered =
                projection(revision = 5, state = SessionState.ARMED).copy(
                    boxId = "ffffffff-ffff-ffff-ffff-ffffffffffff",
                    boxTokenSha256Hex = "f".repeat(64),
                    ringtoneKey = "autre_sonnerie",
                    vibrationEnabled = false,
                )

            merge.merge(tampered)

            val entity = requireNotNull(database.sessionDao().findById(SESSION_ID))
            assertThat(entity.boxId).isEqualTo(RoomTestFixtures.extras().boxId)
            assertThat(entity.boxTokenSha256Hex).isEqualTo(RoomTestFixtures.extras().boxTokenSha256Hex)
            assertThat(entity.ringtoneKey).isEqualTo(RoomTestFixtures.extras().ringtoneKey)
            assertThat(entity.vibrationEnabled).isEqualTo(RoomTestFixtures.extras().vibrationEnabled)
        }

    /**
     * [eventId] est paramétré : `ReceiptDao.insert` est en `ABORT` — c'est le signal
     * `EVENT_ID_CONFLICT` du coordinateur — donc réensemencer avec le même identifiant ferait
     * échouer la transaction avant même d'atteindre ce qu'on veut mesurer.
     */
    private suspend fun seedRoomSession(
        revision: Long,
        eventId: String = "seed-event",
    ) {
        store.commitDecision(
            StoredDecision(
                snapshot = RoomTestFixtures.preparingSnapshot(SESSION_ID, revision = revision),
                receipt = RoomTestFixtures.receipt(eventId, SESSION_ID, appliedRevision = revision),
                effects = emptyList(),
                androidExtras = RoomTestFixtures.extras(),
            ),
        )
    }

    private fun projection(
        revision: Long,
        state: SessionState = SessionState.ARMED,
        sessionId: String = SESSION_ID,
        receipts: List<EventReceipt> = emptyList(),
        effects: List<PendingEffect> = emptyList(),
    ): DirectBootSnapshot.Active =
        DirectBootMapper.projectionOf(
            snapshot = RoomTestFixtures.preparingSnapshot(sessionId, revision).copy(state = state),
            extras = RoomTestFixtures.extras(),
            receipts = receipts,
            effects = effects,
        )

    private fun pendingEffect(
        revision: Long,
        ordinal: Int,
        status: EffectStatus,
    ): PendingEffect =
        RoomTestFixtures
            .effect(SESSION_ID, revision, ordinal, SessionEffectKindDto.PRESENT_SCAN_REQUEST)
            .copy(status = status)

    private companion object {
        const val SESSION_ID = "11111111-1111-1111-1111-111111111111"
        const val OTHER_SESSION_ID = "99999999-9999-9999-9999-999999999999"
        const val NOW = 1_800_000_100_000L
    }
}
