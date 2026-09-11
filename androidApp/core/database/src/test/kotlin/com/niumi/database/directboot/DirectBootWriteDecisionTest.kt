package com.niumi.database.directboot

import com.google.common.truth.Truth.assertThat
import com.niumi.database.mapping.SessionSnapshotDtoFixtures
import org.junit.Test

/**
 * Politique de révision de [FileDirectBootStore], extraite dans [decideWrite] pour être testée
 * sans dépendance Android (SPEC_CORE_KMP §13 : « une révision inférieure est refusée »).
 */
class DirectBootWriteDecisionTest {
    private val extras = SessionSnapshotDtoFixtures.extras()

    private fun activeAt(
        sessionId: String,
        revision: Long,
    ) = DirectBootMapper.projectionOf(
        SessionSnapshotDtoFixtures.preparingSnapshot(sessionId, revision),
        extras,
        emptyList(),
        emptyList(),
    )

    @Test
    fun `no existing snapshot accepts the write`() {
        val candidate = activeAt("11111111-1111-1111-1111-111111111111", revision = 1)

        assertThat(decideWrite(existing = null, candidate)).isEqualTo(DirectBootWriteResult.Written)
    }

    @Test
    fun `lower revision on the same session is refused`() {
        val existing = activeAt("11111111-1111-1111-1111-111111111111", revision = 5)
        val candidate = activeAt("11111111-1111-1111-1111-111111111111", revision = 4)

        assertThat(decideWrite(existing, candidate)).isEqualTo(DirectBootWriteResult.StaleRevision)
    }

    @Test
    fun `equal revision on the same session is idempotent`() {
        val existing = activeAt("11111111-1111-1111-1111-111111111111", revision = 5)
        val candidate = activeAt("11111111-1111-1111-1111-111111111111", revision = 5)

        assertThat(decideWrite(existing, candidate)).isEqualTo(DirectBootWriteResult.Written)
    }

    @Test
    fun `higher revision on the same session is accepted`() {
        val existing = activeAt("11111111-1111-1111-1111-111111111111", revision = 5)
        val candidate = activeAt("11111111-1111-1111-1111-111111111111", revision = 6)

        assertThat(decideWrite(existing, candidate)).isEqualTo(DirectBootWriteResult.Written)
    }

    @Test
    fun `lower revision on a different session is accepted (revision guard is scoped per session)`() {
        val existing = activeAt("11111111-1111-1111-1111-111111111111", revision = 5)
        val candidate = activeAt("22222222-2222-2222-2222-222222222222", revision = 1)

        assertThat(decideWrite(existing, candidate)).isEqualTo(DirectBootWriteResult.Written)
    }

    @Test
    fun `a corrupted existing snapshot never blocks a valid write`() {
        val candidate = activeAt("11111111-1111-1111-1111-111111111111", revision = 1)

        assertThat(decideWrite(DirectBootSnapshot.Corrupted("truncated"), candidate))
            .isEqualTo(DirectBootWriteResult.Written)
    }
}
