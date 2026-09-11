package com.niumi.database.logging

import com.google.common.truth.Truth.assertThat
import com.niumi.database.directboot.UnlockState
import com.niumi.database.entity.TechnicalEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import javax.inject.Provider

/**
 * Garde `ROOM_BEFORE_UNLOCK` côté journal (SPEC_ANDROID §7.3, `ETAPE-09.md` écart 9) : avant
 * déverrouillage, tout va en mémoire et le binding Room n'est jamais résolu ; après, `log()` va en
 * Room et `recent()` fusionne les deux sources.
 *
 * Même piège que `RoomTechnicalEventLogTest` (`ETAPE-09.md`, découverte 13) : `log()` est
 * fire-and-forget, `runTest`/`StandardTestDispatcher` ne ferait pas avancer son `launch`. Le test
 * injecte un vrai `CoroutineScope` et attend explicitement la fin de ses enfants.
 */
class UnlockAwareTechnicalEventLogTest {
    private class FakeUnlockState(
        override var isUserUnlocked: Boolean,
    ) : UnlockState

    private class FakeTechnicalEventDao : com.niumi.database.dao.TechnicalEventDao {
        private val rows = mutableListOf<TechnicalEventEntity>()
        private var nextId = 1L

        override suspend fun insert(entity: TechnicalEventEntity) {
            synchronized(this) { rows += entity.copy(id = nextId++) }
        }

        override suspend fun mostRecent(limit: Int): List<TechnicalEventEntity> =
            synchronized(this) {
                rows
                    .sortedWith(
                        compareByDescending<TechnicalEventEntity> {
                            it.createdAtEpochMillis
                        }.thenByDescending { it.id },
                    ).take(limit)
            }

        override suspend fun purgeBeyond(limit: Int) {
            synchronized(this) {
                val kept =
                    rows
                        .sortedWith(
                            compareByDescending<TechnicalEventEntity> {
                                it.createdAtEpochMillis
                            }.thenByDescending { it.id },
                        ).take(limit)
                        .map { it.id }
                        .toSet()
                rows.retainAll { it.id in kept }
            }
        }
    }

    @Suppress("InjectDispatcher") // Le test est ici le fournisseur du scope de l'objet sous test.
    private fun awaitWrites(scope: CoroutineScope) =
        runBlocking {
            scope.coroutineContext.job.children
                .toList()
                .joinAll()
        }

    @Test
    fun `before unlock log goes to memory and the room provider is never resolved`() {
        val log =
            UnlockAwareTechnicalEventLog(
                unlockState = FakeUnlockState(isUserUnlocked = false),
                inMemory = InMemoryTechnicalEventLog(nowEpochMillis = { 1L }),
                roomLog = Provider { error("Room must not be resolved before unlock") },
            )

        log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "s1")

        assertThat(runBlocking { log.recent() }.single().sessionId).isEqualTo("s1")
    }

    @Test
    @Suppress("InjectDispatcher") // Le test est ici le fournisseur du scope de l'objet sous test.
    fun `after unlock log goes to room`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val roomLog = RoomTechnicalEventLog(FakeTechnicalEventDao(), scope, nowEpochMillis = { 2L })
        val log =
            UnlockAwareTechnicalEventLog(
                unlockState = FakeUnlockState(isUserUnlocked = true),
                inMemory = InMemoryTechnicalEventLog(nowEpochMillis = { 1L }),
                roomLog = Provider { roomLog },
            )

        log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "s2")
        awaitWrites(scope)

        val entries = runBlocking { log.recent() }
        assertThat(entries).hasSize(1)
        assertThat(entries.single().sessionId).isEqualTo("s2")
    }

    @Test
    @Suppress("InjectDispatcher") // Le test est ici le fournisseur du scope de l'objet sous test.
    fun `recent merges both sources sorted by most recent first and bounded to 200`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val roomLog = RoomTechnicalEventLog(FakeTechnicalEventDao(), scope, nowEpochMillis = { 100L })
        val inMemory = InMemoryTechnicalEventLog(nowEpochMillis = { 50L })
        inMemory.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "before-unlock")
        val log = UnlockAwareTechnicalEventLog(FakeUnlockState(isUserUnlocked = true), inMemory, Provider { roomLog })

        log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "after-unlock")
        awaitWrites(scope)

        val entries = runBlocking { log.recent() }
        assertThat(entries.map { it.sessionId }).containsExactly("after-unlock", "before-unlock").inOrder()
    }
}
