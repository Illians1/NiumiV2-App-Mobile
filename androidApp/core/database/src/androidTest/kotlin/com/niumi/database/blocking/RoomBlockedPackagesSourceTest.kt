package com.niumi.database.blocking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.AlwaysUnlockedState
import com.niumi.database.BlockedPackage
import com.niumi.database.EffectStatus
import com.niumi.database.NiumiDatabase
import com.niumi.database.PendingEffect
import com.niumi.database.RoomSessionStore
import com.niumi.database.RoomTestFixtures
import com.niumi.database.StoredDecision
import com.niumi.database.directboot.DirectBootSnapshot
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.directboot.DirectBootWriteResult
import com.niumi.database.newInMemoryDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * Reconstruction de la projection de blocage depuis Room (SPEC_ANDROID §12.2, §3). L'enjeu est
 * qu'un processus tué ne fasse plus disparaître le blocage : la vérité vient de la base, plus de
 * la mémoire du contrôleur (étape 5).
 *
 * Deux règles que l'état seul ne suffit pas à décider (SPEC_CORE_KMP §4 : « `RELEASING` autorise
 * un nettoyage partiel. L'état seul ne permet pas de déduire si le blocage natif est encore
 * appliqué ») :
 * - en `RELEASING`, c'est le statut de l'effet `REMOVE_BLOCKING` qui décide de la liste effective ;
 * - en `PREPARING`, c'est celui de `APPLY_BLOCKING` qui décide si le blocage est déjà posé.
 */
@RunWith(AndroidJUnit4::class)
class RoomBlockedPackagesSourceTest {
    private lateinit var database: NiumiDatabase
    private lateinit var store: RoomSessionStore
    private lateinit var source: RoomBlockedPackagesSource
    private val sessionId = "77777777-7777-7777-7777-777777777777"

    private val blockedPackages =
        listOf(
            BlockedPackage("com.example.first", "Première application"),
            BlockedPackage("com.example.second", "Deuxième application"),
        )

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        store = RoomSessionStore(Provider { database }, AlwaysUnlockedState)
        source =
            RoomBlockedPackagesSource(
                unlockState = AlwaysUnlockedState,
                directBootStore = UnusedDirectBootStore,
                databaseProvider = Provider { database },
            )
    }

    @After
    fun tearDown() = database.close()

    private fun snapshotInState(
        state: SessionStateDto,
        revision: Long = 1,
    ): SessionSnapshotDto = RoomTestFixtures.preparingSnapshot(sessionId, revision).copy(state = state)

    private fun blockingEffect(
        kind: SessionEffectKindDto,
        revision: Long = 1,
        ordinal: Int = 0,
    ): PendingEffect = RoomTestFixtures.effect(sessionId, revision, ordinal, kind)

    /**
     * [eventId] doit être unique par appel : `session_event_receipt.eventId` est une clé primaire,
     * et c'est ce qui fonde le registre d'idempotence de l'étape 11. Les tests qui rejouent
     * plusieurs états sur la même session en fournissent donc un par itération.
     */
    private suspend fun seed(
        state: SessionStateDto,
        effects: List<PendingEffect> = emptyList(),
        eventId: String = "seed-event",
    ) {
        store.commitDecision(
            StoredDecision(
                snapshot = snapshotInState(state),
                receipt = RoomTestFixtures.receipt(eventId, sessionId),
                effects = effects,
                androidExtras = RoomTestFixtures.extras(blockedPackages = blockedPackages),
            ),
        )
    }

    private suspend fun readState(): BlockedPackagesState {
        val read = source.read()
        assertThat(read).isInstanceOf(BlockedPackagesRead.Resolved::class.java)
        return (read as BlockedPackagesRead.Resolved).state
    }

    @Test
    fun anAbsentPointerReadsAsInactive() =
        runTest {
            assertThat(readState()).isEqualTo(BlockedPackagesState.Inactive)
        }

    @Test
    fun anArmedSessionReadsAsActiveWithItsFrozenPackages() =
        runTest {
            seed(SessionStateDto.ARMED)

            assertThat(readState())
                .isEqualTo(BlockedPackagesState.Active(sessionId, blockedPackages.toSet()))
        }

    @Test
    fun ringingAndAwaitingScanStatesStillBlock() =
        runTest {
            listOf(
                SessionStateDto.RINGING,
                SessionStateDto.AWAITING_NFC,
                SessionStateDto.TRIGGERED_AWAITING_NFC,
            ).forEach { state ->
                seed(state, eventId = "seed-$state")

                assertThat(readState())
                    .isEqualTo(BlockedPackagesState.Active(sessionId, blockedPackages.toSet()))
            }
        }

    @Test
    fun releasingWithASucceededRemovalReadsAsReleasingWithNoPackageLeft() =
        runTest {
            val removal = blockingEffect(SessionEffectKindDto.REMOVE_BLOCKING)
            seed(SessionStateDto.RELEASING, listOf(removal))
            store.markEffect(removal.effectId, EffectStatus.SUCCEEDED, null)

            assertThat(readState()).isEqualTo(BlockedPackagesState.Releasing(sessionId, emptySet()))
        }

    @Test
    fun releasingWithASatisfiedRemovalAlsoReadsAsFullyReleased() =
        runTest {
            val removal = blockingEffect(SessionEffectKindDto.REMOVE_BLOCKING)
            seed(SessionStateDto.RELEASING, listOf(removal))
            store.markEffect(removal.effectId, EffectStatus.SATISFIED, null)

            assertThat(readState()).isEqualTo(BlockedPackagesState.Releasing(sessionId, emptySet()))
        }

    @Test
    fun releasingWithAPendingRemovalKeepsEveryPackageBlocked() =
        runTest {
            seed(SessionStateDto.RELEASING, listOf(blockingEffect(SessionEffectKindDto.REMOVE_BLOCKING)))

            assertThat(readState())
                .isEqualTo(BlockedPackagesState.Releasing(sessionId, blockedPackages.toSet()))
        }

    @Test
    fun releasingWithAFailedRemovalKeepsEveryPackageBlocked() =
        runTest {
            val removal = blockingEffect(SessionEffectKindDto.REMOVE_BLOCKING)
            seed(SessionStateDto.RELEASING, listOf(removal))
            store.markEffect(removal.effectId, EffectStatus.FAILED, "boom")

            assertThat(readState())
                .isEqualTo(BlockedPackagesState.Releasing(sessionId, blockedPackages.toSet()))
        }

    /**
     * `RELEASING` sans aucune ligne `REMOVE_BLOCKING` : le retrait n'a pas encore été décidé, donc
     * rien ne prouve que le blocage est levé. Le blocage tient — §13 interdit de le lever sans
     * preuve.
     */
    @Test
    fun releasingWithoutAnyRemovalEffectKeepsEveryPackageBlocked() =
        runTest {
            seed(SessionStateDto.RELEASING)

            assertThat(readState())
                .isEqualTo(BlockedPackagesState.Releasing(sessionId, blockedPackages.toSet()))
        }

    /**
     * Seule la révision la plus récente décrit l'état courant : une reprise peut avoir produit un
     * second `REMOVE_BLOCKING`, encore en attente, après un premier déjà réussi.
     */
    @Test
    fun theMostRecentRemovalRevisionDecides() =
        runTest {
            val firstRemoval = blockingEffect(SessionEffectKindDto.REMOVE_BLOCKING, revision = 1)
            val secondRemoval = blockingEffect(SessionEffectKindDto.REMOVE_BLOCKING, revision = 2)
            seed(SessionStateDto.RELEASING, listOf(firstRemoval, secondRemoval))
            store.markEffect(firstRemoval.effectId, EffectStatus.SUCCEEDED, null)

            assertThat(readState())
                .isEqualTo(BlockedPackagesState.Releasing(sessionId, blockedPackages.toSet()))
        }

    /**
     * Garde-fou pour `SessionReconciler.reconcilePreparing`, qui déduit de `Active` que
     * l'activation a réussi : en `PREPARING`, la seule présence des lignes `blocked_app` ne prouve
     * rien, l'effet `APPLY_BLOCKING` peut n'avoir jamais été exécuté.
     */
    @Test
    fun preparingWithAPendingApplyIsNotYetBlocking() =
        runTest {
            seed(SessionStateDto.PREPARING, listOf(blockingEffect(SessionEffectKindDto.APPLY_BLOCKING)))

            assertThat(readState()).isEqualTo(BlockedPackagesState.Inactive)
        }

    @Test
    fun preparingWithASucceededApplyIsAlreadyBlocking() =
        runTest {
            val apply = blockingEffect(SessionEffectKindDto.APPLY_BLOCKING)
            seed(SessionStateDto.PREPARING, listOf(apply))
            store.markEffect(apply.effectId, EffectStatus.SUCCEEDED, null)

            assertThat(readState())
                .isEqualTo(BlockedPackagesState.Active(sessionId, blockedPackages.toSet()))
        }

    @Test
    fun preparingWithoutAnyApplyEffectIsNotBlocking() =
        runTest {
            seed(SessionStateDto.PREPARING)

            assertThat(readState()).isEqualTo(BlockedPackagesState.Inactive)
        }

    @Test
    fun finalStatesReadAsInactiveEvenWhileThePointerStillExists() =
        runTest {
            listOf(
                SessionStateDto.COMPLETED,
                SessionStateDto.CANCELLED,
                SessionStateDto.FAILED,
            ).forEach { state ->
                seed(state, eventId = "seed-$state")

                assertThat(readState()).isEqualTo(BlockedPackagesState.Inactive)
            }
        }

    @Test
    fun aClearedPointerReadsAsInactive() =
        runTest {
            seed(SessionStateDto.ARMED)
            store.clearActivePointer(sessionId)

            assertThat(readState()).isEqualTo(BlockedPackagesState.Inactive)
        }

    /** Une session sans application sélectionnée est active sans rien bloquer. */
    @Test
    fun anArmedSessionWithoutAnyPackageReadsAsActiveAndEmpty() =
        runTest {
            store.commitDecision(
                StoredDecision(
                    snapshot = snapshotInState(SessionStateDto.ARMED),
                    receipt = RoomTestFixtures.receipt("seed-event", sessionId),
                    effects = emptyList(),
                    androidExtras = RoomTestFixtures.extras(blockedPackages = emptyList()),
                ),
            )

            assertThat(readState()).isEqualTo(BlockedPackagesState.Active(sessionId, emptySet()))
        }

    /** Déverrouillé, le snapshot Direct Boot n'est jamais consulté : Room fait foi (§7.3). */
    private object UnusedDirectBootStore : DirectBootStore {
        override fun read(): DirectBootSnapshot? = error("DIRECT_BOOT_READ_WHILE_UNLOCKED")

        override fun write(snapshot: DirectBootSnapshot.Active): DirectBootWriteResult =
            error("DIRECT_BOOT_WRITE_IN_TEST")

        override fun clear() = error("DIRECT_BOOT_CLEAR_IN_TEST")
    }
}
