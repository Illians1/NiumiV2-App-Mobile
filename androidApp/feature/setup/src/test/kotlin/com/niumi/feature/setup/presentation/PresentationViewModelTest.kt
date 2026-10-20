package com.niumi.feature.setup.presentation

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.niumi.feature.setup.readiness.fakes.FakeSetupPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Écran 1 (`SPECIFICATION.md` §4) : deux panneaux qu'on ne peut pas passer. Le panneau 2 revient
 * au panneau 1, jamais au-delà ; l'accusé de lecture n'est écrit qu'une fois, par « Configurer
 * Niumi ».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PresentationViewModelTest {
    private val preferences = FakeSetupPreferences(onboardingAcknowledged = false)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = PresentationViewModel(preferences, handle)

    @Test
    fun theFirstPanelIsShownFirst() {
        assertThat(viewModel().state.panel).isEqualTo(1)
    }

    @Test
    fun continuingShowsTheSecondPanelAndBackReturnsToTheFirst() {
        val viewModel = viewModel()

        viewModel.next()
        assertThat(viewModel.state.panel).isEqualTo(2)

        viewModel.back()
        assertThat(viewModel.state.panel).isEqualTo(1)
    }

    @Test
    fun thereIsNoPanelBeyondTheSecondNorBeforeTheFirst() {
        val viewModel = viewModel()

        viewModel.back()
        assertThat(viewModel.state.panel).isEqualTo(1)

        viewModel.next()
        viewModel.next()
        assertThat(viewModel.state.panel).isEqualTo(2)
    }

    @Test
    fun configuringWritesTheAcknowledgementThenLeaves() {
        val viewModel = viewModel()
        var left = 0

        viewModel.next()
        viewModel.acknowledge { left++ }

        assertThat(preferences.acknowledgeCallCount).isEqualTo(1)
        assertThat(left).isEqualTo(1)
    }

    /** Un double tap sur « Configurer Niumi » n'écrit ni ne navigue deux fois. */
    @Test
    fun aSecondTapIsIgnored() {
        val viewModel = viewModel()
        var left = 0

        viewModel.next()
        viewModel.acknowledge { left++ }
        viewModel.acknowledge { left++ }

        assertThat(preferences.acknowledgeCallCount).isEqualTo(1)
        assertThat(left).isEqualTo(1)
    }

    /** « Configurer Niumi » n'existe que sur le panneau 2 : le panneau 1 ne peut pas être sauté. */
    @Test
    fun theFirstPanelCannotAcknowledge() {
        val viewModel = viewModel()
        var left = 0

        viewModel.acknowledge { left++ }

        assertThat(preferences.acknowledgeCallCount).isEqualTo(0)
        assertThat(left).isEqualTo(0)
    }

    /** Après une mort du processus, Android rend le panneau affiché par le `SavedStateHandle`. */
    @Test
    fun thePanelSurvivesAProcessDeath() {
        val handle = SavedStateHandle()
        viewModel(handle).next()

        assertThat(viewModel(handle).state.panel).isEqualTo(2)
    }
}
