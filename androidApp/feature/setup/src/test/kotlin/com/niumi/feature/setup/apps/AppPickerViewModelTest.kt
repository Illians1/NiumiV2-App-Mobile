package com.niumi.feature.setup.apps

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.AppSelectionSummary
import com.niumi.database.BlockedPackage
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.apps.InstalledApp
import com.niumi.system.apps.InstalledAppsSource
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
 * SPEC_ANDROID §12.1 : la sélection doit contenir entre 1 et 50 applications, l'écran bloque la
 * confirmation à 0 et refuse la 51ᵉ. Les bornes ne sont jamais réécrites ici : elles viennent de
 * `AppSelectionSummary` (`:shared:core`), seule autorité de la règle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppPickerViewModelTest {
    private class FakeInstalledAppsSource(
        var apps: List<InstalledApp> = emptyList(),
    ) : InstalledAppsSource {
        override suspend fun launchableApps(): List<InstalledApp> = apps
    }

    private class FakeAppSelectionStore(
        private var stored: List<BlockedPackage> = emptyList(),
    ) : AppSelectionStore {
        var replaceCallCount = 0

        override suspend fun selectedCount(): Int = stored.size

        override suspend fun selection(): List<BlockedPackage> = stored

        override suspend fun replace(selection: List<BlockedPackage>) {
            replaceCallCount++
            stored = selection
        }
    }

    private val appsSource = FakeInstalledAppsSource()
    private val store = FakeAppSelectionStore()

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

    private fun viewModel(): AppPickerViewModel = AppPickerViewModel(appsSource, store, SessionSnapshotPublisher())

    @Test
    fun anEmptySelectionCannotBeConfirmed() {
        appsSource.apps = apps(3)

        val state = viewModel().state

        assertThat(state.selectedCount).isEqualTo(0)
        assertThat(state.canConfirm).isFalse()
    }

    @Test
    fun aSingleSelectedApplicationIsEnoughToConfirm() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()

        viewModel.toggle("com.example.app1")

        assertThat(viewModel.state.selectedCount).isEqualTo(AppSelectionSummary.MIN_COUNT)
        assertThat(viewModel.state.canConfirm).isTrue()
    }

    @Test
    fun exactlyFiftyApplicationsCanBeConfirmed() {
        appsSource.apps = apps(AppSelectionSummary.MAX_COUNT + 5)
        val viewModel = viewModel()

        repeat(AppSelectionSummary.MAX_COUNT) { index -> viewModel.toggle("com.example.app${index + 1}") }

        assertThat(viewModel.state.selectedCount).isEqualTo(AppSelectionSummary.MAX_COUNT)
        assertThat(viewModel.state.canConfirm).isTrue()
    }

    @Test
    fun theFiftyFirstApplicationIsRefusedWithAMessageAndDoesNotChangeTheSelection() {
        appsSource.apps = apps(AppSelectionSummary.MAX_COUNT + 5)
        val viewModel = viewModel()
        repeat(AppSelectionSummary.MAX_COUNT) { index -> viewModel.toggle("com.example.app${index + 1}") }

        viewModel.toggle("com.example.app${AppSelectionSummary.MAX_COUNT + 1}")

        assertThat(viewModel.state.selectedCount).isEqualTo(AppSelectionSummary.MAX_COUNT)
        assertThat(viewModel.state.message).isEqualTo(AppPickerTexts.TOO_MANY_MESSAGE)
        assertThat(viewModel.state.canConfirm).isTrue()
    }

    @Test
    fun deselectingBelowTheLimitClearsTheRefusalMessage() {
        appsSource.apps = apps(AppSelectionSummary.MAX_COUNT + 5)
        val viewModel = viewModel()
        repeat(AppSelectionSummary.MAX_COUNT) { index -> viewModel.toggle("com.example.app${index + 1}") }
        viewModel.toggle("com.example.app${AppSelectionSummary.MAX_COUNT + 1}")

        viewModel.toggle("com.example.app1")

        assertThat(viewModel.state.message).isNull()
        assertThat(viewModel.state.selectedCount).isEqualTo(AppSelectionSummary.MAX_COUNT - 1)
    }

    @Test
    fun confirmingPersistsPackagesWithTheirFrozenLabels() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.toggle("com.example.app2")

        viewModel.confirm()

        assertThat(store.replaceCallCount).isEqualTo(1)
        assertThat(store.selectionBlocking()).containsExactly(BlockedPackage("com.example.app2", "Application 2"))
    }

    @Test
    fun confirmingAnInvalidSelectionPersistsNothing() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()

        viewModel.confirm()

        assertThat(store.replaceCallCount).isEqualTo(0)
        assertThat(viewModel.state.message).isEqualTo(AppPickerTexts.NONE_SELECTED_MESSAGE)
    }

    @Test
    fun aPreviouslySavedSelectionIsRestoredEvenIfAnAppWasUninstalled() {
        appsSource.apps = apps(3)
        val store =
            FakeAppSelectionStore(
                listOf(
                    BlockedPackage("com.example.app2", "Application 2"),
                    BlockedPackage("com.example.gone", "Disparue"),
                ),
            )

        val state = AppPickerViewModel(appsSource, store, SessionSnapshotPublisher()).state

        assertThat(state.items.filter { it.isSelected }.map { it.app.packageName })
            .containsExactly("com.example.app2")
    }

    @Test
    fun aDuplicatedLabelMakesBothRowsShowTheirPackageName() {
        appsSource.apps =
            listOf(
                InstalledApp("com.vendor.a.chat", "Chat", icon = null),
                InstalledApp("com.vendor.b.chat", "Chat", icon = null),
                InstalledApp("com.example.unique", "Unique", icon = null),
            )

        val items = viewModel().state.items

        assertThat(items.filter { it.showPackageName }.map { it.app.packageName })
            .containsExactly("com.vendor.a.chat", "com.vendor.b.chat")
    }

    @Test
    fun aSessionInProgressBlocksConfirmation() {
        appsSource.apps = apps(3)
        val viewModel = viewModel()
        viewModel.toggle("com.example.app1")

        viewModel.onSessionStateChanged(isSessionInProgress = true)

        assertThat(viewModel.state.canConfirm).isFalse()
        assertThat(viewModel.state.message).isEqualTo(AppPickerTexts.SESSION_IN_PROGRESS)
    }

    private fun FakeAppSelectionStore.selectionBlocking(): List<BlockedPackage> =
        kotlinx.coroutines.runBlocking { selection() }
}
