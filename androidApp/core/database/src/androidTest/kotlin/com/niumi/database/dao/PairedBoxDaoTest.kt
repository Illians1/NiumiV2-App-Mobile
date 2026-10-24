package com.niumi.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.database.NiumiDatabase
import com.niumi.database.entity.PairedBoxEntity
import com.niumi.database.newInMemoryDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Association complète à l'étape 13, Niumi Spots multiples au Lot 8 ; ici, seule la persistance brute est
 * vérifiée.
 */
@RunWith(AndroidJUnit4::class)
class PairedBoxDaoTest {
    private lateinit var database: NiumiDatabase

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun insertThenReadReturnsTheSameBox() =
        runTest {
            val dao = database.pairedBoxDao()
            val box =
                PairedBoxEntity(
                    boxId = "550e8400-e29b-41d4-a716-446655440000",
                    protocolVersion = 1,
                    tokenSha256 = "c".repeat(64),
                    pairedAtEpochMillis = 1_700_000_000_000L,
                    name = "Cuisine",
                    position = 0,
                )

            dao.upsert(box)

            assertThat(dao.findById(box.boxId)).isEqualTo(box)
        }

    @Test
    fun upsertReplacesAnExistingBoxWithTheSameId() =
        runTest {
            val dao = database.pairedBoxDao()
            val boxId = "550e8400-e29b-41d4-a716-446655440000"
            dao.upsert(
                PairedBoxEntity(
                    boxId,
                    protocolVersion = 1,
                    tokenSha256 = "c".repeat(64),
                    pairedAtEpochMillis = 1L,
                    name = "Cuisine",
                    position = 0,
                ),
            )

            dao.upsert(
                PairedBoxEntity(
                    boxId,
                    protocolVersion = 1,
                    tokenSha256 = "d".repeat(64),
                    pairedAtEpochMillis = 2L,
                    name = "Cuisine",
                    position = 0,
                ),
            )

            assertThat(dao.findById(boxId)?.tokenSha256).isEqualTo("d".repeat(64))
        }

    @Test
    fun allOrdersByPosition() =
        runTest {
            val dao = database.pairedBoxDao()
            dao.upsert(box(boxId = "a", name = "Deuxième", position = 1))
            dao.upsert(box(boxId = "b", name = "Première", position = 0))

            assertThat(dao.all().map { it.name }).containsExactly("Première", "Deuxième").inOrder()
        }

    @Test
    fun renameChangesOnlyTheName() =
        runTest {
            val dao = database.pairedBoxDao()
            val boxId = "550e8400-e29b-41d4-a716-446655440000"
            dao.upsert(box(boxId = boxId, name = "Cuisine", position = 0))

            dao.rename(boxId, "Salle de bain")

            assertThat(dao.findById(boxId)?.name).isEqualTo("Salle de bain")
            assertThat(dao.findById(boxId)?.position).isEqualTo(0)
        }

    @Test
    fun deleteRemovesExactlyOneBox() =
        runTest {
            val dao = database.pairedBoxDao()
            dao.upsert(box(boxId = "a", name = "Cuisine", position = 0))
            dao.upsert(box(boxId = "b", name = "Chambre", position = 1))

            dao.delete("a")

            assertThat(dao.findById("a")).isNull()
            assertThat(dao.findById("b")).isNotNull()
        }

    @Test
    fun countReflectsTheNumberOfStoredBoxes() =
        runTest {
            val dao = database.pairedBoxDao()
            assertThat(dao.count()).isEqualTo(0)

            dao.upsert(box(boxId = "a", name = "Cuisine", position = 0))

            assertThat(dao.count()).isEqualTo(1)
        }

    private fun box(
        boxId: String,
        name: String,
        position: Int,
    ) = PairedBoxEntity(
        boxId = boxId,
        protocolVersion = 1,
        tokenSha256 = "c".repeat(64),
        pairedAtEpochMillis = 1_700_000_000_000L,
        name = name,
        position = position,
    )
}
