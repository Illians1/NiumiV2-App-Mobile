package com.niumi.feature.ringing

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.feature.ringing.fakes.FakeSessionPersistenceGateway
import com.niumi.feature.ringing.fakes.pendingEffect
import com.niumi.feature.ringing.fakes.presentSession
import com.niumi.feature.ringing.fakes.snapshotInState
import com.niumi.feature.ringing.ui.AlarmExitDestination
import com.niumi.feature.ringing.ui.AlarmUiState
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionSnapshotPublisher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * État de l'écran de réveil (SPEC_ANDROID §10.4). Le cas décisif est le premier : dans un
 * processus neuf — celui de tout réveil — le publisher vaut `null`, et l'écran ne doit surtout
 * pas s'en déduire qu'il n'y a pas de session.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlarmViewModelTest {
    private val gateway = FakeSessionPersistenceGateway()
    private val publisher = SessionSnapshotPublisher()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AlarmViewModel(gateway, publisher)

    @Test
    fun aNullPublisherDoesNotCloseTheScreenWhenThePersistenceHasASession() =
        runTest {
            gateway.result = presentSession(SessionStateDto.RINGING)

            val state = viewModel().state

            assertThat(state).isInstanceOf(AlarmUiState.Visible::class.java)
            assertThat((state as AlarmUiState.Visible).screen.sessionState)
                .isEqualTo(SessionStateDto.RINGING)
        }

    @Test
    fun theScreenFollowsTheSnapshotsPublishedAfterwards() =
        runTest {
            gateway.result = presentSession(SessionStateDto.RINGING)
            val viewModel = viewModel()

            publisher.publish(snapshotInState(SessionStateDto.TRIGGERED_AWAITING_NFC))

            assertThat((viewModel.state as AlarmUiState.Visible).screen.sessionState)
                .isEqualTo(SessionStateDto.TRIGGERED_AWAITING_NFC)
        }

    @Test
    fun aConfirmedAbsenceClosesTheScreen() =
        runTest {
            gateway.result = LoadResult.Absent

            assertThat(viewModel().state).isEqualTo(AlarmUiState.Close)
        }

    /** SPEC_CORE_KMP §13 : un snapshot illisible ne se lit jamais « pas de session ». */
    @Test
    fun anUnreadableSnapshotDoesNotCloseTheScreen() =
        runTest {
            gateway.result = LoadResult.Unreadable("SNAPSHOT_CORRUPTED")

            assertThat(viewModel().state).isEqualTo(AlarmUiState.Loading)
        }

    @Test
    fun releasingCarriesTheProgressReadFromTheOutbox() =
        runTest {
            gateway.result =
                presentSession(
                    SessionStateDto.RELEASING,
                    pendingEffects = listOf(pendingEffect(SessionEffectKindDto.REMOVE_BLOCKING)),
                )

            val screen = (viewModel().state as AlarmUiState.Visible).screen

            assertThat(screen.releaseSteps.single { it.kind == SessionEffectKindDto.REMOVE_BLOCKING }.done)
                .isFalse()
            assertThat(screen.releaseSteps.single { it.kind == SessionEffectKindDto.CANCEL_ALARM }.done)
                .isTrue()
        }

    @Test
    fun aFinalStateLeavesForItsScreen() =
        runTest {
            gateway.result = presentSession(SessionStateDto.COMPLETED)
            assertThat(viewModel().state).isEqualTo(AlarmUiState.Exit(AlarmExitDestination.COMPLETED))

            gateway.result = presentSession(SessionStateDto.CANCELLED)
            assertThat(viewModel().state).isEqualTo(AlarmUiState.Exit(AlarmExitDestination.CANCELLED))
        }

    @Test
    fun theDeviceInputsAreMergedIntoTheDisplayedState() =
        runTest {
            gateway.result = presentSession(SessionStateDto.RINGING)
            val viewModel = viewModel()

            viewModel.onDeviceLockChanged(locked = true)
            assertThat((viewModel.state as AlarmUiState.Visible).screen.instructionText)
                .isEqualTo("Déverrouille ton téléphone, puis approche-le du boîtier.")

            viewModel.onNfcAvailabilityChanged(NfcAvailability.DISABLED)
            assertThat((viewModel.state as AlarmUiState.Visible).screen.showsNfcSettingsShortcut)
                .isTrue()
        }

    /**
     * Le double lève sur toute écriture : si le ViewModel dispatchait ou committait quoi que ce
     * soit, ces scénarios échoueraient. L'écran de réveil observe, il ne décide pas (§3, §11.3).
     */
    @Test
    fun theViewModelNeverWritesToTheSession() =
        runTest {
            gateway.result = presentSession(SessionStateDto.RINGING)
            val viewModel = viewModel()

            publisher.publish(snapshotInState(SessionStateDto.RELEASING))
            viewModel.onDeviceLockChanged(locked = true)

            assertThat(viewModel.state).isInstanceOf(AlarmUiState.Visible::class.java)
        }
}
