package com.niumi.database.pairing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.AlwaysUnlockedState
import com.niumi.database.NiumiDatabase
import com.niumi.database.newInMemoryDatabase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * SPEC_ANDROID §11.1 (Lot 8 ; dix au plus depuis le contrat KMP 1.5) : jusqu'à dix Niumi Spots
 * nommés, ordonnés par position, dont le dernier ne peut pas être retiré (décision Q22, étape 43).
 * `all()` avant déverrouillage renvoie une liste vide plutôt que
 * de lever, à la différence du reste de
 * `RoomPairedBoxStore` — voir le KDoc de la classe.
 */
@RunWith(AndroidJUnit4::class)
class RoomPairedBoxStoreTest {
    private lateinit var database: NiumiDatabase
    private lateinit var store: RoomPairedBoxStore

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        store = RoomPairedBoxStore(Provider { database }, AlwaysUnlockedState)
    }

    @After
    fun tearDown() = database.close()

    private fun credential(
        boxId: String = "550e8400-e29b-41d4-a716-446655440000",
        tokenPrefix: Char = 'a',
    ) = PairedBoxCredentialDto(1, boxId, tokenPrefix.toString().repeat(64))

    @Test
    fun allIsEmptyWhenNothingIsPaired() =
        runTest {
            assertThat(store.all()).isEmpty()
        }

    @Test
    fun addThenAllReturnsTheBoxWithItsNameInPositionZero() =
        runTest {
            val credential = credential()

            val result = store.add(credential, "Cuisine")

            assertThat(result).isEqualTo(PairedBoxAddResult.Added)
            assertThat(store.all()).containsExactly(PairedBoxDto(credential, "Cuisine"))
        }

    @Test
    fun addingTheSameBoxIdTwiceReturnsAlreadyPairedAndKeepsOneRow() =
        runTest {
            val credential = credential()
            store.add(credential, "Cuisine")

            val result = store.add(credential.copy(tokenSha256Hex = "b".repeat(64)), "Chambre")

            assertThat(result).isEqualTo(PairedBoxAddResult.AlreadyPaired)
            assertThat(store.all()).containsExactly(PairedBoxDto(credential, "Cuisine"))
        }

    @Test
    fun aTenthBoxIsAccepted() =
        runTest {
            repeat(9) { index -> store.add(credential(boxId = "box-$index"), "Niumi Spot ${index + 1}") }

            val result = store.add(credential(boxId = "box-9"), "Niumi Spot 10")

            assertThat(result).isEqualTo(PairedBoxAddResult.Added)
            assertThat(store.all()).hasSize(10)
        }

    @Test
    fun anEleventhBoxReturnsLimitReachedAndWritesNothing() =
        runTest {
            repeat(10) { index -> store.add(credential(boxId = "box-$index"), "Niumi Spot ${index + 1}") }

            val result = store.add(credential(boxId = "box-10"), "Niumi Spot 11")

            assertThat(result).isEqualTo(PairedBoxAddResult.LimitReached)
            assertThat(store.all()).hasSize(10)
        }

    @Test
    fun renameChangesOnlyTheName() =
        runTest {
            val credential = credential()
            store.add(credential, "Cuisine")

            store.rename(credential.boxId, "Salle de bain")

            assertThat(store.all()).containsExactly(PairedBoxDto(credential, "Salle de bain"))
        }

    @Test
    fun addStoresTheTrimmedName() =
        runTest {
            val credential = credential()

            store.add(credential, "  Cuisine  ")

            assertThat(store.all()).containsExactly(PairedBoxDto(credential, "Cuisine"))
        }

    @Test
    fun renameStoresTheTrimmedName() =
        runTest {
            val credential = credential()
            store.add(credential, "Cuisine")

            store.rename(credential.boxId, " Salle de bain ")

            assertThat(store.all()).containsExactly(PairedBoxDto(credential, "Salle de bain"))
        }

    @Test
    fun removeDropsOneBoxAndRenumbersPositions() =
        runTest {
            store.add(credential(boxId = "box-0"), "Premier")
            val removed = credential(boxId = "box-1")
            store.add(removed, "Deuxième")
            store.add(credential(boxId = "box-2"), "Troisième")

            val result = store.remove(removed.boxId)

            assertThat(result).isEqualTo(PairedBoxRemoveResult.Removed)
            val remaining = store.all()
            assertThat(remaining.map { it.name }).containsExactly("Premier", "Troisième").inOrder()
            assertThat(database.pairedBoxDao().findById("box-0")?.position).isEqualTo(0)
            assertThat(database.pairedBoxDao().findById("box-2")?.position).isEqualTo(1)
        }

    @Test
    fun allIsOrderedByPosition() =
        runTest {
            store.add(credential(boxId = "box-0"), "Premier")
            store.add(credential(boxId = "box-1"), "Deuxième")
            store.add(credential(boxId = "box-2"), "Troisième")

            assertThat(store.all().map { it.name }).containsExactly("Premier", "Deuxième", "Troisième").inOrder()
        }

    /** Décision Q22 (étape 43) : il reste toujours au moins un Niumi Spot ; le refus n'écrit rien. */
    @Test
    fun removingTheLastBoxIsRefusedWithLastBoxKept() =
        runTest {
            val only = credential()
            store.add(only, "Cuisine")

            val result = store.remove(only.boxId)

            assertThat(result).isEqualTo(PairedBoxRemoveResult.LastBoxKept)
            assertThat(store.all()).containsExactly(PairedBoxDto(only, "Cuisine"))
        }

    @Test
    fun removingAnUnknownBoxAnswersUnknownAndWritesNothing() =
        runTest {
            store.add(credential(boxId = "box-0"), "Premier")
            store.add(credential(boxId = "box-1"), "Deuxième")

            val result = store.remove("box-inconnu")

            assertThat(result).isEqualTo(PairedBoxRemoveResult.Unknown)
            assertThat(store.all().map { it.name }).containsExactly("Premier", "Deuxième").inOrder()
        }

    /**
     * Étape 43 : [RoomPairedBoxStore.points] réémet la liste à chaque ajout, renommage et retrait,
     * dans l'ordre des positions. Room peut réémettre une même liste : chaque attente ignore les
     * émissions intermédiaires jusqu'à la liste attendue.
     */
    @Test
    fun pointsEmitsOnAddRenameAndRemove() =
        runBlocking {
            val seen = Channel<List<String>>(Channel.UNLIMITED)
            val collector =
                launch {
                    store.points.collect { boxes ->
                        seen.send(boxes.map { it.name })
                    }
                }

            suspend fun awaitNames(vararg expected: String) =
                withTimeout(FLOW_TIMEOUT_MILLIS) {
                    while (seen.receive() != expected.toList()) Unit
                }

            awaitNames()
            store.add(credential(boxId = "box-0"), "Cuisine")
            awaitNames("Cuisine")
            store.rename("box-0", "Salon")
            awaitNames("Salon")
            store.add(credential(boxId = "box-1"), "Entrée")
            awaitNames("Salon", "Entrée")
            store.remove("box-0")
            awaitNames("Entrée")
            collector.cancel()
        }

    private companion object {
        const val FLOW_TIMEOUT_MILLIS = 5_000L
    }
}
