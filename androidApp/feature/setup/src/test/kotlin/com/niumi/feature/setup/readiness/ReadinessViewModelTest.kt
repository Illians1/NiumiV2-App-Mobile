package com.niumi.feature.setup.readiness

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.feature.setup.readiness.fakes.FakeSetupPreferences
import com.niumi.feature.setup.readiness.fakes.NOW_EPOCH_MILLIS
import com.niumi.feature.setup.readiness.fakes.reportWith
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome
import com.niumi.system.readiness.ReadinessReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * SPEC_ANDROID §13 : une seule action principale, le premier blocage d'abord, et la décision
 * d'autoriser l'activation prise par `NiumiCoreFacade.evaluateActivation` — jamais recalculée
 * côté Android. Le test passe donc par la **vraie** façade, jamais par une politique simulée.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReadinessViewModelTest {
    private val preferences = FakeSetupPreferences()
    private var report: ReadinessReport = reportWith()

    @Before
    fun setUp() {
        // `viewModelScope` poste sur `Dispatchers.Main` : sans dispatcher de test, `refresh()`
        // ne s'exécuterait jamais. `Unconfined` le rend synchrone, l'état est donc lisible dès
        // le retour de l'appel.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): ReadinessViewModel =
        ReadinessViewModel(
            readinessChecker = DeviceReadinessChecker { report },
            facade = NiumiCoreFacade(),
            setupPreferences = preferences,
        )

    @Test
    fun theFirstFailureInSpecOrderIsTheSinglePrimaryAction() {
        report = reportWith(failing = setOf(ReadinessCheckId.ACCESSIBILITY_SERVICE, ReadinessCheckId.ALARM_VOLUME))

        val state = viewModel().state

        // ALARM_VOLUME précède ACCESSIBILITY_SERVICE dans le tableau de §13.
        assertThat(state.primary?.id).isEqualTo(ReadinessCheckId.ALARM_VOLUME)
    }

    @Test
    fun notApplicableChecksAreNeverDisplayed() {
        report = reportWith(notApplicable = setOf(ReadinessCheckId.FULL_SCREEN_INTENT))

        val state = viewModel().state

        assertThat(state.items.map { it.id }).doesNotContain(ReadinessCheckId.FULL_SCREEN_INTENT)
        assertThat(state.items).hasSize(ReadinessCheckId.entries.size - 1)
    }

    @Test
    fun aWarningAloneNeverBlocksTheActivation() {
        report = reportWith(failing = setOf(ReadinessCheckId.DND_OTHER_MODE))

        val state = viewModel().state

        assertThat(state.isAllowed).isTrue()
        assertThat(state.primary?.id).isEqualTo(ReadinessCheckId.DND_OTHER_MODE)
    }

    @Test
    fun aSingleBlockingCheckRefusesTheActivation() {
        report = reportWith(failing = setOf(ReadinessCheckId.DND_TOTAL_SILENCE))

        assertThat(viewModel().state.isAllowed).isFalse()
    }

    @Test
    fun everythingPassingAllowsTheActivationAndLeavesNoPrimaryAction() {
        report = reportWith()

        val state = viewModel().state

        assertThat(state.isAllowed).isTrue()
        assertThat(state.primary).isNull()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun aMissingWakeTimeRefusesTheActivationWithoutDisplayingAnyFailedCheck() {
        // Diagnostic lancé avant le choix de l'heure : FUTURE_TRIGGER est sans objet, mais la
        // politique commune refuse par TRIGGER_NOT_IN_FUTURE (§13, point 2 de l'implémentation).
        report =
            reportWith(
                notApplicable = setOf(ReadinessCheckId.FUTURE_TRIGGER),
                candidateTriggerAtEpochMillis = null,
            )

        val state = viewModel().state

        assertThat(state.isAllowed).isFalse()
        assertThat(state.items.none { it.outcome == ReadinessOutcome.FAILED }).isTrue()
        assertThat(state.isDeviceReady).isTrue()
    }

    /** Les écrans 3 et 4 existent depuis l'étape 13 : leurs recours sont désormais actionnables. */
    @Test
    fun pairingAndAppPickerActionsAreAvailableNowThatTheirScreensExist() {
        report = reportWith(failing = setOf(ReadinessCheckId.PAIRED_BOX, ReadinessCheckId.APP_SELECTION))

        val state = viewModel().state

        assertThat(state.primary?.id).isEqualTo(ReadinessCheckId.PAIRED_BOX)
        assertThat(state.primary?.isActionAvailable).isTrue()

        val appSelection = state.items.single { it.id == ReadinessCheckId.APP_SELECTION }
        assertThat(appSelection.isActionAvailable).isTrue()
    }

    /**
     * Mesuré sur appareil à l'étape 13 : une fois le boîtier associé et les applications choisies,
     * ces deux lignes passaient au vert et perdaient leur bouton — or c'était le seul chemin vers
     * les écrans 3 et 4, qui devenaient définitivement inatteignables. SPEC_ANDROID §11.1 exige
     * pourtant qu'une nouvelle association puisse remplacer l'ancienne. Les deux contrôles de
     * parcours gardent donc leur action une fois satisfaits ; les contrôles de blocage, non.
     */
    @Test
    fun theTwoJourneyChecksKeepAnActionOnceSatisfied() {
        report = reportWith()

        val state = viewModel().state

        val journey = state.items.filter { it.id in setOf(ReadinessCheckId.PAIRED_BOX, ReadinessCheckId.APP_SELECTION) }
        assertThat(journey).hasSize(2)
        journey.forEach { item ->
            assertThat(item.outcome).isEqualTo(ReadinessOutcome.PASSED)
            assertThat(item.isRevisitable).isTrue()
            assertThat(item.isActionAvailable).isTrue()
        }
    }

    @Test
    fun theRevisitLabelSaysChangeRatherThanSetUpOnceSatisfied() {
        report = reportWith()

        val pairedBox = viewModel().state.items.single { it.id == ReadinessCheckId.PAIRED_BOX }

        assertThat(pairedBox.actionLabel).isEqualTo(ReadinessMessages.CHANGE_PAIRED_BOX_LABEL)
        assertThat(pairedBox.actionLabel).isNotEqualTo(ReadinessMessages.actionLabelFor(ReadinessCheckId.PAIRED_BOX))
    }

    /** Un contrôle de blocage satisfait n'est pas une étape de parcours : il reste sans action. */
    @Test
    fun aSatisfiedBlockingCheckOffersNoAction() {
        report = reportWith()

        val volume = viewModel().state.items.single { it.id == ReadinessCheckId.ALARM_VOLUME }

        assertThat(volume.outcome).isEqualTo(ReadinessOutcome.PASSED)
        assertThat(volume.isRevisitable).isFalse()
    }

    /** En échec, les contrôles de parcours restent l'action principale, pas une action secondaire. */
    @Test
    fun aFailingJourneyCheckIsStillThePrimaryActionAndKeepsItsSetUpLabel() {
        report = reportWith(failing = setOf(ReadinessCheckId.PAIRED_BOX))

        val state = viewModel().state

        assertThat(state.primary?.id).isEqualTo(ReadinessCheckId.PAIRED_BOX)
        assertThat(state.primary?.actionLabel)
            .isEqualTo(ReadinessMessages.actionLabelFor(ReadinessCheckId.PAIRED_BOX))
        assertThat(state.primary?.isRevisitable).isFalse()
    }

    /** `FixTime` attend le choix de l'heure (étape 14) : son recours reste inactif. */
    @Test
    fun anActionWhoseScreenIsStillMissingStaysUnavailable() {
        report = reportWith(failing = setOf(ReadinessCheckId.FUTURE_TRIGGER))

        val state = viewModel().state

        assertThat(state.primary?.id).isEqualTo(ReadinessCheckId.FUTURE_TRIGGER)
        assertThat(state.primary?.isActionAvailable).isFalse()
    }

    @Test
    fun theBatteryActionBecomesAConfirmationOnceTheSettingsHaveBeenOpened() {
        report = reportWith(failing = setOf(ReadinessCheckId.BATTERY_OPTIMIZATION))
        val viewModel = viewModel()

        assertThat(viewModel.state.primary?.actionLabel)
            .isEqualTo(ReadinessMessages.actionLabelFor(ReadinessCheckId.BATTERY_OPTIMIZATION))

        viewModel.onBatterySettingsOpened()

        assertThat(viewModel.state.primary?.actionLabel).isEqualTo(ReadinessMessages.BATTERY_CONFIRM_LABEL)
    }

    @Test
    fun confirmingTheBatteryExemptionPersistsItAndRecomputesTheDiagnostic() {
        report = reportWith(failing = setOf(ReadinessCheckId.BATTERY_OPTIMIZATION))
        val viewModel = viewModel()

        // Le contrôleur relit la confirmation : le rapport suivant en tient compte (§13).
        report = reportWith()
        viewModel.confirmBatteryExemption()

        assertThat(preferences.batteryExemptionConfirmed).isTrue()
        assertThat(preferences.batteryWrites).isEqualTo(1)
        assertThat(viewModel.state.primary).isNull()
    }

    @Test
    fun theDiagnosticIsRecomputedOnEveryRefresh() {
        report = reportWith(failing = setOf(ReadinessCheckId.NFC_ENABLED))
        val viewModel = viewModel()
        assertThat(viewModel.state.primary?.id).isEqualTo(ReadinessCheckId.NFC_ENABLED)

        report = reportWith()
        viewModel.refresh()

        assertThat(viewModel.state.primary).isNull()
    }

    @Test
    fun everyDisplayedItemCarriesItsSpecMessage() {
        report = reportWith(failing = setOf(ReadinessCheckId.ALARM_CHANNEL))

        val state = viewModel().state

        state.items.forEach { item ->
            assertThat(item.message).isEqualTo(ReadinessMessages.forCheck(item.id))
        }
        assertThat(state.nowEpochMillis).isEqualTo(NOW_EPOCH_MILLIS)
    }
}

/**
 * `summary` est ce que l'écran affiche pour chaque contrôle. Régression du 2026-09-11 : un
 * contrôle satisfait montrait son message de remédiation, donc l'inverse de la vérité.
 */
class ReadinessItemSummaryTest {
    @Test
    fun aPassedCheckIsNamedByItsLabel() {
        val item = itemWith(ReadinessOutcome.PASSED)

        assertThat(item.summary).isEqualTo(ReadinessMessages.labelFor(ReadinessCheckId.ALARM_VOLUME))
    }

    @Test
    fun aFailedCheckKeepsItsRemediationMessage() {
        val item = itemWith(ReadinessOutcome.FAILED)

        assertThat(item.summary).isEqualTo(ReadinessMessages.forCheck(ReadinessCheckId.ALARM_VOLUME))
    }

    private fun itemWith(outcome: ReadinessOutcome) =
        ReadinessItem(
            id = ReadinessCheckId.ALARM_VOLUME,
            message = ReadinessMessages.forCheck(ReadinessCheckId.ALARM_VOLUME),
            label = ReadinessMessages.labelFor(ReadinessCheckId.ALARM_VOLUME),
            severity = com.niumi.core.interop.ReadinessSeverityDto.BLOCKING_FOR_ALARM,
            outcome = outcome,
            action = com.niumi.system.readiness.ReadinessAction.OpenSoundSettings,
            actionLabel = ReadinessMessages.actionLabelFor(ReadinessCheckId.ALARM_VOLUME),
            isActionAvailable = true,
        )
}
