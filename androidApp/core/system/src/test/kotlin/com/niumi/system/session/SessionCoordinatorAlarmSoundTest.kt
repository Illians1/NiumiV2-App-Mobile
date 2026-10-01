package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Lot 7, SPEC_ANDROID §3 et §10.2 : sonnerie et montée restent modifiables sans scan tant que la
 * session est `ARMED`, et seulement alors. Aucun événement KMP, aucune révision.
 */
class SessionCoordinatorAlarmSoundTest {
    private val harness = TestCoordinatorHarness()

    private suspend fun armSession(): SessionSnapshotDto {
        val result =
            harness.coordinator.dispatch(
                SessionDtoFixtures.activationRequested(eventId = "00000000-0000-0000-0000-000000000001"),
                SessionDtoFixtures.extras(ringtoneKey = NiumiRingtones.DEFAULT_KEY, volumeRampSeconds = 120),
            )
        return (result as DispatchResult.Applied).snapshot!!
    }

    private suspend fun present() = harness.gateway.load() as LoadResult.Present

    @Test
    fun anArmedSessionTakesTheNewRingtoneAndRampWithoutANewRevision() =
        runTest {
            val armed = armSession()
            val reducerCalls = harness.recordingReducer.callCount

            val result = harness.coordinator.updateAlarmSound("niumi_oiseaux", 300)

            assertThat(result).isEqualTo(AlarmSoundUpdateResult.Updated)
            assertThat(present().extras.ringtoneKey).isEqualTo("niumi_oiseaux")
            assertThat(present().extras.volumeRampSeconds).isEqualTo(300)
            assertThat(present().snapshot.revision).isEqualTo(armed.revision)
            assertThat(harness.recordingReducer.callCount).isEqualTo(reducerCalls)
        }

    @Test
    fun theRampCanBeTurnedOff() =
        runTest {
            armSession()

            harness.coordinator.updateAlarmSound("niumi_bell", null)

            assertThat(present().extras.volumeRampSeconds).isNull()
        }

    @Test
    fun aRingingSessionKeepsItsSound() =
        runTest {
            val armed = armSession()
            harness.coordinator.dispatch(harness.eventFactory.alarmFired(armed))

            val result = harness.coordinator.updateAlarmSound("niumi_oiseaux", null)

            assertThat(result).isEqualTo(AlarmSoundUpdateResult.NotArmed)
            assertThat(present().extras.ringtoneKey).isEqualTo(NiumiRingtones.DEFAULT_KEY)
            assertThat(present().extras.volumeRampSeconds).isEqualTo(120)
        }

    @Test
    fun aRingtoneOutsideTheCatalogIsRefusedBeforeAnyWrite() =
        runTest {
            armSession()

            val result = harness.coordinator.updateAlarmSound(NiumiRingtones.LEGACY_KEY, null)

            assertThat(result).isEqualTo(AlarmSoundUpdateResult.UnknownRingtone)
            assertThat(harness.journal.calls).doesNotContain("gateway.updateAlarmSound")
        }

    @Test
    fun aRampDurationOutsideTheListIsRefusedBeforeAnyWrite() =
        runTest {
            armSession()

            val result = harness.coordinator.updateAlarmSound("niumi_bell", 45)

            assertThat(result).isEqualTo(AlarmSoundUpdateResult.InvalidRampDuration)
            assertThat(harness.journal.calls).doesNotContain("gateway.updateAlarmSound")
        }

    @Test
    fun withoutASessionThereIsNothingToChange() =
        runTest {
            val result = harness.coordinator.updateAlarmSound("niumi_bell", null)

            assertThat(result).isEqualTo(AlarmSoundUpdateResult.NoActiveSession)
        }

    @Test
    fun aLockedDeviceWritesNothing() =
        runTest {
            armSession()
            harness.gateway.locked = true

            val result = harness.coordinator.updateAlarmSound("niumi_oiseaux", null)

            assertThat(result).isEqualTo(AlarmSoundUpdateResult.DeferredUntilUnlock)
            assertThat(present().extras.ringtoneKey).isEqualTo(NiumiRingtones.DEFAULT_KEY)
        }

    @Test
    fun aDecisionAfterTheChangeKeepsTheNewSound() =
        runTest {
            val armed = armSession()
            harness.coordinator.updateAlarmSound("niumi_oiseaux", 30)

            harness.coordinator.dispatch(harness.eventFactory.alarmFired(armed))

            assertThat(present().extras.ringtoneKey).isEqualTo("niumi_oiseaux")
            assertThat(present().extras.volumeRampSeconds).isEqualTo(30)
        }
}
