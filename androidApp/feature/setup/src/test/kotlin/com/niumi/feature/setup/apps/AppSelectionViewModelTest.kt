package com.niumi.feature.setup.apps

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.AppSelectionSummary
import com.niumi.core.interop.BlockingScheduleDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.BlockedPackage
import com.niumi.feature.setup.presentation.fakes.FakeSetupPreferences
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.apps.InstalledApp
import com.niumi.system.apps.InstalledAppsSource
import com.niumi.system.plan.PlanApplyResult
import com.niumi.system.plan.PlanChangeReason
import com.niumi.system.plan.WakePlan
import com.niumi.system.plan.WakePlanScheduler
import com.niumi.system.plan.WakePlanStore
import com.niumi.system.session.SessionSnapshotPublisher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Écran 4 (`SPECIFICATION.md` §8, SPEC_ANDROID §12.1) : la feuille travaille sur un brouillon, la
 * croix l'abandonne, seul « Valider » écrit `niumi_app_selection` ; la carte montre la sélection
 * validée (Q3, 2026-10-22). Les bornes 1..50 viennent d'`AppSelectionSummary` (`:shared:core`) :
 * à 50, les applications non cochées sont inactives (décision du 2026-10-22).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppSelectionViewModelTest {
    private class FakeInstalledAppsSource(
        var apps: List<InstalledApp> = emptyList(),
    ) : InstalledAppsSource {
        /** Quand renseigné, la lecture attend : un chargement lent après une mort du processus. */
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun launchableApps(): List<InstalledApp> {
            gate?.await()
            return apps
        }
    }

    private class FakeAppSelectionStore(
        var stored: List<BlockedPackage> = emptyList(),
    ) : AppSelectionStore {
        var replaceCallCount = 0

        /** Quand renseigné, l'écriture attend : un second « Valider » arrive pendant l'écriture. */
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun selectedCount(): Int = stored.size

        override suspend fun selection(): List<BlockedPackage> = stored

        val journal = mutableListOf<String>()

        override suspend fun replace(selection: List<BlockedPackage>) {
            journal += "replace"
            replaceCallCount++
            gate?.await()
            stored = selection
        }
    }

    private class FakeWakePlanStore(
        initial: WakePlan,
    ) : WakePlanStore {
        private val flow = MutableStateFlow(initial)

        override val plan: Flow<WakePlan> = flow

        override suspend fun read(): WakePlan = flow.value

        override suspend fun write(plan: WakePlan) {
            flow.value = plan
        }
    }

    private class RecordingScheduler(
        private val journal: MutableList<String>,
    ) : WakePlanScheduler {
        val applied = mutableListOf<Pair<WakePlan, PlanChangeReason>>()

        var failure: Exception? = null

        override suspend fun apply(
            plan: WakePlan,
            reason: PlanChangeReason,
        ): PlanApplyResult {
            failure?.let { throw it }
            journal += "apply"
            applied += plan to reason
            return PlanApplyResult.Unchanged
        }
    }

    private val journal = mutableListOf<String>()
    private val planStore = FakeWakePlanStore(WakePlan.DEFAULT)
    private val scheduler = RecordingScheduler(journal)
    private val setupPreferences = FakeSetupPreferences()
    private val appsSource = FakeInstalledAppsSource()
    private val store = FakeAppSelectionStore()
    private val publisher = SessionSnapshotPublisher()

    @Before
    fun setUp() {
        // `Unconfined` rend le chargement de `init` synchrone : l'état est lisible dès la
        // construction du ViewModel.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun apps(count: Int): List<InstalledApp> =
        (1..count).map { index -> InstalledApp("com.example.app$index", "Application $index", icon = null) }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): AppSelectionViewModel =
        AppSelectionViewModel(
            appsSource,
            store,
            publisher,
            AppSelectionPlanning(planStore, scheduler, setupPreferences),
            savedStateHandle,
        )

    private fun AppSelectionViewModel.page(): AppSelectionPageState = checkNotNull(state.page)

    @Test
    fun withoutASavedSelectionTheCardIsEmptyAndContinueIsOff() {
        appsSource.apps = apps(3)

        val state = viewModel().state

        assertThat(state.saved).isEmpty()
        assertThat(state.canContinue).isFalse()
        assertThat(state.page).isNull()
    }

    @Test
    fun thePageStartsFromTheSavedSelection() {
        appsSource.apps = apps(3)
        store.stored = listOf(BlockedPackage("com.example.app2", "Application 2"))
        val viewModel = viewModel()

        viewModel.openPage()

        assertThat(viewModel.page().draft).containsExactly("com.example.app2")
        assertThat(viewModel.page().query).isEmpty()
        assertThat(viewModel.page().counter).isEqualTo("1 application choisie")
    }

    @Test
    fun checkingInThePageLeavesTheCardAndTheStoreUntouched() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.openPage()

        viewModel.toggle("com.example.app1")

        assertThat(viewModel.page().counter).isEqualTo("1 application choisie")
        assertThat(viewModel.state.saved).isEmpty()
        assertThat(viewModel.state.canContinue).isFalse()
        assertThat(store.replaceCallCount).isEqualTo(0)
    }

    @Test
    fun theCrossDropsTheDraftWithoutWriting() {
        appsSource.apps = apps(6)
        val viewModel = viewModel()
        viewModel.openPage()
        (1..6).forEach { viewModel.toggle("com.example.app$it") }

        viewModel.dismissPage()

        assertThat(viewModel.state.page).isNull()
        assertThat(viewModel.state.saved).isEmpty()
        assertThat(store.replaceCallCount).isEqualTo(0)
        viewModel.openPage()
        assertThat(viewModel.page().draft).isEmpty()
    }

    @Test
    fun validatingWritesOnceWithFrozenLabelsAndFillsTheCard() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.openPage()
        viewModel.toggle("com.example.app2")

        viewModel.validate()
        viewModel.validate()

        assertThat(store.replaceCallCount).isEqualTo(1)
        assertThat(store.stored).containsExactly(BlockedPackage("com.example.app2", "Application 2"))
        assertThat(viewModel.state.saved.map { it.packageName }).containsExactly("com.example.app2")
        assertThat(viewModel.state.canContinue).isTrue()
    }

    /**
     * Après l'écriture, la feuille reste là le temps de se ranger avec son animation (relecture de
     * l'étape 37) ; « Valider » n'y agit plus. Sa fermeture, ensuite, ne défait rien.
     */
    @Test
    fun afterValidatingThePageClosesItselfWithoutUndoingTheChoice() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.openPage()
        viewModel.toggle("com.example.app2")

        viewModel.validate()

        assertThat(viewModel.page().isSaved).isTrue()
        assertThat(viewModel.page().canValidate).isFalse()
        viewModel.dismissPage()
        assertThat(viewModel.state.page).isNull()
        assertThat(viewModel.state.saved.map { it.packageName }).containsExactly("com.example.app2")
        assertThat(store.replaceCallCount).isEqualTo(1)
    }

    @Test
    fun duringTheWriteValidateIsOff() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.openPage()
        viewModel.toggle("com.example.app1")
        val gate = CompletableDeferred<Unit>()
        store.gate = gate

        viewModel.validate()

        assertThat(viewModel.page().canValidate).isFalse()
        assertThat(viewModel.page().isSaved).isFalse()
        gate.complete(Unit)
        assertThat(viewModel.page().isSaved).isTrue()
    }

    @Test
    fun anEmptyDraftCannotBeValidated() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.openPage()

        assertThat(viewModel.page().canValidate).isFalse()
        assertThat(viewModel.page().counter).isEqualTo("Aucune application choisie")
        viewModel.validate()

        assertThat(store.replaceCallCount).isEqualTo(0)
        assertThat(viewModel.state.page).isNotNull()
    }

    @Test
    fun uncheckingEverySavedAppCannotEmptyTheSelection() {
        appsSource.apps = apps(3)
        store.stored = listOf(BlockedPackage("com.example.app1", "Application 1"))
        val viewModel = viewModel()
        viewModel.openPage()

        viewModel.toggle("com.example.app1")
        viewModel.validate()

        assertThat(store.replaceCallCount).isEqualTo(0)
        assertThat(viewModel.state.saved.map { it.packageName }).containsExactly("com.example.app1")
    }

    @Test
    fun theSearchIgnoresCaseAndAccents() {
        appsSource.apps =
            listOf(
                InstalledApp("com.instagram.android", "Instagram", icon = null),
                InstalledApp("com.android.dialer", "Téléphone", icon = null),
                InstalledApp("com.spotify.music", "Spotify", icon = null),
            )
        val viewModel = viewModel()
        viewModel.openPage()

        viewModel.search("INSTA")
        assertThat(viewModel.page().items.map { it.app.label }).containsExactly("Instagram")

        viewModel.search("telephone")
        assertThat(viewModel.page().items.map { it.app.label }).containsExactly("Téléphone")

        viewModel.search("")
        assertThat(viewModel.page().items).hasSize(3)
    }

    @Test
    fun aCheckedAppHiddenByTheSearchStaysCheckedAndCounted() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.openPage()
        viewModel.toggle("com.example.app1")

        viewModel.search("Application 2")

        assertThat(viewModel.page().items.map { it.app.packageName }).containsExactly("com.example.app2")
        assertThat(viewModel.page().draft).containsExactly("com.example.app1")
        assertThat(viewModel.page().counter).isEqualTo("1 application choisie")
    }

    @Test
    fun theEmptyStatesOfThePageSayWhatHappened() {
        appsSource.apps = apps(2)
        val viewModel = viewModel()
        viewModel.openPage()
        assertThat(viewModel.page().emptyMessage).isNull()

        viewModel.search("zzz")
        assertThat(viewModel.page().emptyMessage).isEqualTo(AppSelectionTexts.NO_MATCH)

        appsSource.apps = emptyList()
        val empty = viewModel()
        empty.openPage()
        assertThat(empty.page().emptyMessage).isEqualTo(AppSelectionTexts.NO_APPS)
    }

    @Test
    fun atFiftyTheUncheckedAppsAreOffAndTheCounterSaysItIsTheMaximum() {
        appsSource.apps = apps(AppSelectionSummary.MAX_COUNT + 5)
        val viewModel = viewModel()
        viewModel.openPage()
        repeat(AppSelectionSummary.MAX_COUNT) { index -> viewModel.toggle("com.example.app${index + 1}") }

        val page = viewModel.page()
        assertThat(page.isAtLimit).isTrue()
        assertThat(page.counter).isEqualTo(AppSelectionTexts.limitReached())
        assertThat(page.canValidate).isTrue()
        assertThat(page.items.filter { it.isSelected }.all { it.isEnabled }).isTrue()
        assertThat(page.items.filterNot { it.isSelected }.none { it.isEnabled }).isTrue()
    }

    @Test
    fun theFiftyFirstAppIsRefused() {
        appsSource.apps = apps(AppSelectionSummary.MAX_COUNT + 5)
        val viewModel = viewModel()
        viewModel.openPage()
        repeat(AppSelectionSummary.MAX_COUNT) { index -> viewModel.toggle("com.example.app${index + 1}") }

        viewModel.toggle("com.example.app${AppSelectionSummary.MAX_COUNT + 1}")

        assertThat(viewModel.page().draft).hasSize(AppSelectionSummary.MAX_COUNT)
    }

    @Test
    fun uncheckingOneAtTheLimitTurnsTheOthersBackOn() {
        appsSource.apps = apps(AppSelectionSummary.MAX_COUNT + 5)
        val viewModel = viewModel()
        viewModel.openPage()
        repeat(AppSelectionSummary.MAX_COUNT) { index -> viewModel.toggle("com.example.app${index + 1}") }

        viewModel.toggle("com.example.app1")

        val page = viewModel.page()
        assertThat(page.isAtLimit).isFalse()
        assertThat(page.items.all { it.isEnabled }).isTrue()
        assertThat(page.counter).isEqualTo("${AppSelectionSummary.MAX_COUNT - 1} applications choisies")
    }

    @Test
    fun aSavedAppThatWasUninstalledDisappears() {
        appsSource.apps = apps(3)
        store.stored =
            listOf(
                BlockedPackage("com.example.app2", "Application 2"),
                BlockedPackage("com.example.gone", "Disparue"),
            )

        val viewModel = viewModel()
        viewModel.openPage()

        assertThat(viewModel.state.saved.map { it.packageName }).containsExactly("com.example.app2")
        assertThat(viewModel.page().draft).containsExactly("com.example.app2")
    }

    @Test
    fun aDuplicatedLabelMakesBothTilesShowTheirPackageName() {
        appsSource.apps =
            listOf(
                InstalledApp("com.vendor.a.chat", "Chat", icon = null),
                InstalledApp("com.vendor.b.chat", "Chat", icon = null),
                InstalledApp("com.example.unique", "Unique", icon = null),
            )
        val viewModel = viewModel()
        viewModel.openPage()

        assertThat(
            viewModel
                .page()
                .items
                .filter { it.showPackageName }
                .map { it.app.packageName },
        ).containsExactly("com.vendor.a.chat", "com.vendor.b.chat")
    }

    @Test
    fun aSessionInProgressForbidsValidating() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.openPage()
        viewModel.toggle("com.example.app1")

        viewModel.onSessionStateChanged(isSessionInProgress = true)
        viewModel.validate()

        assertThat(viewModel.state.isSessionInProgress).isTrue()
        assertThat(viewModel.page().canValidate).isFalse()
        assertThat(store.replaceCallCount).isEqualTo(0)
    }

    @Test
    fun theOpenPageDraftAndSearchSurviveTheProcess() {
        appsSource.apps = apps(3)
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.openPage()
        first.toggle("com.example.app3")
        first.search("Application")

        val restored = viewModel(handle)

        assertThat(restored.page().draft).containsExactly("com.example.app3")
        assertThat(restored.page().query).isEqualTo("Application")
        assertThat(store.replaceCallCount).isEqualTo(0)
    }

    @Test
    fun aClosedPageStaysClosedAfterTheProcess() {
        appsSource.apps = apps(3)
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.openPage()
        first.toggle("com.example.app3")
        first.dismissPage()

        assertThat(viewModel(handle).state.page).isNull()
    }

    /** Relecture du 2026-10-22 : pendant le chargement, « Valider » écrivait une sélection vide. */
    @Test
    fun aDraftRestoredWhileTheAppsAreLoadingCannotBeValidated() {
        appsSource.apps = apps(3)
        store.stored = listOf(BlockedPackage("com.example.app1", "Application 1"))
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            openPage()
            toggle("com.example.app2")
        }
        appsSource.gate = CompletableDeferred()

        val restored = viewModel(handle)
        assertThat(restored.page().isLoading).isTrue()
        assertThat(restored.page().canValidate).isFalse()
        restored.validate()

        assertThat(store.replaceCallCount).isEqualTo(0)
        assertThat(store.stored).containsExactly(BlockedPackage("com.example.app1", "Application 1"))
    }

    @Test
    fun aRestoredDraftDropsAnAppUninstalledMeanwhile() {
        appsSource.apps = apps(3)
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            openPage()
            toggle("com.example.app3")
            toggle("com.example.app1")
        }
        appsSource.apps = apps(2)

        val restored = viewModel(handle)

        assertThat(restored.page().draft).containsExactly("com.example.app1")
    }

    @Test
    fun aSecondValidateDuringTheWriteDoesNotWriteTwice() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.openPage()
        viewModel.toggle("com.example.app1")
        val gate = CompletableDeferred<Unit>()
        store.gate = gate

        viewModel.validate()
        viewModel.validate()
        gate.complete(Unit)

        assertThat(store.replaceCallCount).isEqualTo(1)
        assertThat(viewModel.page().isSaved).isTrue()
    }

    /**
     * Section « Applications déjà choisies » en tête (décision de Mehdi du 2026-10-22) : les applications cochées à
     * l'ouverture, et elles seules ; la liste qui suit ne les répète pas.
     */
    @Test
    fun theChosenSectionHoldsTheAppsCheckedWhenThePageOpened() {
        appsSource.apps = apps(4)
        store.stored = listOf(BlockedPackage("com.example.app3", "Application 3"))
        val viewModel = viewModel()

        viewModel.openPage()

        assertThat(viewModel.page().chosenItems.map { it.app.packageName }).containsExactly("com.example.app3")
        assertThat(
            viewModel
                .page()
                .chosenItems
                .single()
                .isSelected,
        ).isTrue()
        assertThat(viewModel.page().items.map { it.app.packageName })
            .containsExactly("com.example.app1", "com.example.app2", "com.example.app4")
            .inOrder()
    }

    /** Rien ne saute sous le doigt : une case cochée ou décochée pendant la visite reste où elle est. */
    @Test
    fun nothingMovesWhileThePageIsOpen() {
        appsSource.apps = apps(4)
        store.stored = listOf(BlockedPackage("com.example.app3", "Application 3"))
        val viewModel = viewModel()
        viewModel.openPage()

        viewModel.toggle("com.example.app1")
        viewModel.toggle("com.example.app3")

        val page = viewModel.page()
        assertThat(page.chosenItems.map { it.app.packageName to it.isSelected })
            .containsExactly("com.example.app3" to false)
        assertThat(
            page.items
                .first()
                .app.packageName,
        ).isEqualTo("com.example.app1")
        assertThat(page.items.first().isSelected).isTrue()
    }

    @Test
    fun theSectionFollowsTheValidatedSelectionAtTheNextOpening() {
        appsSource.apps = apps(4)
        val viewModel = viewModel()
        viewModel.openPage()
        assertThat(viewModel.page().chosenItems).isEmpty()
        viewModel.toggle("com.example.app2")
        viewModel.validate()
        viewModel.dismissPage()

        viewModel.openPage()

        assertThat(viewModel.page().chosenItems.map { it.app.packageName }).containsExactly("com.example.app2")
    }

    @Test
    fun theSearchFiltersBothSections() {
        appsSource.apps = apps(12)
        store.stored =
            listOf(
                BlockedPackage("com.example.app1", "Application 1"),
                BlockedPackage("com.example.app2", "Application 2"),
            )
        val viewModel = viewModel()
        viewModel.openPage()

        viewModel.search("Application 1")

        assertThat(viewModel.page().chosenItems.map { it.app.label }).containsExactly("Application 1")
        assertThat(viewModel.page().items.map { it.app.label })
            .containsExactly("Application 10", "Application 11", "Application 12")
    }

    @Test
    fun theChosenSectionSurvivesTheProcess() {
        appsSource.apps = apps(3)
        store.stored = listOf(BlockedPackage("com.example.app2", "Application 2"))
        val handle = SavedStateHandle()
        viewModel(handle).openPage()

        val restored = viewModel(handle)

        assertThat(restored.page().chosenItems.map { it.app.packageName }).containsExactly("com.example.app2")
    }

    // Étape 45 : l'écran 4 rouvert depuis l'accueil (M11, décision 11 amendée le 2026-10-24).

    private fun snapshot(blockingApplied: Boolean) =
        SessionSnapshotDto(
            schemaVersion = 3,
            revision = 1,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule = WakeScheduleDto("2026-10-23", "07:00", "Europe/Paris", 2L),
            blockingSchedule = BlockingScheduleDto("2026-10-22", "22:30", 1L),
            state = SessionStateDto.ARMED,
            releaseTarget = null,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = 1L,
            armedAtEpochMillis = 1L,
            blockingAppliedAtEpochMillis = if (blockingApplied) 1L else null,
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
    fun beforeBedtimeAnArmedSessionLeavesTheSelectionEditable() {
        publisher.publish(snapshot(blockingApplied = false))

        assertThat(viewModel().state.isSessionInProgress).isFalse()
    }

    @Test
    fun fromBedtimeTheSelectionIsLocked() {
        publisher.publish(snapshot(blockingApplied = true))

        assertThat(viewModel().state.isSessionInProgress).isTrue()
    }

    @Test
    fun aValidatedSelectionIsArmedAgainWhenThePlanIsOn() {
        val enabled = WakePlan.DEFAULT.copy(enabled = true)
        kotlinx.coroutines.runBlocking { planStore.write(enabled) }
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.openPage()
        viewModel.toggle("com.example.app1")

        viewModel.validate()

        assertThat(scheduler.applied).containsExactly(enabled to PlanChangeReason.APP_SELECTION)
        assertThat(store.journal + journal).containsExactly("replace", "apply").inOrder()
    }

    /** Relecture : un réarmement qui échoue n'empêche pas d'enregistrer la sélection ni ne fait planter l'écran. */
    @Test
    fun aFailingRearmStillSavesTheSelection() {
        kotlinx.coroutines.runBlocking { planStore.write(WakePlan.DEFAULT.copy(enabled = true)) }
        scheduler.failure = java.io.IOException("disque")
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.openPage()
        viewModel.toggle("com.example.app1")

        viewModel.validate()

        assertThat(store.replaceCallCount).isEqualTo(1)
        assertThat(viewModel.state.saved.map { it.packageName }).containsExactly("com.example.app1")
        // La page se ferme comme après un enregistrement réussi.
        assertThat(viewModel.page().isSaved).isTrue()
    }

    @Test
    fun aValidatedSelectionArmsNothingWhenThePlanIsOff() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.openPage()
        viewModel.toggle("com.example.app1")

        viewModel.validate()

        assertThat(store.replaceCallCount).isEqualTo(1)
        assertThat(scheduler.applied).isEmpty()
    }

    @Test
    fun continuingTheSetupRecordsItsEnd() {
        var continued = false

        viewModel().completeSetup { continued = true }

        assertThat(setupPreferences.setupCompleted).isTrue()
        assertThat(continued).isTrue()
    }
}
