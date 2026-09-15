package com.niumi.system.boot

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.SessionStoreUnreadableException
import com.niumi.database.directboot.DirectBootMapper
import com.niumi.database.directboot.DirectBootMergeResult
import com.niumi.database.directboot.DirectBootSnapshot
import com.niumi.system.boot.fakes.FakeUnlockState
import com.niumi.system.boot.fakes.InMemoryDirectBootStore
import com.niumi.system.boot.fakes.InMemorySessionStore
import com.niumi.system.boot.fakes.RecordingDirectBootRoomMerge
import com.niumi.system.session.fakes.SessionDtoFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Orchestration de la fusion Direct Boot → Room (SPEC_ANDROID §9.3, dernier alinéa). La transaction
 * Room elle-même — dédoublonnage des reçus, remontée de statut, refus d'une révision inférieure —
 * est prouvée par `RoomDirectBootMergeTest`, instrumenté : elle n'existe qu'en SQL.
 */
class DirectBootMergerTest {
    /**
     * **Régression mesurée sur appareil le 2026-09-15 (étape 20).** La fusion touche Room *avant*
     * le `gateway.load()` de la passe. Une base illisible y faisait remonter une
     * `SQLiteCantOpenDatabaseException` brute jusqu'au scope du réconciliateur : le processus
     * plantait à chaque démarrage — boucle de plantage — et l'écran de diagnostic promis par §18
     * n'était jamais atteint. Aucun test ne pouvait le voir : les doublures de Room ne levaient
     * jamais, et le harnais du coordinateur simule l'échec sur `gateway.load()`, pas sur la fusion.
     */
    @Test
    fun anUnreadableRoomIsReportedWithoutLettingTheExceptionEscape() =
        runTest {
            val fixture = MergerFixture(roomFailure = SessionStoreUnreadableException("SQLITE_CANTOPEN"))
            fixture.seedProjection(revision = 3)

            val outcome = fixture.merger.merge()

            assertThat(outcome).isEqualTo(DirectBootMergeOutcome.RoomUnreadable("SQLITE_CANTOPEN"))
            assertThat(fixture.directBootStore.writeCount).isEqualTo(0)
        }

    @Test
    fun aLockedDeviceMergesNothingAndNeverTouchesRoom() =
        runTest {
            val fixture = MergerFixture(unlocked = false)
            fixture.seedProjection(revision = 3)

            val outcome = fixture.merger.merge()

            assertThat(outcome).isEqualTo(DirectBootMergeOutcome.NothingToMerge)
            assertThat(fixture.roomMerge.merged).isEmpty()
            assertThat(fixture.directBootStore.writeCount).isEqualTo(0)
        }

    @Test
    fun anAbsentProjectionMergesNothing() =
        runTest {
            val fixture = MergerFixture()

            val outcome = fixture.merger.merge()

            assertThat(outcome).isEqualTo(DirectBootMergeOutcome.NothingToMerge)
            assertThat(fixture.roomMerge.merged).isEmpty()
        }

    /**
     * SPEC_CORE_KMP §13 : « corruption traitée explicitement », « aucune suppression silencieuse ».
     * Le fichier reste en place — `SessionReconciler` produira `SnapshotCorrupted` juste après.
     */
    @Test
    fun aCorruptedProjectionIsReportedWithoutMergingOrErasingAnything() =
        runTest {
            val fixture = MergerFixture()
            fixture.directBootStore.seed(DirectBootSnapshot.Corrupted("DIRECT_BOOT_MALFORMED_JSON"))

            val outcome = fixture.merger.merge()

            assertThat(outcome).isEqualTo(DirectBootMergeOutcome.Corrupted("DIRECT_BOOT_MALFORMED_JSON"))
            assertThat(fixture.roomMerge.merged).isEmpty()
            assertThat(fixture.directBootStore.clearCount).isEqualTo(0)
            assertThat(fixture.directBootStore.writeCount).isEqualTo(0)
        }

    @Test
    fun anActiveProjectionIsHandedToTheRoomTransaction() =
        runTest {
            val fixture = MergerFixture()
            fixture.seedProjection(revision = 4)

            fixture.merger.merge()

            assertThat(fixture.roomMerge.merged.map { it.domainRevision }).containsExactly(4L)
            assertThat(
                fixture.roomMerge.merged
                    .single()
                    .sessionId,
            ).isEqualTo(SessionDtoFixtures.SESSION_ID)
        }

    /**
     * §9.2 : Room fait foi. Une projection refusée pour révision inférieure est précisément celle
     * qu'il faut remettre à niveau, pas celle qu'il faut garder.
     */
    @Test
    fun aStaleProjectionIsRewrittenFromRoom() =
        runTest {
            val fixture = MergerFixture()
            fixture.seedProjection(revision = 2)
            fixture.seedRoom(revision = 7, state = SessionStateDto.TRIGGERED_AWAITING_NFC)
            fixture.roomMerge.result = DirectBootMergeResult.StaleRevision

            val outcome = fixture.merger.merge()

            assertThat(outcome).isEqualTo(DirectBootMergeOutcome.Merged(DirectBootMergeResult.StaleRevision))
            val rewritten = fixture.directBootStore.read() as DirectBootSnapshot.Active
            assertThat(rewritten.domainRevision).isEqualTo(7L)
            assertThat(rewritten.state).isEqualTo(SessionStateDto.TRIGGERED_AWAITING_NFC)
        }

    /**
     * Room absorbe la projection, puis la réécrit : les deux convergent en une passe, et un effet
     * resté rejouable en Direct Boot se retrouve dans la projection réécrite pour être rejoué par la
     * réconciliation qui suit.
     */
    @Test
    fun afterMergingTheProjectionIsRewrittenFromRoom() =
        runTest {
            val fixture = MergerFixture()
            fixture.seedProjection(revision = 5)
            fixture.seedRoom(
                revision = 5,
                state = SessionStateDto.TRIGGERED_AWAITING_NFC,
                effects =
                    listOf(
                        PendingEffect(
                            effectId = "${SessionDtoFixtures.SESSION_ID}:5:RECORD_INCIDENT:0",
                            sessionId = SessionDtoFixtures.SESSION_ID,
                            revision = 5,
                            kind = SessionEffectKindDto.RECORD_INCIDENT,
                            ordinal = 0,
                            payloadJson = null,
                            status = EffectStatus.PENDING,
                            lastError = null,
                        ),
                    ),
            )

            fixture.merger.merge()

            val rewritten = fixture.directBootStore.read() as DirectBootSnapshot.Active
            assertThat(rewritten.pendingEffects.map { it.kind })
                .containsExactly(SessionEffectKindDto.RECORD_INCIDENT)
        }

    /**
     * Room n'a pas la session : rien n'est écrit et surtout rien n'est effacé — la projection
     * orpheline reste lisible pour un diagnostic.
     */
    @Test
    fun anUnknownSessionLeavesTheProjectionUntouched() =
        runTest {
            val fixture = MergerFixture()
            fixture.seedProjection(revision = 3)
            fixture.roomMerge.result = DirectBootMergeResult.UnknownSession

            fixture.merger.merge()

            assertThat(fixture.directBootStore.clearCount).isEqualTo(0)
            val kept = fixture.directBootStore.read() as DirectBootSnapshot.Active
            assertThat(kept.domainRevision).isEqualTo(3L)
        }

    /** Rejouer la fusion ne change rien : c'est le prérequis des trois déclencheurs de §9.3. */
    @Test
    fun mergingTwiceIsIdempotent() =
        runTest {
            val fixture = MergerFixture()
            fixture.seedProjection(revision = 6)
            fixture.seedRoom(revision = 6, state = SessionStateDto.TRIGGERED_AWAITING_NFC)

            fixture.merger.merge()
            val afterFirst = fixture.directBootStore.read()
            fixture.merger.merge()

            assertThat(fixture.directBootStore.read()).isEqualTo(afterFirst)
            assertThat(fixture.roomMerge.merged).hasSize(2)
        }
}

private class MergerFixture(
    unlocked: Boolean = true,
    roomFailure: SessionStoreUnreadableException? = null,
) {
    val unlockState = FakeUnlockState(isUserUnlocked = unlocked)
    val directBootStore = InMemoryDirectBootStore()
    val sessionStore = InMemorySessionStore()
    val roomMerge = RecordingDirectBootRoomMerge(failure = roomFailure)
    val merger = DirectBootMerger(unlockState, directBootStore, sessionStore, roomMerge)

    fun seedProjection(
        revision: Long,
        state: SessionStateDto = SessionStateDto.TRIGGERED_AWAITING_NFC,
        receipts: List<EventReceipt> = emptyList(),
        effects: List<PendingEffect> = emptyList(),
    ) {
        directBootStore.seed(
            DirectBootMapper.projectionOf(
                snapshot = SessionDtoFixtures.snapshotInState(state, revision = revision),
                extras = SessionDtoFixtures.extras(),
                receipts = receipts,
                effects = effects,
            ),
        )
    }

    fun seedRoom(
        revision: Long,
        state: SessionStateDto,
        receipts: List<EventReceipt> = emptyList(),
        effects: List<PendingEffect> = emptyList(),
    ) {
        sessionStore.seed(
            snapshot = SessionDtoFixtures.snapshotInState(state, revision = revision),
            extras = SessionDtoFixtures.extras(),
            receipts = receipts,
            effects = effects,
        )
    }
}
