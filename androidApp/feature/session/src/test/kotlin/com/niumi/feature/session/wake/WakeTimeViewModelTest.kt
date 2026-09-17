package com.niumi.feature.session.wake

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.feature.session.wake.fakes.FakeClock
import com.niumi.feature.session.wake.fakes.FakeSetupPreferences
import com.niumi.feature.session.wake.fakes.FakeTimeZoneProvider
import com.niumi.system.session.SessionSnapshotPublisher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Écran 5 (SPEC_ANDROID §15 ; SPEC_CORE_KMP §8.1). Le calcul lui-même est prouvé dans
 * `:shared:core` (`WakeScheduleCalculatorTest`) : ces tests portent sur ce que le ViewModel en
 * fait — affichage, message, garde de session, mémorisation de la dernière heure choisie.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WakeTimeViewModelTest {
    private fun paris(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long =
        ZonedDateTime
            .of(year, month, day, hour, minute, 0, 0, ZoneId.of("Europe/Paris"))
            .toInstant()
            .toEpochMilli()

    private val clock = FakeClock(now = paris(2026, 9, 3, 20, 0))
    private val timeZoneProvider = FakeTimeZoneProvider(zoneId = "Europe/Paris")
    private val snapshotPublisher = SessionSnapshotPublisher()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(setupPreferences: FakeSetupPreferences = FakeSetupPreferences()): WakeTimeViewModel =
        WakeTimeViewModel(NiumiCoreFacade(), clock, timeZoneProvider, setupPreferences, snapshotPublisher)

    @Test
    fun aTimeChosenBeforeTheCurrentHourIsScheduledForTomorrow() {
        val viewModel = viewModel()

        viewModel.onTimeChanged(7, 0)

        assertThat(viewModel.state.display?.relativeDayLabel).isEqualTo("Demain")
    }

    @Test
    fun aTimeChosenAfterTheCurrentHourIsScheduledForToday() {
        val viewModel = viewModel()

        viewModel.onTimeChanged(23, 0)

        assertThat(viewModel.state.display?.relativeDayLabel).isEqualTo("Aujourd'hui")
    }

    @Test
    fun aTimeExactlyEqualToTheCurrentInstantIsScheduledForTomorrow() {
        clock.now = paris(2026, 9, 4, 7, 0)
        val viewModel = viewModel()

        viewModel.onTimeChanged(7, 0)

        assertThat(viewModel.state.display?.relativeDayLabel).isEqualTo("Demain")
    }

    @Test
    fun onTimeChangedProducesATwoDigitIsoLocalTime() {
        val viewModel = viewModel()

        viewModel.onTimeChanged(7, 5)

        assertThat(viewModel.state.localTimeIso).isEqualTo("07:05")
    }

    @Test
    fun aSystemTimeZoneChangeIsPickedUpOnRefresh() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)
        val before = viewModel.state.display?.let { it.timeLabel to it.zoneLabel }

        timeZoneProvider.zoneId = "Pacific/Auckland"
        viewModel.refresh(use24Hour = true)

        assertThat(viewModel.state.display?.zoneLabel).isEqualTo("Pacific/Auckland")
        assertThat(viewModel.state.display?.let { it.timeLabel to it.zoneLabel }).isNotEqualTo(before)
    }

    @Test
    fun anUnknownTimeZoneShowsAMessageAndDisablesContinuationWithoutThrowing() {
        timeZoneProvider.zoneId = "Not/AZone"
        val viewModel = viewModel()

        viewModel.onTimeChanged(7, 0)

        assertThat(viewModel.state.display).isNull()
        assertThat(viewModel.state.message).isEqualTo(WakeTimeTexts.UNKNOWN_ZONE_MESSAGE)
        assertThat(viewModel.state.canContinue).isFalse()
    }

    @Test
    fun aSessionInProgressDisablesContinuation() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)
        assertThat(viewModel.state.canContinue).isTrue()

        snapshotPublisher.publish(armedSnapshot())

        assertThat(viewModel.state.isSessionInProgress).isTrue()
        assertThat(viewModel.state.canContinue).isFalse()
    }

    @Test
    fun theDisplayedScheduleIsNeverNullWhenContinuationIsAllowed() {
        val viewModel = viewModel()

        viewModel.onTimeChanged(7, 0)

        if (viewModel.state.canContinue) {
            assertThat(viewModel.state.display).isNotNull()
        }
    }

    @Test
    fun theDialOpensOnTheLastConfirmedTimeRatherThanTheDefault() {
        val viewModel = viewModel(FakeSetupPreferences(lastWakeTimeIsoValue = "06:30"))

        assertThat(viewModel.state.localTimeIso).isEqualTo("06:30")
    }

    @Test
    fun theDialOpensOnTheDefaultTimeWhenNoneWasEverConfirmed() {
        val viewModel = viewModel(FakeSetupPreferences(lastWakeTimeIsoValue = null))

        assertThat(viewModel.state.localTimeIso).isEqualTo(DEFAULT_LOCAL_TIME_ISO)
    }

    @Test
    fun continuingPersistsTheChosenTimeAndInvokesTheCallback() {
        val preferences = FakeSetupPreferences()
        val viewModel = viewModel(preferences)
        viewModel.onTimeChanged(6, 30)
        var confirmed: String? = null

        viewModel.continueToSummary { localTimeIso, _ -> confirmed = localTimeIso }

        assertThat(confirmed).isEqualTo("06:30")
        assertThat(preferences.lastWakeTimeIsoValue).isEqualTo("06:30")
    }

    // Blocage différé (Lot 6, SPEC_ANDROID §15 « Écran 5 — début du blocage » ; SPEC_CORE_KMP §8.3).
    // Le calcul est prouvé par `BlockingScheduleCalculatorTest` dans `:shared:core` : ces tests
    // portent sur ce que l'écran en fait.

    @Test
    fun blockingIsImmediateByDefaultAndNeverBlocksContinuation() {
        val viewModel = viewModel()

        viewModel.onTimeChanged(7, 0)

        assertThat(viewModel.state.isBlockingImmediate).isTrue()
        assertThat(viewModel.state.blockingDisplay).isNull()
        assertThat(viewModel.state.blockingMessage).isNull()
        assertThat(viewModel.state.canContinue).isTrue()
    }

    @Test
    fun theLastConfirmedBlockingStartIsRestoredWhileImmediateStaysTheDefaultOtherwise() {
        val restored = viewModel(FakeSetupPreferences(lastBlockingStartTimeIsoValue = "22:30"))
        assertThat(restored.state.blockingLocalTimeIso).isEqualTo("22:30")
        assertThat(restored.state.isBlockingImmediate).isFalse()

        val fresh = viewModel(FakeSetupPreferences())
        assertThat(fresh.state.blockingLocalTimeIso).isNull()
        assertThat(fresh.state.isBlockingImmediate).isTrue()
    }

    @Test
    fun aStartChosenBeforeTheWakeUpDisplaysTheObtainedInstant() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)

        viewModel.onBlockingTimeChanged(22, 30)

        assertThat(viewModel.state.blockingLocalTimeIso).isEqualTo("22:30")
        assertThat(viewModel.state.blockingDisplay?.relativeDayLabel).isEqualTo("Aujourd'hui")
        assertThat(viewModel.state.blockingDisplay?.timeLabel).isEqualTo("22:30")
        assertThat(viewModel.state.blockingMessage).isNull()
        assertThat(viewModel.state.canContinue).isTrue()
    }

    @Test
    fun aStartWhoseNextOccurrenceFallsAfterTheWakeUpIsRefused() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)

        // 08:00 et 19:00 désignent tous deux demain, donc après le réveil de demain 07:00 : §8.3
        // refuse, et l'écran 5 le dit avant tout diagnostic (SPEC_ANDROID §13, point 4).
        listOf(8 to 0, 19 to 0).forEach { (hour, minute) ->
            viewModel.onBlockingTimeChanged(hour, minute)

            assertThat(viewModel.state.blockingDisplay).isNull()
            assertThat(viewModel.state.blockingMessage)
                .isEqualTo(WakeTimeTexts.BLOCKING_NOT_BEFORE_TRIGGER_MESSAGE)
            assertThat(viewModel.state.canContinue).isFalse()
        }
    }

    @Test
    fun aStartEqualToTheWakeUpIsRefused() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)

        viewModel.onBlockingTimeChanged(7, 0)

        assertThat(viewModel.state.blockingMessage)
            .isEqualTo(WakeTimeTexts.BLOCKING_NOT_BEFORE_TRIGGER_MESSAGE)
        assertThat(viewModel.state.canContinue).isFalse()
    }

    /**
     * **Défaut mesuré sur appareil le 2026-09-17.** Un début refusé affichait la phrase du blocage
     * immédiat au-dessus de son propre message de refus : deux affirmations contradictoires, dont
     * l'une promettait un blocage que l'activation n'aurait pas appliqué (§15, ne jamais afficher
     * un faux état de fiabilité).
     */
    @Test
    fun aRefusedStartConfirmsNothingRatherThanPromisingAnImmediateBlocking() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)

        viewModel.onBlockingTimeChanged(8, 0)

        assertThat(viewModel.state.blockingSentence).isNull()
        assertThat(viewModel.state.blockingMessage)
            .isEqualTo(WakeTimeTexts.BLOCKING_NOT_BEFORE_TRIGGER_MESSAGE)
    }

    @Test
    fun anImmediateBlockingConfirmsItselfAndADeferredOneNamesItsInstant() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)
        assertThat(viewModel.state.blockingSentence).isEqualTo(WakeTimeTexts.BLOCKING_IMMEDIATE_SENTENCE)

        viewModel.onBlockingTimeChanged(22, 30)

        assertThat(viewModel.state.blockingSentence).contains("22:30")
        assertThat(viewModel.state.blockingSentence).doesNotContain("dès l'activation")
    }

    // Ligne d'heure de la section blocage : elle ouvre le sélecteur et fait écho à la **saisie**,
    // dans la convention du système, comme le cadran du réveil. La phrase, elle, énonce l'instant.

    /**
     * **Constat D1 de l'étape 24, mesuré sur appareil le 2026-09-17.** Un début refusé affichait la
     * saisie ISO « 15:00 » sous un réveil « 7:00 AM » : la ligne montrait l'instant obtenu quand le
     * choix était valide et la saisie brute quand il était refusé, alors que le sélecteur qu'elle
     * ouvre se rouvre toujours sur la saisie.
     */
    @Test
    fun aRefusedStartIsStillEchoedInTheSystemConvention() {
        val viewModel = viewModel()
        viewModel.refresh(use24Hour = false)
        viewModel.onTimeChanged(7, 0)

        viewModel.onBlockingTimeChanged(8, 0)

        assertThat(viewModel.state.blockingTimeLabel).isEqualTo("8:00 AM")
        assertThat(viewModel.state.blockingDisplay).isNull()
        assertThat(viewModel.state.blockingMessage).isEqualTo(WakeTimeTexts.BLOCKING_NOT_BEFORE_TRIGGER_MESSAGE)
    }

    @Test
    fun theLineAndTheSentenceAgreeOnAValidStart() {
        val viewModel = viewModel()
        viewModel.refresh(use24Hour = false)
        viewModel.onTimeChanged(7, 0)

        viewModel.onBlockingTimeChanged(22, 30)

        assertThat(viewModel.state.blockingTimeLabel).isEqualTo("10:30 PM")
        assertThat(viewModel.state.blockingSentence).contains("10:30 PM")
    }

    @Test
    fun anImmediateBlockingHasNoTimeToEcho() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)

        assertThat(viewModel.state.blockingTimeLabel).isNull()
    }

    /**
     * Trou d'heure d'été : la ligne fait écho à la saisie (02:30), comme le sélecteur qui se rouvre
     * dessus ; seule la phrase porte l'instant réellement programmé (03:00, SPEC_CORE_KMP §8.1).
     */
    @Test
    fun inADaylightSavingGapTheLineEchoesTheChoiceAndTheSentenceTheInstant() {
        clock.now = paris(2026, 3, 28, 20, 0)
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)

        viewModel.onBlockingTimeChanged(2, 30)

        assertThat(viewModel.state.blockingTimeLabel).isEqualTo("02:30")
        assertThat(viewModel.state.blockingDisplay?.timeLabel).isEqualTo("03:00")
        assertThat(viewModel.state.blockingSentence).contains("03:00")
    }

    @Test
    fun switchingBackToImmediateClearsTheRefusal() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)
        viewModel.onBlockingTimeChanged(8, 0)
        assertThat(viewModel.state.canContinue).isFalse()

        viewModel.onBlockingModeChanged(immediate = true)

        assertThat(viewModel.state.blockingLocalTimeIso).isNull()
        assertThat(viewModel.state.blockingMessage).isNull()
        assertThat(viewModel.state.canContinue).isTrue()
    }

    @Test
    fun choosingDeferredAgainRestoresTheStartAlreadyChosen() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)
        viewModel.onBlockingTimeChanged(22, 30)
        viewModel.onBlockingModeChanged(immediate = true)

        viewModel.onBlockingModeChanged(immediate = false)

        assertThat(viewModel.state.blockingLocalTimeIso).isEqualTo("22:30")
        assertThat(viewModel.state.blockingDisplay?.timeLabel).isEqualTo("22:30")
    }

    @Test
    fun changingTheWakeTimeRecomputesTheBlockingStart() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(23, 0)
        viewModel.onBlockingTimeChanged(22, 30)
        assertThat(viewModel.state.blockingMessage).isNull()

        // Le réveil passe à 07:00 demain : 22:30 reste antérieur, mais l'inverse doit se voir aussi.
        viewModel.onTimeChanged(22, 0)

        assertThat(viewModel.state.blockingDisplay).isNull()
        assertThat(viewModel.state.blockingMessage)
            .isEqualTo(WakeTimeTexts.BLOCKING_NOT_BEFORE_TRIGGER_MESSAGE)
    }

    @Test
    fun refreshRecomputesBothInstantsInTheNewSystemZone() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)
        viewModel.onBlockingTimeChanged(22, 30)

        // Europe/London plutôt qu'un fuseau antipodal : le décalage d'une heure garde 22:30
        // antérieur au réveil, et c'est la relecture des **deux** instants qui est en cause ici.
        timeZoneProvider.zoneId = "Europe/London"
        viewModel.refresh(use24Hour = true)

        assertThat(viewModel.state.display?.zoneLabel).isEqualTo("Europe/London")
        assertThat(viewModel.state.blockingDisplay?.zoneLabel).isEqualTo("Europe/London")
        assertThat(viewModel.state.blockingDisplay?.timeLabel).isEqualTo("22:30")
    }

    @Test
    fun anInvalidWakeScheduleLeavesTheBlockingStartUnjudged() {
        timeZoneProvider.zoneId = "Not/AZone"
        val viewModel = viewModel(FakeSetupPreferences(lastBlockingStartTimeIsoValue = "22:30"))

        viewModel.onTimeChanged(7, 0)

        assertThat(viewModel.state.display).isNull()
        assertThat(viewModel.state.blockingDisplay).isNull()
        assertThat(viewModel.state.blockingMessage).isNull()
        assertThat(viewModel.state.canContinue).isFalse()
    }

    @Test
    fun continuingADeferredSessionPersistsAndForwardsBothChoices() {
        val preferences = FakeSetupPreferences()
        val viewModel = viewModel(preferences)
        viewModel.onTimeChanged(7, 0)
        viewModel.onBlockingTimeChanged(22, 30)
        var confirmed: Pair<String, String?>? = null

        viewModel.continueToSummary { localTimeIso, blockingLocalTimeIso ->
            confirmed = localTimeIso to blockingLocalTimeIso
        }

        assertThat(confirmed).isEqualTo("07:00" to "22:30")
        assertThat(preferences.lastWakeTimeIsoValue).isEqualTo("07:00")
        assertThat(preferences.lastBlockingStartTimeIsoValue).isEqualTo("22:30")
    }

    @Test
    fun continuingAnImmediateSessionForgetsTheStoredStart() {
        val preferences = FakeSetupPreferences(lastBlockingStartTimeIsoValue = "22:30")
        val viewModel = viewModel(preferences)
        viewModel.onTimeChanged(7, 0)
        viewModel.onBlockingModeChanged(immediate = true)
        var confirmed: Pair<String, String?>? = null

        viewModel.continueToSummary { localTimeIso, blockingLocalTimeIso ->
            confirmed = localTimeIso to blockingLocalTimeIso
        }

        assertThat(confirmed).isEqualTo("07:00" to null)
        assertThat(preferences.lastBlockingStartTimeIsoValue).isNull()
    }

    @Test
    fun aRefusedBlockingStartForbidsContinuationEntirely() {
        val viewModel = viewModel()
        viewModel.onTimeChanged(7, 0)
        viewModel.onBlockingTimeChanged(8, 0)
        var called = false

        viewModel.continueToSummary { _, _ -> called = true }

        assertThat(called).isFalse()
    }

    private fun armedSnapshot() =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = 1,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule =
                WakeScheduleDto(
                    localDateIso = "2026-09-04",
                    localTimeIso = "07:00",
                    zoneIdAtActivation = "Europe/Paris",
                    triggerAtEpochMillis = paris(2026, 9, 4, 7, 0),
                ),
            state = SessionStateDto.ARMED,
            releaseTarget = null,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = 1L,
            armedAtEpochMillis = 1L,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
        )
}
