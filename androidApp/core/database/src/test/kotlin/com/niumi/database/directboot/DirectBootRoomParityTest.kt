package com.niumi.database.directboot

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.SessionState
import com.niumi.database.mapping.SessionSnapshotDtoFixtures
import com.niumi.database.mapping.toEntity
import com.niumi.database.mapping.toSnapshotDto
import org.junit.Test

/**
 * Les projections Room et Direct Boot partagent la même source (`SessionSnapshotDto`,
 * `AndroidSessionExtras`) sans partager de code (`ETAPE-10.md`, écart 4 : deux mappers
 * indépendants). Ce test attrape une divergence de mise à plat entre les deux sans les coupler :
 * depuis le même snapshot, les deux allers-retours doivent rendre le même `SessionSnapshotDto`.
 */
class DirectBootRoomParityTest {
    @Test
    fun `Room and Direct Boot projections round trip to the same snapshot`() {
        val extras = SessionSnapshotDtoFixtures.extras()
        val snapshot =
            SessionSnapshotDtoFixtures.fullyPopulatedSnapshot(
                state = SessionState.ARMED,
                releaseTarget = null,
            )

        val viaRoom = snapshot.toEntity(extras).toSnapshotDto()
        val viaDirectBoot = DirectBootMapper.projectionOf(snapshot, extras, emptyList(), emptyList()).toSnapshotDto()

        assertThat(viaDirectBoot).isEqualTo(viaRoom)
        assertThat(viaDirectBoot).isEqualTo(snapshot)
    }
}
