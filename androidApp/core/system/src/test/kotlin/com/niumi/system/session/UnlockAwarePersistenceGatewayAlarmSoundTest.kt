package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.AlarmSoundStoreResult
import com.niumi.database.SessionAlarmSoundStore
import com.niumi.database.directboot.DirectBootSnapshot
import com.niumi.system.boot.fakes.FakeUnlockState
import com.niumi.system.boot.fakes.InMemoryDirectBootStore
import com.niumi.system.boot.fakes.InMemorySessionStore
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.SessionDtoFixtures.SESSION_ID
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Lot 7, SPEC_ANDROID §7.3 et §10.2 : une sonnerie modifiée en `ARMED` est recopiée dans la
 * projection Direct Boot, sans quoi un redémarrage avant déverrouillage sonnerait l'ancien choix.
 */
class UnlockAwarePersistenceGatewayAlarmSoundTest {
    private val sessionStore = InMemorySessionStore()
    private val directBootStore = InMemoryDirectBootStore()
    private val unlockState = FakeUnlockState(isUserUnlocked = true)
    private val armed = SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED)

    /** Simule Room : l'écriture réussie se voit dans la session active relue ensuite. */
    private var storeResult: AlarmSoundStoreResult = AlarmSoundStoreResult.Updated
    private var storeCalls = 0
    private val alarmSoundStore =
        SessionAlarmSoundStore { _, ringtoneKey, volumeRampSeconds ->
            storeCalls++
            if (storeResult == AlarmSoundStoreResult.Updated) {
                val updated =
                    SessionDtoFixtures.extras(
                        ringtoneKey = ringtoneKey,
                        volumeRampSeconds = volumeRampSeconds,
                    )
                sessionStore.seed(armed, updated)
            }
            storeResult
        }

    private val gateway = UnlockAwarePersistenceGateway(sessionStore, directBootStore, unlockState, alarmSoundStore)

    init {
        sessionStore.seed(armed, SessionDtoFixtures.extras(ringtoneKey = "niumi_piano", volumeRampSeconds = 120))
    }

    @Test
    fun anUpdateIsMirroredToDirectBoot() =
        runTest {
            val result = gateway.updateAlarmSound(SESSION_ID, "niumi_oiseaux", 30)

            assertThat(result).isEqualTo(AlarmSoundUpdateResult.Updated)
            val projection = directBootStore.read() as DirectBootSnapshot.Active
            assertThat(projection.ringtoneKey).isEqualTo("niumi_oiseaux")
            assertThat(projection.volumeRampSeconds).isEqualTo(30)
        }

    @Test
    fun aRefusedUpdateLeavesDirectBootUntouched() =
        runTest {
            storeResult = AlarmSoundStoreResult.NotArmed

            val result = gateway.updateAlarmSound(SESSION_ID, "niumi_oiseaux", 30)

            assertThat(result).isEqualTo(AlarmSoundUpdateResult.NotArmed)
            assertThat(directBootStore.read()).isNull()
        }

    @Test
    fun beforeUnlockNothingIsWrittenAnywhere() =
        runTest {
            unlockState.isUserUnlocked = false

            val result = gateway.updateAlarmSound(SESSION_ID, "niumi_oiseaux", 30)

            assertThat(result).isEqualTo(AlarmSoundUpdateResult.DeferredUntilUnlock)
            assertThat(storeCalls).isEqualTo(0)
            assertThat(directBootStore.read()).isNull()
        }
}
