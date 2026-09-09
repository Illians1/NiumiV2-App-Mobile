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

/** Association complète à l'étape 13 ; ici, seule la persistance brute est vérifiée. */
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
                PairedBoxEntity(boxId, protocolVersion = 1, tokenSha256 = "c".repeat(64), pairedAtEpochMillis = 1L),
            )

            dao.upsert(
                PairedBoxEntity(boxId, protocolVersion = 1, tokenSha256 = "d".repeat(64), pairedAtEpochMillis = 2L),
            )

            assertThat(dao.findById(boxId)?.tokenSha256).isEqualTo("d".repeat(64))
        }
}
