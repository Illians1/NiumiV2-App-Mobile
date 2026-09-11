package com.niumi.database.directboot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.database.RoomTestFixtures
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val SESSION_ID = "11111111-1111-1111-1111-111111111111"
private const val OTHER_SESSION_ID = "22222222-2222-2222-2222-222222222222"

/**
 * [FileDirectBootStore] sur un vrai fichier, dans le stockage protégé de l'appareil
 * (SPEC_ANDROID §7.3). `decideWrite` est déjà couvert en JVM
 * (`DirectBootWriteDecisionTest`) : ces tests vérifient l'orchestration réelle (lecture, écriture
 * atomique, tolérance à la corruption), qu'aucun test JVM ne peut exercer sans le framework
 * Android.
 */
@RunWith(AndroidJUnit4::class)
class FileDirectBootStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var store: FileDirectBootStore
    private lateinit var backingFile: File

    private fun activeAt(
        sessionId: String,
        revision: Long,
    ) = DirectBootMapper.projectionOf(
        RoomTestFixtures.preparingSnapshot(sessionId, revision),
        RoomTestFixtures.extras(),
        listOf(RoomTestFixtures.receipt(eventId = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee", sessionId = sessionId)),
        listOf(RoomTestFixtures.effect(sessionId, revision, ordinal = 0)),
    )

    @Before
    fun setUp() {
        store = FileDirectBootStore(context)
        backingFile = File(context.createDeviceProtectedStorageContext().filesDir, "niumi_session.json")
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun readReturnsNullWhenNoSnapshotWasEverWritten() {
        assertThat(store.read()).isNull()
    }

    @Test
    fun writeThenReadReturnsTheSameSnapshot() {
        val snapshot = activeAt(SESSION_ID, revision = 1)

        val result = store.write(snapshot)

        assertThat(result).isEqualTo(DirectBootWriteResult.Written)
        assertThat(store.read()).isEqualTo(snapshot)
    }

    @Test
    fun theFileLivesUnderDeviceProtectedStorage() {
        store.write(activeAt(SESSION_ID, revision = 1))

        val expectedDirectory = context.createDeviceProtectedStorageContext().filesDir.absolutePath
        assertThat(backingFile.absolutePath).startsWith(expectedDirectory)
        assertThat(backingFile.exists()).isTrue()
    }

    @Test
    fun lowerRevisionOnTheSameSessionIsRefusedAndTheFileIsUnchanged() {
        store.write(activeAt(SESSION_ID, revision = 5))

        val result = store.write(activeAt(SESSION_ID, revision = 4))

        assertThat(result).isEqualTo(DirectBootWriteResult.StaleRevision)
        assertThat(store.read()).isEqualTo(activeAt(SESSION_ID, revision = 5))
    }

    @Test
    fun equalRevisionOnTheSameSessionIsIdempotent() {
        store.write(activeAt(SESSION_ID, revision = 5))

        val result = store.write(activeAt(SESSION_ID, revision = 5))

        assertThat(result).isEqualTo(DirectBootWriteResult.Written)
        assertThat(store.read()).isEqualTo(activeAt(SESSION_ID, revision = 5))
    }

    @Test
    fun lowerRevisionOnADifferentSessionIsAccepted() {
        store.write(activeAt(SESSION_ID, revision = 5))

        val result = store.write(activeAt(OTHER_SESSION_ID, revision = 1))

        assertThat(result).isEqualTo(DirectBootWriteResult.Written)
        assertThat(store.read()).isEqualTo(activeAt(OTHER_SESSION_ID, revision = 1))
    }

    @Test
    fun corruptedFileIsReportedAndNeverErased() {
        backingFile.parentFile?.mkdirs()
        backingFile.writeText("{ not valid json")

        val read = store.read()

        assertThat(read).isInstanceOf(DirectBootSnapshot.Corrupted::class.java)
        assertThat(backingFile.exists()).isTrue()
        assertThat(backingFile.readText()).isEqualTo("{ not valid json")
    }

    @Test
    fun aValidWriteOverwritesAPreviouslyCorruptedFile() {
        backingFile.parentFile?.mkdirs()
        backingFile.writeText("{ not valid json")

        val result = store.write(activeAt(SESSION_ID, revision = 1))

        assertThat(result).isEqualTo(DirectBootWriteResult.Written)
        assertThat(store.read()).isEqualTo(activeAt(SESSION_ID, revision = 1))
    }

    @Test
    fun clearDeletesTheFile() {
        store.write(activeAt(SESSION_ID, revision = 1))

        store.clear()

        assertThat(store.read()).isNull()
        assertThat(backingFile.exists()).isFalse()
    }

    @Test
    fun noTemporaryFileSurvivesASuccessfulWrite() {
        store.write(activeAt(SESSION_ID, revision = 1))

        val leftovers = backingFile.parentFile?.listFiles { file -> file.name != backingFile.name }.orEmpty()

        assertThat(leftovers).isEmpty()
    }
}
