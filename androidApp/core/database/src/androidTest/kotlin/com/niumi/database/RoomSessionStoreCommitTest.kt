package com.niumi.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.database.entity.ActiveSessionPointerEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * `commitDecision` écrit session, applications, pointeur, reçu et effets dans une seule
 * transaction (SPEC_CORE_KMP §6.1, §12 ; SPEC_ANDROID §9.2 étape 4). Nécessite un appareil ou un
 * émulateur (`connectedDebugAndroidTest`) : Room refuse toute requête hors instrumentation.
 */
@RunWith(AndroidJUnit4::class)
class RoomSessionStoreCommitTest {
    private lateinit var database: NiumiDatabase
    private lateinit var store: RoomSessionStore

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        store = RoomSessionStore(Provider { database }, AlwaysUnlockedState)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun commitDecisionWritesTheFiveTablesAndActiveSessionRereadsThemAll() =
        runTest {
            val sessionId = "11111111-1111-1111-1111-111111111111"
            val snapshot = RoomTestFixtures.preparingSnapshot(sessionId)
            val extras = RoomTestFixtures.extras()
            val receipt = RoomTestFixtures.receipt(eventId = "event-1", sessionId = sessionId)
            val effect = RoomTestFixtures.effect(sessionId, revision = 1, ordinal = 0)

            store.commitDecision(StoredDecision(snapshot, receipt, listOf(effect), extras))
            val active = store.activeSession()

            assertThat(active).isNotNull()
            assertThat(active?.snapshot).isEqualTo(snapshot)
            assertThat(active?.extras).isEqualTo(extras)
            assertThat(active?.pendingEffects).containsExactly(effect)
            assertThat(store.findReceipt("event-1")).isEqualTo(receipt)
        }

    @Test
    fun secondDecisionPreservesFrozenExtrasEvenIfCallerSuppliesDifferentOnes() =
        runTest {
            val sessionId = "22222222-2222-2222-2222-222222222222"
            val originalExtras = RoomTestFixtures.extras(boxId = "original-box")
            store.commitDecision(
                StoredDecision(
                    RoomTestFixtures.preparingSnapshot(sessionId, revision = 1),
                    RoomTestFixtures.receipt("event-1", sessionId, appliedRevision = 1),
                    emptyList(),
                    originalExtras,
                ),
            )

            val tamperedExtras = RoomTestFixtures.extras(boxId = "different-box-should-be-ignored")
            store.commitDecision(
                StoredDecision(
                    RoomTestFixtures.preparingSnapshot(sessionId, revision = 2),
                    RoomTestFixtures.receipt("event-2", sessionId, appliedRevision = 2),
                    emptyList(),
                    tamperedExtras,
                ),
            )

            val active = store.activeSession()
            assertThat(active?.extras?.boxId).isEqualTo("original-box")
        }

    @Test
    fun conflictingEventIdRollsBackTheEntireTransaction() =
        runTest {
            val firstSessionId = "33333333-3333-3333-3333-333333333333"
            store.commitDecision(
                StoredDecision(
                    RoomTestFixtures.preparingSnapshot(firstSessionId),
                    RoomTestFixtures.receipt("shared-event-id", firstSessionId),
                    emptyList(),
                    RoomTestFixtures.extras(),
                ),
            )

            val secondSessionId = "44444444-4444-4444-4444-444444444444"
            var threw = false
            try {
                store.commitDecision(
                    StoredDecision(
                        RoomTestFixtures.preparingSnapshot(secondSessionId),
                        // Même eventId qu'une session différente : conflit sur la PK du reçu.
                        RoomTestFixtures.receipt("shared-event-id", secondSessionId),
                        listOf(RoomTestFixtures.effect(secondSessionId, revision = 1, ordinal = 0)),
                        RoomTestFixtures.extras(),
                    ),
                )
            } catch (expected: Exception) {
                threw = true
            }

            assertThat(threw).isTrue()
            assertThat(database.sessionDao().findById(secondSessionId)).isNull()
            assertThat(database.blockedAppDao().forSession(secondSessionId)).isEmpty()
            assertThat(database.outboxDao().replayable(secondSessionId)).isEmpty()
            // Le pointeur n'a pas bougé vers la session en échec : la première session reste active.
            assertThat(store.activeSession()?.snapshot?.sessionId).isEqualTo(firstSessionId)
        }

    /**
     * Second point d'échec, plus tardif que celui du reçu : la dernière écriture de la transaction
     * (l'outbox) échoue sur sa clé étrangère. Prouve que la session, ses applications, le pointeur
     * **et le reçu** sont tous annulés — le rollback ne dépend donc pas d'un ordre d'écriture
     * heureux.
     */
    @Test
    fun outboxForeignKeyViolationRollsBackTheEntireTransaction() =
        runTest {
            val sessionId = "55555555-5555-5555-5555-555555555555"
            val effectOnAnUnknownSession =
                RoomTestFixtures.effect("99999999-9999-9999-9999-999999999999", revision = 1, ordinal = 0)

            var threw = false
            try {
                store.commitDecision(
                    StoredDecision(
                        RoomTestFixtures.preparingSnapshot(sessionId),
                        RoomTestFixtures.receipt("event-fk", sessionId),
                        listOf(effectOnAnUnknownSession),
                        RoomTestFixtures.extras(),
                    ),
                )
            } catch (expected: Exception) {
                threw = true
            }

            assertThat(threw).isTrue()
            assertThat(database.sessionDao().findById(sessionId)).isNull()
            assertThat(database.blockedAppDao().forSession(sessionId)).isEmpty()
            assertThat(database.receiptDao().findByEventId("event-fk")).isNull()
            assertThat(database.activeSessionPointerDao().current()).isNull()
        }

    /**
     * `BlockedAppDao.insertAll` est en `REPLACE` : une sélection contenant deux fois le même
     * package est absorbée (dernier gagnant) plutôt que rejetée. Comportement vérifié ici pour que
     * le coordinateur (étape 11) puisse s'y fier : un doublon ne fait pas échouer une activation.
     */
    @Test
    fun duplicateBlockedPackageIsCollapsedByLastWriteWins() =
        runTest {
            val sessionId = "66666666-6666-6666-6666-666666666666"
            val duplicated =
                RoomTestFixtures.extras(
                    blockedPackages =
                        listOf(
                            BlockedPackage("com.example.dup", "Premier libellé"),
                            BlockedPackage("com.example.dup", "Second libellé"),
                        ),
                )

            store.commitDecision(
                StoredDecision(
                    RoomTestFixtures.preparingSnapshot(sessionId),
                    RoomTestFixtures.receipt("event-dup", sessionId),
                    emptyList(),
                    duplicated,
                ),
            )

            val stored = store.activeSession()?.extras?.blockedPackages
            assertThat(stored).hasSize(1)
            assertThat(stored?.single()?.displayNameSnapshot).isEqualTo("Second libellé")
        }

    /**
     * Un pointeur orphelin est impossible par construction : la clé étrangère vers `alarm_session`
     * le refuse, et `onDelete = CASCADE` l'emporte avec sa session. `activeSession()` conserve
     * malgré tout sa branche défensive, inatteignable — §13 interdit qu'un état incohérent soit
     * effacé silencieusement, et l'intégrité référentielle empêche ici cet état d'exister.
     */
    @Test
    fun aPointerCanNeverReferenceAMissingSession() =
        runTest {
            var threw = false
            try {
                database.activeSessionPointerDao().set(
                    ActiveSessionPointerEntity(sessionId = "77777777-0000-0000-0000-000000000000"),
                )
            } catch (expected: Exception) {
                threw = true
            }

            assertThat(threw).isTrue()
            assertThat(database.activeSessionPointerDao().current()).isNull()
            assertThat(store.activeSession()).isNull()
        }

    @Test
    fun clearActivePointerIsIdempotentAndScopedToItsSession() =
        runTest {
            val sessionId = "77777777-7777-7777-7777-777777777777"
            store.commitDecision(
                StoredDecision(
                    RoomTestFixtures.preparingSnapshot(sessionId),
                    RoomTestFixtures.receipt("event-clear", sessionId),
                    emptyList(),
                    RoomTestFixtures.extras(),
                ),
            )

            store.clearActivePointer("some-other-session-id")
            assertThat(store.activeSession()).isNotNull()

            store.clearActivePointer(sessionId)
            store.clearActivePointer(sessionId)
            assertThat(store.activeSession()).isNull()
        }
}
