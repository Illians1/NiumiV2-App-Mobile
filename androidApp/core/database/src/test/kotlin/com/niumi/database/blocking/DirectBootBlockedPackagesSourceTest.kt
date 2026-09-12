package com.niumi.database.blocking

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.SessionEffectKind
import com.niumi.core.domain.SessionHealth
import com.niumi.core.domain.SessionState
import com.niumi.database.BlockedPackage
import com.niumi.database.EffectStatus
import com.niumi.database.directboot.DirectBootBlockedPackage
import com.niumi.database.directboot.DirectBootEffect
import com.niumi.database.directboot.DirectBootSnapshot
import com.niumi.database.directboot.DirectBootStore
import com.niumi.database.directboot.DirectBootWriteResult
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.test.runTest
import org.junit.Test
import javax.inject.Provider

/**
 * Branche avant déverrouillage de [RoomBlockedPackagesSource] (SPEC_ANDROID §7.3 : Room est
 * inaccessible, seul le snapshot du stockage protégé est lisible). Test JVM et non instrumenté :
 * aucune base n'est ouverte — c'est même ce que le [Provider] piégé ci-dessous prouve, sur le
 * modèle de `RoomSessionStoreUnlockGuardTest`.
 *
 * Règle centrale : un snapshot illisible ne se lit **jamais** « pas de session ». SPEC_ANDROID
 * §13 interdit la suppression silencieuse du blocage, donc la source renvoie
 * [BlockedPackagesRead.Unreadable] et laisse l'appelant conserver ce qu'il savait.
 */
class DirectBootBlockedPackagesSourceTest {
    private val sessionId = "11111111-1111-1111-1111-111111111111"

    private val blockedPackages =
        listOf(
            DirectBootBlockedPackage("com.example.first", "Première application"),
            DirectBootBlockedPackage("com.example.second", "Deuxième application"),
        )

    private val expectedPackages =
        setOf(
            BlockedPackage("com.example.first", "Première application"),
            BlockedPackage("com.example.second", "Deuxième application"),
        )

    private fun snapshot(
        state: SessionState,
        pendingEffects: List<DirectBootEffect> = emptyList(),
    ) = DirectBootSnapshot.Active(
        domainSchemaVersion = 1,
        domainRevision = 3,
        sessionId = sessionId,
        localDate = "2026-09-08",
        localTime = "07:00",
        zoneIdAtActivation = "Europe/Paris",
        triggerAtEpochMillis = 1_800_000_000_000L,
        state = state,
        releaseTarget = null,
        health = SessionHealth.HEALTHY,
        createdAtEpochMillis = 1_700_000_000_000L,
        armedAtEpochMillis = 1_700_000_001_000L,
        ringingAtEpochMillis = null,
        alarmSoundStoppedAtEpochMillis = null,
        triggerElapsedAtEpochMillis = null,
        nfcVerifiedAtEpochMillis = null,
        releasingAtEpochMillis = null,
        completedAtEpochMillis = null,
        cancelledAtEpochMillis = null,
        failureCode = null,
        ringtoneKey = "niumi_default",
        vibrationEnabled = true,
        boxId = "550e8400-e29b-41d4-a716-446655440000",
        boxTokenSha256Hex = "a".repeat(64),
        blockedPackages = blockedPackages,
        eventReceipts = emptyList(),
        pendingEffects = pendingEffects,
    )

    private fun effect(
        kind: SessionEffectKind,
        status: EffectStatus,
        revision: Long = 3,
    ) = DirectBootEffect(
        effectId = "$sessionId:$revision:$kind:0",
        sessionId = sessionId,
        revision = revision,
        kind = kind,
        ordinal = 0,
        payloadJson = null,
        status = status,
        lastError = null,
    )

    private fun sourceReading(stored: DirectBootSnapshot?): RoomBlockedPackagesSource =
        RoomBlockedPackagesSource(
            unlockState = LockedState,
            directBootStore = FixedDirectBootStore(stored),
            databaseProvider = Provider { error("ROOM_OPENED_BEFORE_UNLOCK") },
        )

    private suspend fun readState(stored: DirectBootSnapshot?): BlockedPackagesState {
        val read = sourceReading(stored).read()
        assertThat(read).isInstanceOf(BlockedPackagesRead.Resolved::class.java)
        return (read as BlockedPackagesRead.Resolved).state
    }

    @Test
    fun anAbsentSnapshotReadsAsInactive() =
        runTest {
            assertThat(readState(null)).isEqualTo(BlockedPackagesState.Inactive)
        }

    @Test
    fun aCorruptedSnapshotIsUnreadableAndNeverInactive() =
        runTest {
            val read = sourceReading(DirectBootSnapshot.Corrupted("json")).read()

            assertThat(read).isEqualTo(BlockedPackagesRead.Unreadable("json"))
        }

    @Test
    fun anArmedSnapshotReadsAsActive() =
        runTest {
            assertThat(readState(snapshot(SessionState.ARMED)))
                .isEqualTo(BlockedPackagesState.Active(sessionId, expectedPackages))
        }

    /**
     * Le miroir Direct Boot ne recopie que les effets rejouables (`PENDING`/`FAILED`) : un
     * `REMOVE_BLOCKING` déjà réussi y est **absent**, et son absence vaut « exécuté ». C'est
     * l'inverse de la règle Room, où l'absence de ligne signifie « jamais décidé ».
     */
    @Test
    fun releasingWithoutAPendingRemovalReadsAsFullyReleased() =
        runTest {
            assertThat(readState(snapshot(SessionState.RELEASING)))
                .isEqualTo(BlockedPackagesState.Releasing(sessionId, emptySet()))
        }

    @Test
    fun releasingWithAPendingRemovalKeepsEveryPackageBlocked() =
        runTest {
            val stored =
                snapshot(
                    SessionState.RELEASING,
                    listOf(effect(SessionEffectKind.REMOVE_BLOCKING, EffectStatus.PENDING)),
                )

            assertThat(readState(stored))
                .isEqualTo(BlockedPackagesState.Releasing(sessionId, expectedPackages))
        }

    @Test
    fun preparingWithAPendingApplyIsNotYetBlocking() =
        runTest {
            val stored =
                snapshot(
                    SessionState.PREPARING,
                    listOf(effect(SessionEffectKind.APPLY_BLOCKING, EffectStatus.PENDING)),
                )

            assertThat(readState(stored)).isEqualTo(BlockedPackagesState.Inactive)
        }

    /** Absence d'`APPLY_BLOCKING` rejouable : l'effet a déjà réussi, le blocage est posé. */
    @Test
    fun preparingWithoutAPendingApplyIsAlreadyBlocking() =
        runTest {
            assertThat(readState(snapshot(SessionState.PREPARING)))
                .isEqualTo(BlockedPackagesState.Active(sessionId, expectedPackages))
        }

    @Test
    fun finalStatesReadAsInactive() =
        runTest {
            listOf(SessionState.COMPLETED, SessionState.CANCELLED, SessionState.FAILED)
                .forEach { state ->
                    assertThat(readState(snapshot(state))).isEqualTo(BlockedPackagesState.Inactive)
                }
        }

    private object LockedState : UnlockState {
        override val isUserUnlocked: Boolean = false
    }

    private class FixedDirectBootStore(
        private val stored: DirectBootSnapshot?,
    ) : DirectBootStore {
        override fun read(): DirectBootSnapshot? = stored

        override fun write(snapshot: DirectBootSnapshot.Active): DirectBootWriteResult =
            error("DIRECT_BOOT_WRITE_IN_TEST")

        override fun clear() = error("DIRECT_BOOT_CLEAR_IN_TEST")
    }
}
