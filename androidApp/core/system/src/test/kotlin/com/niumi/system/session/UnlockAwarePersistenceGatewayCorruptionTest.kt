package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.SessionStore
import com.niumi.database.SessionStoreUnreadableException
import com.niumi.database.StoredDecision
import com.niumi.database.StoredSession
import com.niumi.system.boot.fakes.FakeUnlockState
import com.niumi.system.boot.fakes.InMemoryDirectBootStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * « Room aussi illisible », appareil déverrouillé (SPEC_ANDROID §18 ; SPEC_CORE_KMP §13, étape
 * 20). `RoomSessionStore.activeSession()` n'est pas testable en JVM — `SQLiteException` n'est pas
 * instanciable hors d'un appareil (stub `android.jar`) — ce fichier prouve donc la traduction que
 * [UnlockAwarePersistenceGateway] applique, indépendamment de la classe qui la déclenche.
 */
class UnlockAwarePersistenceGatewayCorruptionTest {
    private class ThrowingSessionStore(
        private val failure: Throwable,
    ) : SessionStore {
        override suspend fun activeSession(): StoredSession? = throw failure

        override suspend fun commitDecision(decision: StoredDecision) = throw UnsupportedOperationException()

        override suspend fun findReceipt(eventId: String): EventReceipt? = throw UnsupportedOperationException()

        override suspend fun receipts(sessionId: String): List<EventReceipt> = throw UnsupportedOperationException()

        override suspend fun pendingEffects(sessionId: String): List<PendingEffect> =
            throw UnsupportedOperationException()

        override suspend fun markEffect(
            effectId: String,
            status: EffectStatus,
            error: String?,
        ) = throw UnsupportedOperationException()

        override suspend fun clearActivePointer(sessionId: String) = throw UnsupportedOperationException()

        override suspend fun recordIncident(
            sessionId: String,
            incident: SessionIncidentDto,
        ) = throw UnsupportedOperationException()
    }

    private fun gatewayWith(sessionStore: SessionStore) =
        UnlockAwarePersistenceGateway(
            sessionStore = sessionStore,
            directBootStore = InMemoryDirectBootStore(),
            unlockState = FakeUnlockState(isUserUnlocked = true),
        )

    @Test
    fun aStoreFailureBecomesUnreadableRatherThanAnException() =
        runTest {
            val gateway = gatewayWith(ThrowingSessionStore(SessionStoreUnreadableException("SQLITE_CORRUPT")))

            val result = gateway.load()

            assertThat(result).isEqualTo(LoadResult.Unreadable("SQLITE_CORRUPT"))
        }

    /**
     * La garde de déverrouillage (`IllegalStateException("ROOM_BEFORE_UNLOCK")`) est un défaut de
     * programmation — un appelant a atteint Room sans passer par le bon chemin — pas une
     * corruption. La confondre avec [SessionStoreUnreadableException] masquerait un bug réel
     * derrière un écran de diagnostic anodin.
     */
    @Test
    fun theUnlockGuardIsNeverSwallowed() =
        runTest {
            val gateway = gatewayWith(ThrowingSessionStore(IllegalStateException("ROOM_BEFORE_UNLOCK")))

            assertThrows(IllegalStateException::class.java) {
                runBlocking { gateway.load() }
            }
        }
}
