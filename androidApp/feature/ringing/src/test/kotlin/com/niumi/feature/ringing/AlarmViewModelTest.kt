package com.niumi.feature.ringing

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.feature.ringing.fakes.FakeSessionPersistenceGateway
import com.niumi.feature.ringing.fakes.TEST_SESSION_ID
import com.niumi.feature.ringing.fakes.presentSession
import com.niumi.feature.ringing.fakes.snapshotInState
import com.niumi.feature.ringing.ui.AlarmExitDestination
import com.niumi.feature.ringing.ui.AlarmUiState
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.ScanFailureReason
import com.niumi.system.nfc.ScanOutcome
import com.niumi.system.nfc.ScanSheetState
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionHistoryReader
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

    private val history = mutableMapOf<String, SessionSnapshotDto>()

    private fun viewModel() = AlarmViewModel(gateway, publisher, SessionHistoryReader { history[it] })

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
    fun releasingShowsTheLoader() =
        runTest {
            gateway.result = presentSession(SessionStateDto.RELEASING)

            assertThat((viewModel().state as AlarmUiState.Visible).screen.showsReleaseLoader).isTrue()
        }

    @Test
    fun aFinalStateLeavesForItsScreen() =
        runTest {
            gateway.result = presentSession(SessionStateDto.COMPLETED)
            assertThat(viewModel().state).isEqualTo(AlarmUiState.Exit(AlarmExitDestination.Success(TEST_SESSION_ID)))

            gateway.result = presentSession(SessionStateDto.CANCELLED)
            assertThat(viewModel().state).isEqualTo(AlarmUiState.Exit(AlarmExitDestination.Home))
        }

    /**
     * Étape 46 (constat de l'étape 44) : après le scan, le crochet du planning arme la session
     * suivante sous le même verrou, et le publisher passe de `COMPLETED` à `ARMED` (autre
     * identifiant) avant que l'écran ait vu la fin. La session suivie, relue, mène à l'écran 11 —
     * jamais à une simple fermeture.
     */
    @Test
    fun theNextSessionArmedRightAfterTheScanStillLeadsToTheSuccessScreen() =
        runTest {
            val viewModel = ringingViewModel()
            publisher.publish(snapshotInState(SessionStateDto.RELEASING))
            history[TEST_SESSION_ID] = snapshotInState(SessionStateDto.COMPLETED)

            publisher.publish(snapshotInState(SessionStateDto.ARMED).copy(sessionId = NEXT_SESSION_ID))

            assertThat(viewModel.state).isEqualTo(AlarmUiState.Exit(AlarmExitDestination.Success(TEST_SESSION_ID)))
        }

    /** La fin est retenue : une publication suivante ne la remplace pas par une fermeture. */
    @Test
    fun theExitIsKeptWhenAnotherSnapshotFollows() =
        runTest {
            val viewModel = ringingViewModel()
            history[TEST_SESSION_ID] = snapshotInState(SessionStateDto.COMPLETED)
            publisher.publish(snapshotInState(SessionStateDto.ARMED).copy(sessionId = NEXT_SESSION_ID))

            publisher.publish(snapshotInState(SessionStateDto.ARMED, revision = 3).copy(sessionId = NEXT_SESSION_ID))

            assertThat(viewModel.state).isEqualTo(AlarmUiState.Exit(AlarmExitDestination.Success(TEST_SESSION_ID)))
        }

    /** Une session suivie introuvable (cas défensif) garde l'ancien comportement : fermeture. */
    @Test
    fun anUnreadableTrackedSessionFallsBackToClosing() =
        runTest {
            val viewModel = ringingViewModel()

            publisher.publish(snapshotInState(SessionStateDto.ARMED).copy(sessionId = NEXT_SESSION_ID))

            assertThat(viewModel.state).isEqualTo(AlarmUiState.Close)
        }

    /** Ouvert sans session de sonnerie (session armée pour ce soir) : fermeture, comme avant. */
    @Test
    fun anArmedSessionAtOpeningClosesTheScreen() =
        runTest {
            gateway.result = presentSession(SessionStateDto.ARMED)

            assertThat(viewModel().state).isEqualTo(AlarmUiState.Close)
        }

    private val AlarmViewModel.screen get() = (state as AlarmUiState.Visible).screen

    private fun ringingViewModel(): AlarmViewModel {
        gateway.result = presentSession(SessionStateDto.RINGING)
        return viewModel()
    }

    /** Q4 (2026-10-22) : à l'arrivée, aucune feuille et aucune lecture NFC. */
    @Test
    fun theScreenOpensWithoutASheetAndWithoutReading() =
        runTest {
            val viewModel = ringingViewModel()

            assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Hidden)
            assertThat(viewModel.readerModeRequested).isFalse()
        }

    @Test
    fun scanOpensTheReadySheetAndRequestsTheReader() =
        runTest {
            val viewModel = ringingViewModel()

            viewModel.openScanSheet()

            assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Ready)
            assertThat(viewModel.readerModeRequested).isTrue()
        }

    @Test
    fun cancelClosesTheSheetAndStopsReading() =
        runTest {
            val viewModel = ringingViewModel()
            viewModel.openScanSheet()

            viewModel.cancelScan()

            assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Hidden)
            assertThat(viewModel.readerModeRequested).isFalse()
        }

    @Test
    fun scanWithNfcOffOpensTheNfcFailureWithoutReading() =
        runTest {
            val viewModel = ringingViewModel()
            viewModel.onNfcAvailabilityChanged(NfcAvailability.DISABLED)

            viewModel.openScanSheet()

            assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED))
            assertThat(viewModel.readerModeRequested).isFalse()
        }

    @Test
    fun aRefusedScanShowsItsFailureAndStopsReading() =
        runTest {
            val cases =
                mapOf(
                    ScanOutcome.NotANiumiSpot to ScanFailureReason.NOT_A_NIUMI_SPOT,
                    ScanOutcome.UnknownBox to ScanFailureReason.NOT_REGISTERED,
                    ScanOutcome.Unreadable to ScanFailureReason.UNREADABLE,
                )
            cases.forEach { (outcome, reason) ->
                val viewModel = ringingViewModel()
                viewModel.openScanSheet()

                viewModel.onScanOutcome(outcome)

                assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Failed(reason))
                assertThat(viewModel.readerModeRequested).isFalse()
            }
        }

    @Test
    fun retryReopensTheReadySheet() =
        runTest {
            val viewModel = ringingViewModel()
            viewModel.openScanSheet()
            viewModel.onScanOutcome(ScanOutcome.Unreadable)

            viewModel.retryScan()

            assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Ready)
            assertThat(viewModel.readerModeRequested).isTrue()
        }

    /** Un scan accepté ou ignoré ne change pas la feuille : c'est le snapshot qui la fermera. */
    @Test
    fun anAcceptedOrIgnoredScanKeepsTheReadySheet() =
        runTest {
            val viewModel = ringingViewModel()
            viewModel.openScanSheet()

            viewModel.onScanOutcome(ScanOutcome.Accepted)
            viewModel.onScanOutcome(ScanOutcome.Ignored)

            assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Ready)
        }

    /** Un résultat arrivé après « Annuler » (lecture déjà partie) n'ouvre pas de feuille. */
    @Test
    fun anOutcomeArrivingWithTheSheetClosedIsIgnored() =
        runTest {
            val viewModel = ringingViewModel()

            viewModel.onScanOutcome(ScanOutcome.UnknownBox)

            assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Hidden)
        }

    @Test
    fun nfcTurnedOffWhileReadyShowsTheNfcFailure() =
        runTest {
            val viewModel = ringingViewModel()
            viewModel.openScanSheet()

            viewModel.onNfcAvailabilityChanged(NfcAvailability.DISABLED)

            assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED))
            assertThat(viewModel.readerModeRequested).isFalse()
        }

    /**
     * SPEC_ANDROID §11.2 (étape 31, vigilance 13) : juste après un démarrage, le NFC se lit coupé
     * puis revient. L'état stable « activé » remplace seul l'échec par « Prêt à scanner » et la
     * lecture reprend : effacer le message sans écouter inviterait à scanner un écran sourd.
     */
    @Test
    fun anAdapterThatSettlesOnTurnsTheNfcFailureBackIntoReading() =
        runTest {
            val viewModel = ringingViewModel()
            viewModel.onNfcAvailabilityChanged(NfcAvailability.DISABLED)
            viewModel.openScanSheet()

            viewModel.onNfcAvailabilityChanged(NfcAvailability.ENABLED)

            assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Ready)
            assertThat(viewModel.readerModeRequested).isTrue()
        }

    /** Un autre échec n'est pas effacé par le retour du NFC : il attend « Réessayer ». */
    @Test
    fun anAdapterThatSettlesOnKeepsAnotherFailure() =
        runTest {
            val viewModel = ringingViewModel()
            viewModel.openScanSheet()
            viewModel.onScanOutcome(ScanOutcome.UnknownBox)

            viewModel.onNfcAvailabilityChanged(NfcAvailability.ENABLED)

            assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Failed(ScanFailureReason.NOT_REGISTERED))
        }

    @Test
    fun releasingClosesTheSheetAndStopsReading() =
        runTest {
            val viewModel = ringingViewModel()
            viewModel.openScanSheet()

            publisher.publish(snapshotInState(SessionStateDto.RELEASING))

            assertThat(viewModel.screen.sheet).isEqualTo(ScanSheetState.Hidden)
            assertThat(viewModel.readerModeRequested).isFalse()
        }

    @Test
    fun aFinalStateStopsReading() =
        runTest {
            val viewModel = ringingViewModel()
            viewModel.openScanSheet()

            publisher.publish(snapshotInState(SessionStateDto.COMPLETED))

            assertThat(viewModel.readerModeRequested).isFalse()
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

            viewModel.openScanSheet()
            viewModel.onScanOutcome(ScanOutcome.UnknownBox)
            viewModel.retryScan()
            viewModel.onNfcAvailabilityChanged(NfcAvailability.DISABLED)
            viewModel.cancelScan()
            publisher.publish(snapshotInState(SessionStateDto.RELEASING))

            assertThat(viewModel.state).isInstanceOf(AlarmUiState.Visible::class.java)
        }

    private companion object {
        const val NEXT_SESSION_ID = "22222222-2222-2222-2222-222222222222"
    }
}
