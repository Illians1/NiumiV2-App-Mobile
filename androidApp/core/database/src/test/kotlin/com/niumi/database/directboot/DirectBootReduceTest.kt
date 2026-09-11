package com.niumi.database.directboot

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.SessionEventKind
import com.niumi.core.domain.SessionState
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionEventDto
import com.niumi.database.mapping.SessionSnapshotDtoFixtures
import org.junit.Test

/**
 * Exigence explicite du « Terminé quand » de l'étape 10 : un snapshot Direct Boot actif se
 * convertit en `SessionSnapshotDto` accepté par `NiumiCoreFacade.reduce` — preuve que la
 * projection suffit à appeler KMP avant le premier déverrouillage (SPEC_CORE_KMP §13).
 */
class DirectBootReduceTest {
    @Test
    fun `snapshot reconstructed from an active projection is accepted by reduce`() {
        val armedSnapshot =
            SessionSnapshotDtoFixtures.preparingSnapshot().copy(
                state = SessionState.ARMED,
                armedAtEpochMillis = 1_700_000_001_000L,
            )
        val active =
            DirectBootMapper.projectionOf(
                armedSnapshot,
                SessionSnapshotDtoFixtures.extras(),
                emptyList(),
                emptyList(),
            )
        val reconstructed = active.toSnapshotDto()

        val decision =
            NiumiCoreFacade().reduce(
                reconstructed,
                SessionEventDto(
                    eventId = "33333333-3333-3333-3333-333333333333",
                    sessionId = reconstructed.sessionId,
                    kind = SessionEventKind.ALARM_FIRED,
                    occurredAtEpochMillis = 1_700_000_010_000L,
                    expectedRevision = reconstructed.revision,
                    activationRequest = null,
                    failureCode = null,
                    incident = null,
                ),
            )

        assertThat(decision.violations).isEmpty()
    }
}
