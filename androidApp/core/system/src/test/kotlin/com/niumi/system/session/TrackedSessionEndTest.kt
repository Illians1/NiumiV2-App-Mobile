package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.fakes.SessionDtoFixtures
import org.junit.Test

/** Étape 46 : fin de la session suivie par l'écran de réveil et la feuille d'annulation. */
class TrackedSessionEndTest {
    private val tracked = SessionDtoFixtures.SESSION_ID

    private fun snapshot(
        state: SessionStateDto,
        sessionId: String = tracked,
    ) = SessionDtoFixtures.snapshotInState(state, sessionId = sessionId)

    @Test
    fun theTrackedSessionIsReadAgainWhenAnotherSessionIsPublished() {
        assertThat(
            needsTrackedSessionRead(tracked, snapshot(SessionStateDto.ARMED, SessionDtoFixtures.OTHER_SESSION_ID)),
        ).isTrue()
    }

    @Test
    fun theTrackedSessionIsReadAgainWhenNothingIsPublishedAnyMore() {
        assertThat(needsTrackedSessionRead(tracked, null)).isTrue()
    }

    @Test
    fun theTrackedSessionIsReadAgainWhenItIsPublishedFinal() {
        assertThat(needsTrackedSessionRead(tracked, snapshot(SessionStateDto.CANCELLED))).isTrue()
    }

    @Test
    fun theTrackedSessionStillRunningNeedsNoRead() {
        assertThat(needsTrackedSessionRead(tracked, snapshot(SessionStateDto.RELEASING))).isFalse()
        assertThat(needsTrackedSessionRead(tracked, snapshot(SessionStateDto.RINGING))).isFalse()
    }

    @Test
    fun aCompletedSessionEndsOnTheSuccessScreenWithItsIdentifier() {
        assertThat(trackedSessionEndOf(snapshot(SessionStateDto.COMPLETED)))
            .isEqualTo(TrackedSessionEnd.Completed(tracked))
    }

    @Test
    fun aCancelledSessionEndsCancelled() {
        assertThat(trackedSessionEndOf(snapshot(SessionStateDto.CANCELLED))).isEqualTo(TrackedSessionEnd.Cancelled)
    }

    @Test
    fun aDisarmedOrFailedSessionEndsElsewhere() {
        assertThat(trackedSessionEndOf(snapshot(SessionStateDto.DISARMED))).isEqualTo(TrackedSessionEnd.Other)
        assertThat(trackedSessionEndOf(snapshot(SessionStateDto.FAILED))).isEqualTo(TrackedSessionEnd.Other)
    }

    /** `Accepted` seul ne conclut jamais (SPEC_ANDROID §11.3) : seul un état final relu compte. */
    @Test
    fun aSessionNotYetFinalOrNotFoundHasNoEnd() {
        assertThat(trackedSessionEndOf(snapshot(SessionStateDto.RELEASING))).isNull()
        assertThat(trackedSessionEndOf(null)).isNull()
    }
}
