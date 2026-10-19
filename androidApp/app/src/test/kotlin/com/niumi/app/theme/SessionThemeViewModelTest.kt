package com.niumi.app.theme

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.StoredDecision
import com.niumi.system.common.OperationResult
import com.niumi.system.session.AlarmSoundUpdateResult
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.SessionThemeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Passerelle dont la lecture n'aboutit qu'à [answer] : le thème reste inconnu jusque-là. */
private class DeferredLoadGateway : SessionPersistenceGateway {
    val answer = CompletableDeferred<LoadResult>()

    override suspend fun load(): LoadResult = answer.await()

    override suspend fun commit(decision: StoredDecision) = error("COMMIT_FROM_THEME")

    override suspend fun receipt(eventId: String): EventReceipt? = error("RECEIPT_FROM_THEME")

    override suspend fun pendingEffects(sessionId: String): List<PendingEffect> = error("EFFECTS_FROM_THEME")

    override suspend fun markEffect(
        effectId: String,
        status: EffectStatus,
        error: String?,
    ) = error("MARK_EFFECT_FROM_THEME")

    override suspend fun clearActive(sessionId: String) = error("CLEAR_ACTIVE_FROM_THEME")

    override suspend fun recordIncident(
        sessionId: String,
        incident: SessionIncidentDto,
    ): OperationResult = error("RECORD_INCIDENT_FROM_THEME")

    override suspend fun updateAlarmSound(
        sessionId: String,
        ringtoneKey: String,
        volumeRampSeconds: Int?,
    ): AlarmSoundUpdateResult = error("UPDATE_ALARM_SOUND_FROM_THEME")
}

/**
 * Thème de `MainActivity` (étape 33) : inconnu (`null`) tant que la persistance n'a pas répondu —
 * l'activité ne compose rien plutôt que d'afficher un écran clair qui virerait au sombre — puis
 * celui de la session, et ses changements.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionThemeViewModelTest {
    private val gateway = DeferredLoadGateway()
    private val publisher = SessionSnapshotPublisher()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SessionThemeViewModel(SessionThemeSource(gateway, publisher))

    private fun snapshot(state: SessionStateDto) =
        SessionSnapshotDto(
            schemaVersion = 2,
            revision = 2,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule = WakeScheduleDto("2026-09-13", "07:00", "Europe/Paris", 2_000_000_000_000L),
            state = state,
            releaseTarget = null,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = 1_000L,
            armedAtEpochMillis = 1_100L,
            blockingAppliedAtEpochMillis = 1_100L,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
        )

    @Test
    fun theThemeIsUnknownUntilThePersistenceAnswers() =
        runTest {
            val viewModel = viewModel()

            assertThat(viewModel.dark.value).isNull()

            gateway.answer.complete(LoadResult.Present(snapshot(SessionStateDto.ARMED), extras(), emptyList()))

            assertThat(viewModel.dark.value).isTrue()
        }

    @Test
    fun theThemeFollowsTheEndOfTheSession() =
        runTest {
            gateway.answer.complete(LoadResult.Absent)
            val viewModel = viewModel()
            assertThat(viewModel.dark.value).isFalse()

            publisher.publish(snapshot(SessionStateDto.RINGING))
            assertThat(viewModel.dark.value).isTrue()

            publisher.publish(snapshot(SessionStateDto.COMPLETED))
            assertThat(viewModel.dark.value).isFalse()
        }

    private fun extras() =
        AndroidSessionExtras(
            boxes = emptyList(),
            ringtoneKey = "niumi_default",
            vibrationEnabled = true,
            volumeRampSeconds = null,
            blockedPackages = emptyList(),
        )
}
