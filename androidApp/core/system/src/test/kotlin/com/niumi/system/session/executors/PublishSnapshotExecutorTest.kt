package com.niumi.system.session.executors

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EffectStatus
import com.niumi.database.PendingEffect
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.fakes.FakeTechnicalEventLog
import com.niumi.system.session.fakes.SessionDtoFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Les événements d'état de SPEC_ANDROID §17 sont écrits **une fois par transition**, pas à chaque
 * publication. Mesuré le 2026-09-29 sur Xiaomi 25080RABDG / Android 16 : un incident reçu pendant
 * `ARMED` publie le snapshot sans changer d'état (SPEC_CORE_KMP §6), et `SESSION_ARMED` était
 * journalisé une seconde fois, comme si la session avait été réarmée.
 */
class PublishSnapshotExecutorTest {
    private val publisher = SessionSnapshotPublisher()
    private val technicalEventLog = FakeTechnicalEventLog()
    private val executor = PublishSnapshotExecutor(publisher, technicalEventLog)
    private val extras = SessionDtoFixtures.extras()

    @Test
    fun theFirstPublicationOfAStateIsJournaled() =
        runTest {
            publish(armed())

            assertThat(technicalEventLog.logged).containsExactly(TechnicalEventType.SESSION_ARMED)
        }

    /** Le défaut mesuré : `INCIDENT_REPORTED` republie `ARMED` sans transition. */
    @Test
    fun aRepublicationWithoutAnyStateChangeIsNotJournaledAgain() =
        runTest {
            publish(armed(revision = 2))
            publish(armed(revision = 3))

            assertThat(technicalEventLog.logged).containsExactly(TechnicalEventType.SESSION_ARMED)
            assertThat(publisher.snapshot.value?.revision).isEqualTo(3)
        }

    @Test
    fun aRealTransitionIsJournaled() =
        runTest {
            publish(SessionDtoFixtures.snapshotInState(SessionStateDto.PREPARING))
            publish(armed())

            assertThat(technicalEventLog.logged)
                .containsExactly(TechnicalEventType.SESSION_PREPARING, TechnicalEventType.SESSION_ARMED)
                .inOrder()
        }

    /** Une nouvelle session qui arrive dans le même état que la précédente est un autre fait. */
    @Test
    fun anotherSessionInTheSameStateIsJournaled() =
        runTest {
            publish(armed())
            publish(armed(sessionId = SessionDtoFixtures.OTHER_SESSION_ID))

            assertThat(technicalEventLog.logged)
                .containsExactly(TechnicalEventType.SESSION_ARMED, TechnicalEventType.SESSION_ARMED)
        }

    private fun armed(
        sessionId: String = SessionDtoFixtures.SESSION_ID,
        revision: Long = 2,
    ) = SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED, sessionId, revision)

    private suspend fun publish(snapshot: SessionSnapshotDto) {
        executor.execute(publishEffect(snapshot), snapshot, extras)
    }

    private fun publishEffect(snapshot: SessionSnapshotDto) =
        PendingEffect(
            effectId = "${snapshot.sessionId}:${snapshot.revision}:PUBLISH_PLATFORM_SNAPSHOT:0",
            sessionId = snapshot.sessionId,
            revision = snapshot.revision,
            kind = SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT,
            ordinal = 0,
            payloadJson = null,
            status = EffectStatus.PENDING,
            lastError = null,
        )
}
