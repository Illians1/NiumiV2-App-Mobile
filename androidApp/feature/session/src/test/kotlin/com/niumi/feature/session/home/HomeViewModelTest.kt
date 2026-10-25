package com.niumi.feature.session.home

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.database.BlockedPackage
import com.niumi.designsystem.drawing.DialHandle
import com.niumi.feature.session.active.fakes.FakeSessionPersistenceGateway
import com.niumi.feature.session.active.fakes.presentSession
import com.niumi.feature.session.fakes.FakeAlarmSoundPreferences
import com.niumi.feature.session.fakes.FakeClock
import com.niumi.feature.session.fakes.FakeTimeZoneProvider
import com.niumi.feature.session.home.fakes.FakeInstalledApps
import com.niumi.feature.session.home.fakes.FakeWakePlanStore
import com.niumi.feature.session.home.fakes.InMemoryAppSelection
import com.niumi.feature.session.home.fakes.ManualMinuteTicker
import com.niumi.feature.session.home.fakes.PARIS
import com.niumi.feature.session.home.fakes.RecordingReadinessTrigger
import com.niumi.feature.session.home.fakes.RecordingWakePlanScheduler
import com.niumi.feature.session.home.fakes.armedSnapshot
import com.niumi.feature.session.home.fakes.paris
import com.niumi.system.apps.InstalledApp
import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.plan.ActivationFailure
import com.niumi.system.plan.PlanApplyResult
import com.niumi.system.plan.PlanArmingStatus
import com.niumi.system.plan.PlanChangeReason
import com.niumi.system.plan.WakePlan
import com.niumi.system.session.SessionSnapshotPublisher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Accueil (`design-plan/SPECIFICATION.md` §9, maquettes 15 à 20 ; décisions de Mehdi du 2026-10-25).
 * Jeudi 22 octobre 2026 à Paris, planning lundi à vendredi, réveil 7:00, coucher 22:30. Le planning
 * est la source du cadran et de la carte ; la session, celle du verrouillage.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val enabledPlan = WakePlan.DEFAULT.copy(enabled = true)
    private val store = FakeWakePlanStore(WakePlan.DEFAULT)
    private val scheduler = RecordingWakePlanScheduler(store)
    private val armingStatus = PlanArmingStatus()
    private val publisher = SessionSnapshotPublisher()
    private val gateway = FakeSessionPersistenceGateway()
    private val selection = InMemoryAppSelection()
    private val installed = FakeInstalledApps()
    private val sound = FakeAlarmSoundPreferences(AlarmSoundSettings("niumi_oiseaux", null))
    private val clock = FakeClock(paris("2026-10-22T21:34"))
    private val ticker = ManualMinuteTicker()
    private val readiness = RecordingReadinessTrigger()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): HomeViewModel =
        HomeViewModel(
            HomeSources(
                plan = HomePlanSources(store, scheduler, armingStatus, NiumiCoreFacade(), readiness),
                rows = HomeRowSources(selection, installed, sound, gateway),
                snapshotPublisher = publisher,
                time = HomeTime(clock, FakeTimeZoneProvider(PARIS), ticker),
            ),
        )

    // Les trois états (maquettes 15, 16, 17).

    @Test
    fun aSwitchedOffPlanShowsWakeDisabled() {
        val state = viewModel().state

        assertThat(state.isLoading).isFalse()
        assertThat(state.mode).isEqualTo(HomeMode.Disabled)
        assertThat(state.centerLabel).isEqualTo("Réveil")
        assertThat(state.centerValue).isEqualTo("Désactivé")
        assertThat(state.cardTitle).isEqualTo("Réveil désactivé")
        assertThat(state.cardLines).containsExactly("Aucun réveil ni blocage prévu")
        assertThat(state.switchChecked).isFalse()
        assertThat(state.bedtimeMinutes).isEqualTo(22 * 60 + 30)
        assertThat(state.wakeMinutes).isEqualTo(7 * 60)
        assertThat(state.nowMinutes).isEqualTo(21 * 60 + 34)
        assertThat(state.dayMode).isEqualTo(DayMode.CUSTOM)
    }

    @Test
    fun aPendingSessionShowsTheCountdownsOfWakeScheduled() {
        runBlockingWrite(enabledPlan)
        publisher.publish(armedSnapshot())

        val state = viewModel().state

        assertThat(state.mode).isEqualTo(HomeMode.Scheduled)
        assertThat(state.centerLabel).isEqualTo("Réveil dans")
        assertThat(state.centerValue).isEqualTo("9 h 26")
        assertThat(state.cardTitle).isEqualTo("Réveil activé")
        assertThat(state.cardLines).containsExactly("Blocage des apps dans 56 min", "8 h 30 de sommeil").inOrder()
        assertThat(state.switchChecked).isTrue()
    }

    @Test
    fun beforeTheSessionIsArmedTheNextOccurrenceIsComputed() {
        runBlockingWrite(enabledPlan)

        val state = viewModel().state

        assertThat(state.mode).isEqualTo(HomeMode.Scheduled)
        assertThat(state.centerValue).isEqualTo("9 h 26")
        assertThat(state.cardLines).containsExactly("Blocage des apps dans 56 min", "8 h 30 de sommeil").inOrder()
    }

    @Test
    fun theBlockingInForceShowsTheSessionInProgress() {
        runBlockingWrite(enabledPlan)
        clock.now = paris("2026-10-22T23:10")
        publisher.publish(armedSnapshot(blockingApplied = true))

        val state = viewModel().state

        assertThat(state.mode).isEqualTo(HomeMode.InSession)
        assertThat(state.isLocked).isTrue()
        assertThat(state.centerValue).isEqualTo("7 h 50")
        assertThat(state.cardTitle).isEqualTo("Blocage en cours")
        assertThat(state.cardLines).containsExactly("Applications bloquées jusqu’au scan de ton Niumi Spot")
    }

    /** Cas limite 4 du plan : « 56 min », jamais « 0 h 56 ». */
    @Test
    fun underAnHourTheCountdownIsInMinutes() {
        runBlockingWrite(enabledPlan)
        clock.now = paris("2026-10-23T06:04")
        publisher.publish(armedSnapshot(blockingApplied = true))

        assertThat(viewModel().state.centerValue).isEqualTo("56 min")
    }

    /** Q14 : l'armement a échoué, l'interrupteur reste allumé, jamais un faux « Réveil prévu ». */
    @Test
    fun aFailedArmingShowsNothingPlannedWithTheSwitchOn() {
        runBlockingWrite(enabledPlan)
        armingStatus.reportFailed(ActivationFailure.Duplicate, atEpochMillis = 1L)

        val state = viewModel().state

        assertThat(state.mode).isEqualTo(HomeMode.NothingArmed)
        assertThat(state.cardTitle).isEqualTo("Réveil activé")
        assertThat(state.cardLines).containsExactly("Aucun réveil ni blocage prévu")
        assertThat(state.switchChecked).isTrue()
        assertThat(state.centerValue).isEmpty()
    }

    @Test
    fun aTwentyFourHourPeriodIsNamedOnTheCard() {
        runBlockingWrite(enabledPlan.copy(bedtimeIso = "07:00"))

        assertThat(viewModel().state.cardLines).contains("24 h de blocage")
    }

    // Ponctuel et Quotidien (maquette 18).

    @Test
    fun theDayModeFollowsTheCheckedDays() {
        runBlockingWrite(enabledPlan.copy(enabledIsoDays = emptySet()))
        assertThat(viewModel().state.dayMode).isEqualTo(DayMode.ONE_OFF)

        runBlockingWrite(enabledPlan.copy(enabledIsoDays = (1..7).toSet()))
        assertThat(viewModel().state.dayMode).isEqualTo(DayMode.DAILY)
    }

    @Test
    fun choosingDailyChecksEveryDayAndOneOffNone() {
        runBlockingWrite(enabledPlan)
        val viewModel = viewModel()

        viewModel.onDayModeSelected(DayMode.DAILY)
        viewModel.onDayModeSelected(DayMode.ONE_OFF)

        assertThat(scheduler.applied.map { it.first.enabledIsoDays })
            .containsExactly((1..7).toSet(), emptySet<Int>())
            .inOrder()
        assertThat(scheduler.applied.map { it.second }).containsExactly(PlanChangeReason.DAYS, PlanChangeReason.DAYS)
    }

    @Test
    fun uncheckingTheLastDayMakesTheAlarmOneOff() {
        runBlockingWrite(enabledPlan.copy(enabledIsoDays = setOf(3)))
        val viewModel = viewModel()

        viewModel.onDayToggled(3)

        assertThat(
            scheduler.applied.single(),
        ).isEqualTo(enabledPlan.copy(enabledIsoDays = emptySet()) to PlanChangeReason.DAYS)
        assertThat(viewModel.state.dayMode).isEqualTo(DayMode.ONE_OFF)
    }

    // Modifications, toutes par le planning.

    @Test
    fun theSwitchAppliesThePlan() {
        val viewModel = viewModel()

        viewModel.onSwitchChanged(true)

        assertThat(scheduler.applied.single()).isEqualTo(enabledPlan to PlanChangeReason.SWITCH)
    }

    /**
     * Essai du 2026-10-25 : l'interrupteur allumé sans le service d'accessibilité ne montrait rien.
     * La surveillance §13.1 (notification d'avertissement) est évaluée dès qu'un réglage est appliqué,
     * sans attendre le prochain retour au premier plan.
     */
    @Test
    fun anAppliedSettingTriggersTheReadinessWatch() {
        val viewModel = viewModel()

        viewModel.onSwitchChanged(true)

        assertThat(readiness.evaluations).isEqualTo(1)
    }

    @Test
    fun aLockedTouchTriggersNoReadinessWatch() {
        runBlockingWrite(enabledPlan)
        publisher.publish(armedSnapshot(blockingApplied = true))
        val viewModel = viewModel()

        viewModel.onDayToggled(6)

        assertThat(readiness.evaluations).isEqualTo(0)
    }

    @Test
    fun aReleasedHandleAppliesItsTimeOnce() {
        runBlockingWrite(enabledPlan)
        val viewModel = viewModel()

        viewModel.onTimeChanged(DialHandle.BEDTIME, 22 * 60 + 15)

        assertThat(
            scheduler.applied.single(),
        ).isEqualTo(enabledPlan.copy(bedtimeIso = "22:15") to PlanChangeReason.BEDTIME)
        assertThat(viewModel.state.bedtimeMinutes).isEqualTo(22 * 60 + 15)
    }

    @Test
    fun tappingTheDialOpensThePickerAndConfirmingAppliesBothTimesOnce() {
        runBlockingWrite(enabledPlan)
        val viewModel = viewModel()

        viewModel.onDialTapped(DialHandle.WAKE)
        assertThat(viewModel.state.picker).isEqualTo(DialHandle.WAKE)
        viewModel.onPickerConfirmed(bedtime = 22 * 60 + 31, wake = 6 * 60 + 50)

        assertThat(viewModel.state.picker).isNull()
        assertThat(scheduler.applied.single())
            .isEqualTo(enabledPlan.copy(bedtimeIso = "22:31", wakeTimeIso = "06:50") to PlanChangeReason.WAKE_TIME)
    }

    /** Relecture : « Valider » sans rien changer doit fermer la fenêtre. */
    @Test
    fun confirmingThePickerWithoutAnyChangeClosesIt() {
        runBlockingWrite(enabledPlan)
        val viewModel = viewModel()
        viewModel.onDialTapped(DialHandle.WAKE)

        viewModel.onPickerConfirmed(bedtime = 22 * 60 + 30, wake = 7 * 60)

        assertThat(viewModel.state.picker).isNull()
        assertThat(scheduler.applied).isEmpty()
    }

    /** Relecture : une erreur d'écriture du planning ne fait pas planter l'accueil ; il revient au planning lu. */
    @Test
    fun aStorageFailureWhileApplyingKeepsTheStoredPlanOnScreen() {
        runBlockingWrite(enabledPlan)
        scheduler.failure = java.io.IOException("disque")
        val viewModel = viewModel()

        viewModel.onTimeChanged(DialHandle.WAKE, 8 * 60)

        assertThat(viewModel.state.wakeMinutes).isEqualTo(7 * 60)
    }

    /** Ponctuel de 24 h juste après l'heure : « 1 j », jamais « 24 h 00 ». */
    @Test
    fun aFullDayCountdownReadsOneDay() {
        runBlockingWrite(enabledPlan.copy(enabledIsoDays = emptySet(), bedtimeIso = "07:00"))
        clock.now = paris("2026-10-22T07:00") + 30_000

        assertThat(viewModel().state.centerValue).isEqualTo("1 j")
    }

    @Test
    fun dismissingThePickerChangesNothing() {
        val viewModel = viewModel()

        viewModel.onDialTapped(DialHandle.BEDTIME)
        viewModel.onPickerDismissed()

        assertThat(viewModel.state.picker).isNull()
        assertThat(scheduler.applied).isEmpty()
    }

    /** M10 : en session, toucher un champ verrouillé montre la bulle et ne modifie rien. */
    @Test
    fun inSessionEveryLockedFieldShowsTheMessageAndAppliesNothing() {
        runBlockingWrite(enabledPlan)
        clock.now = paris("2026-10-22T23:10")
        publisher.publish(armedSnapshot(blockingApplied = true))
        val viewModel = viewModel()

        viewModel.onDayToggled(6)
        viewModel.onDayModeSelected(DayMode.DAILY)
        viewModel.onTimeChanged(DialHandle.WAKE, 8 * 60)
        viewModel.onDialTapped(DialHandle.WAKE)

        assertThat(scheduler.applied).isEmpty()
        assertThat(viewModel.state.picker).isNull()
        assertThat(viewModel.state.message).isEqualTo("Modifiable après avoir scanné ton Niumi Spot.")
    }

    @Test
    fun theNextTouchDismissesTheMessage() {
        val viewModel = viewModel()
        viewModel.onLockedTouch()

        viewModel.dismissMessage()

        assertThat(viewModel.state.message).isNull()
    }

    /** Q13 : le compte à rebours avance à chaque minute. */
    @Test
    fun eachMinuteTickMovesTheCountdown() {
        runBlockingWrite(enabledPlan)
        publisher.publish(armedSnapshot())
        val viewModel = viewModel()

        clock.now = paris("2026-10-22T21:35")
        ticker.flow.tryEmit(Unit)

        assertThat(viewModel.state.centerValue).isEqualTo("9 h 25")
        assertThat(viewModel.state.nowMinutes).isEqualTo(21 * 60 + 35)
    }

    @Test
    fun aSessionStartingWhileShownTurnsTheHomeDark() {
        runBlockingWrite(enabledPlan)
        val viewModel = viewModel()

        publisher.publish(armedSnapshot(blockingApplied = true))

        assertThat(viewModel.state.mode).isEqualTo(HomeMode.InSession)
    }

    // Lignes « Applications bloquées » et « Sonnerie ».

    @Test
    fun everyChosenAppHasAnIconEvenWhenUninstalled() {
        selection.stored =
            listOf(BlockedPackage("com.example.chat", "Chat"), BlockedPackage("com.example.gone", "Gone"))
        installed.apps = listOf(InstalledApp("com.example.chat", "Chat", icon = null))

        val apps = viewModel().state.apps

        assertThat(apps.map { it.packageName }).containsExactly("com.example.chat", "com.example.gone").inOrder()
    }

    /** D5 : sonnerie progressive éteinte, le nom du son seul. */
    @Test
    fun withoutAnArmedSessionTheRingtoneRowShowsThePreference() {
        val ringtone = viewModel().state.ringtone

        assertThat(ringtone?.name).isEqualTo("Oiseaux")
        assertThat(ringtone?.detail).isNull()
    }

    @Test
    fun anArmedSessionShowsItsOwnRingtone() {
        runBlockingWrite(enabledPlan)
        gateway.result = presentSession(armedSnapshot(), emptyList())

        val ringtone = viewModel().state.ringtone

        assertThat(ringtone?.name).isEqualTo("Piano")
        assertThat(ringtone?.detail).isEqualTo("Progressive · 2 min")
    }

    @Test
    fun aPlanLockedBySessionIsNotKeptOnScreen() {
        runBlockingWrite(enabledPlan)
        scheduler.result = PlanApplyResult.LockedBySession
        val viewModel = viewModel()

        viewModel.onTimeChanged(DialHandle.WAKE, 8 * 60)

        assertThat(viewModel.state.wakeMinutes).isEqualTo(7 * 60)
    }

    private fun runBlockingWrite(plan: WakePlan) = kotlinx.coroutines.runBlocking { store.write(plan) }
}
