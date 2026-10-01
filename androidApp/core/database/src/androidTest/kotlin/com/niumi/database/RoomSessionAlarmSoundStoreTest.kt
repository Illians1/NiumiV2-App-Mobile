package com.niumi.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.directboot.DirectBootSnapshot
import com.niumi.database.directboot.FileDirectBootStore
import com.niumi.database.directboot.UnlockState
import com.niumi.database.directboot.mirrorActiveSessionToDirectBoot
import com.niumi.database.directboot.toExtras
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * Lot 7, SPEC_ANDROID §3, §7.2, §7.3 : sonnerie et montée d'une session `ARMED` se modifient dans
 * Room, se relisent dans la projection Direct Boot recopiée ensuite, et ne se modifient plus hors
 * de `ARMED`. Instrumenté : Room refuse toute requête hors d'un appareil.
 */
@RunWith(AndroidJUnit4::class)
class RoomSessionAlarmSoundStoreTest {
    private lateinit var database: NiumiDatabase
    private lateinit var sessionStore: RoomSessionStore
    private lateinit var alarmSoundStore: RoomSessionAlarmSoundStore
    private val directBootStore =
        FileDirectBootStore(InstrumentationRegistry.getInstrumentation().targetContext)

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        sessionStore = RoomSessionStore(Provider { database }, AlwaysUnlockedState)
        alarmSoundStore = RoomSessionAlarmSoundStore(Provider { database }, AlwaysUnlockedState)
        directBootStore.clear()
    }

    @After
    fun tearDown() {
        database.close()
        directBootStore.clear()
    }

    private suspend fun seed(state: SessionStateDto) {
        sessionStore.commitDecision(
            StoredDecision(
                RoomTestFixtures.preparingSnapshot(SESSION_ID).copy(state = state),
                RoomTestFixtures.receipt("event-1", SESSION_ID),
                emptyList(),
                RoomTestFixtures.extras(ringtoneKey = "niumi_piano", volumeRampSeconds = 120),
            ),
        )
    }

    @Test
    fun anArmedSessionTakesTheNewSoundInRoomAndInTheMirroredProjection() =
        runTest {
            seed(SessionStateDto.ARMED)

            val result = alarmSoundStore.update(SESSION_ID, "niumi_oiseaux", 30)
            mirrorActiveSessionToDirectBoot(sessionStore, directBootStore)

            assertThat(result).isEqualTo(AlarmSoundStoreResult.Updated)
            val extras = requireNotNull(sessionStore.activeSession()).extras
            assertThat(extras.ringtoneKey).isEqualTo("niumi_oiseaux")
            assertThat(extras.volumeRampSeconds).isEqualTo(30)
            val projection = directBootStore.read() as DirectBootSnapshot.Active
            assertThat(projection.toExtras().ringtoneKey).isEqualTo("niumi_oiseaux")
            assertThat(projection.toExtras().volumeRampSeconds).isEqualTo(30)
        }

    @Test
    fun onlyTheSoundColumnsChange() =
        runTest {
            seed(SessionStateDto.ARMED)
            val before = requireNotNull(sessionStore.activeSession())

            alarmSoundStore.update(SESSION_ID, "niumi_bell", null)

            val after = requireNotNull(sessionStore.activeSession())
            assertThat(after.snapshot).isEqualTo(before.snapshot)
            assertThat(after.extras).isEqualTo(before.extras.copy(ringtoneKey = "niumi_bell", volumeRampSeconds = null))
        }

    @Test
    fun aRingingSessionKeepsItsSound() =
        runTest {
            seed(SessionStateDto.RINGING)

            val result = alarmSoundStore.update(SESSION_ID, "niumi_oiseaux", 30)

            assertThat(result).isEqualTo(AlarmSoundStoreResult.NotArmed)
            assertThat(requireNotNull(sessionStore.activeSession()).extras.ringtoneKey).isEqualTo("niumi_piano")
        }

    @Test
    fun anotherSessionIsNotTouched() =
        runTest {
            seed(SessionStateDto.ARMED)

            val result = alarmSoundStore.update("99999999-9999-9999-9999-999999999999", "niumi_oiseaux", 30)

            assertThat(result).isEqualTo(AlarmSoundStoreResult.NoActiveSession)
            assertThat(requireNotNull(sessionStore.activeSession()).extras.ringtoneKey).isEqualTo("niumi_piano")
        }

    @Test
    fun theDatabaseIsNeverOpenedBeforeUnlock() {
        var resolved = false
        val locked =
            RoomSessionAlarmSoundStore(
                Provider {
                    resolved = true
                    database
                },
                object : UnlockState {
                    override val isUserUnlocked: Boolean = false
                },
            )

        assertThrows(IllegalStateException::class.java) {
            runBlocking { locked.update(SESSION_ID, "niumi_bell", null) }
        }
        assertThat(resolved).isFalse()
    }

    private companion object {
        const val SESSION_ID = "11111111-1111-1111-1111-111111111111"
    }
}
