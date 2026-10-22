package com.niumi.feature.setup.permissions

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.feature.setup.presentation.fakes.FakeSetupPreferences
import com.niumi.system.permissions.NiumiPermission
import com.niumi.system.permissions.PermissionStatus
import com.niumi.system.permissions.PermissionStatusSource
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheck
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Écran 2 (`SPECIFICATION.md` §5, SPEC_ANDROID §13) : chaque tuile reflète les contrôles réels,
 * « Continuer » n'est actif qu'avec les trois, et « Autoriser » traite un seul manque à la fois.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PermissionsViewModelTest {
    private var statuses: List<PermissionStatus> = allGranted()

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        // `viewModelScope` poste sur `Dispatchers.Main` ; `Unconfined` rend `refresh()` synchrone.
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val preferences = FakeSetupPreferences()

    private fun viewModel(
        source: PermissionStatusSource =
            PermissionStatusSource {
                statuses
            },
    ) = PermissionsViewModel(source, preferences)

    @Test
    fun everyGrantedPermissionLetsTheUserContinue() {
        val viewModel = viewModel()

        assertThat(viewModel.state.tiles.map { it.granted }).containsExactly(true, true, true, true, true)
        assertThat(viewModel.state.canContinue).isTrue()
    }

    @Test
    fun aMissingPermissionUnchecksItsTileAndNothingElse() {
        statuses = withPending(NiumiPermission.NOTIFICATIONS, ReadinessCheckId.NOTIFICATIONS)

        val viewModel = viewModel()

        assertThat(viewModel.state.tiles.map { it.permission to it.granted })
            .containsExactly(
                NiumiPermission.ALARM to true,
                NiumiPermission.APP_BLOCKING to true,
                NiumiPermission.BATTERY to true,
                NiumiPermission.RECENTS_LOCK to true,
                NiumiPermission.NOTIFICATIONS to false,
            ).inOrder()
        assertThat(viewModel.state.canContinue).isFalse()
        assertThat(viewModel.state.dialog).isNull()
    }

    @Test
    fun nothingCanContinueBeforeTheFirstReading() {
        val gate = CompletableDeferred<List<PermissionStatus>>()

        val viewModel = viewModel { gate.await() }

        assertThat(viewModel.state.isLoading).isTrue()
        assertThat(viewModel.state.canContinue).isFalse()
    }

    @Test
    fun aGrantPickedUpOnReturnFromTheSettingsChecksTheTile() {
        statuses = withPending(NiumiPermission.APP_BLOCKING, ReadinessCheckId.ACCESSIBILITY_SERVICE)
        val viewModel = viewModel()

        statuses = allGranted()
        viewModel.refresh()

        assertThat(viewModel.state.canContinue).isTrue()
    }

    @Test
    fun aPermissionWithdrawnAfterwardsIsSeenAtTheNextReading() {
        val viewModel = viewModel()

        statuses = withPending(NiumiPermission.APP_BLOCKING, ReadinessCheckId.ACCESSIBILITY_SERVICE)
        viewModel.refresh()

        assertThat(viewModel.state.canContinue).isFalse()
    }

    @Test
    fun aStaleReadingNeverOverwritesTheLatestOne() {
        val slow = CompletableDeferred<List<PermissionStatus>>()
        var calls = 0
        val viewModel =
            viewModel {
                calls++
                if (calls == 1) slow.await() else allGranted()
            }

        viewModel.refresh()
        slow.complete(withPending(NiumiPermission.ALARM, ReadinessCheckId.EXACT_ALARM))

        assertThat(viewModel.state.canContinue).isTrue()
    }

    @Test
    fun authorizingTheMissingAccessibilityServiceShowsTheConsentWindowFirst() {
        // Divulgation Google Play (§12.3) avant tout accès aux réglages, dans une fenêtre centrée
        // de l'écran 2 (décision du 2026-10-21).
        statuses = withPending(NiumiPermission.APP_BLOCKING, ReadinessCheckId.ACCESSIBILITY_SERVICE)
        val viewModel = viewModel()

        val step = viewModel.authorize(NiumiPermission.APP_BLOCKING)

        assertThat(step).isEqualTo(PermissionStep.None)
        assertThat(viewModel.state.dialog).isEqualTo(PermissionDialog.CONSENT)
    }

    @Test
    fun theConsentWindowClosesItselfOnceTheServiceIsBound() {
        statuses = withPending(NiumiPermission.APP_BLOCKING, ReadinessCheckId.ACCESSIBILITY_SERVICE)
        val viewModel = viewModel()
        viewModel.authorize(NiumiPermission.APP_BLOCKING)

        statuses = allGranted()
        viewModel.refresh()

        assertThat(viewModel.state.dialog).isNull()
        assertThat(viewModel.state.canContinue).isTrue()
    }

    @Test
    fun authorizingTheBatteryExemptionOpensTheSystemListDirectly() {
        // Décision du 2026-10-21 : la tuile « Batterie » dit déjà pourquoi, pas de feuille.
        statuses = withPending(NiumiPermission.BATTERY, ReadinessCheckId.BATTERY_OPTIMIZATION)
        val viewModel = viewModel()

        val step = viewModel.authorize(NiumiPermission.BATTERY)

        assertThat(step).isEqualTo(PermissionStep.OpenSettings(ReadinessAction.OpenBatterySettings))
        assertThat(viewModel.state.dialog).isNull()
    }

    @Test
    fun authorizingTheRecentsLockShowsItsDialog() {
        statuses = withPending(NiumiPermission.RECENTS_LOCK, ReadinessCheckId.RECENTS_LOCK)
        val viewModel = viewModel()

        viewModel.authorize(NiumiPermission.RECENTS_LOCK)

        assertThat(viewModel.state.dialog).isEqualTo(PermissionDialog.RECENTS_LOCK)
    }

    @Test
    fun authorizingTheExactAlarmOnlyExplainsSinceNothingCanBeAsked() {
        statuses = withPending(NiumiPermission.ALARM, ReadinessCheckId.EXACT_ALARM)
        val viewModel = viewModel()

        val step = viewModel.authorize(NiumiPermission.ALARM)

        assertThat(step).isEqualTo(PermissionStep.None)
        assertThat(viewModel.state.dialog).isEqualTo(PermissionDialog.EXACT_ALARM)
    }

    @Test
    fun authorizingNotificationsAsksTheSystemThenTheAlarmChannelSettings() {
        statuses = withPending(NiumiPermission.NOTIFICATIONS, ReadinessCheckId.NOTIFICATIONS)
        assertThat(viewModel().authorize(NiumiPermission.NOTIFICATIONS))
            .isEqualTo(PermissionStep.RequestNotifications)

        statuses = withPending(NiumiPermission.NOTIFICATIONS, ReadinessCheckId.ALARM_CHANNEL)
        assertThat(viewModel().authorize(NiumiPermission.NOTIFICATIONS))
            .isEqualTo(PermissionStep.OpenSettings(ReadinessAction.OpenChannelSettings(CHANNEL)))
    }

    /**
     * Relecture de l'étape 36 : avant Android 13, le contrôle « notifications » porte l'action du
     * canal du réveil. « Autoriser » doit pourtant ouvrir les réglages de notification de Niumi,
     * seuls à pouvoir les rétablir quand elles sont toutes coupées.
     */
    @Test
    fun notificationsBeforeAndroid13StillGoThroughTheNotificationRequest() {
        statuses =
            withPending(NiumiPermission.NOTIFICATIONS, ReadinessCheckId.NOTIFICATIONS).map {
                if (it.permission == NiumiPermission.NOTIFICATIONS) {
                    it.copy(pending = it.pending!!.copy(action = ReadinessAction.OpenChannelSettings(CHANNEL)))
                } else {
                    it
                }
            }

        assertThat(viewModel().authorize(NiumiPermission.NOTIFICATIONS)).isEqualTo(PermissionStep.RequestNotifications)
    }

    @Test
    fun theNotificationRequestIsRememberedOnceMarked() {
        val viewModel = viewModel()
        assertThat(viewModel.state.notificationRequestedBefore).isFalse()

        viewModel.markNotificationPermissionRequested()

        assertThat(viewModel.state.notificationRequestedBefore).isTrue()
        assertThat(preferences.notificationRequested).isTrue()
    }

    @Test
    fun aRequestMadeInAnEarlierVisitIsReadBack() {
        preferences.notificationRequested = true

        assertThat(viewModel().state.notificationRequestedBefore).isTrue()
    }

    @Test
    fun authorizingAGrantedTileDoesNothing() {
        val viewModel = viewModel()

        assertThat(viewModel.authorize(NiumiPermission.ALARM)).isEqualTo(PermissionStep.None)
        assertThat(viewModel.state.dialog).isNull()
    }

    @Test
    fun confirmingTheRecentsLockClosesTheDialogAndReadsAgain() {
        statuses = withPending(NiumiPermission.RECENTS_LOCK, ReadinessCheckId.RECENTS_LOCK)
        val viewModel = viewModel()
        viewModel.authorize(NiumiPermission.RECENTS_LOCK)

        statuses = allGranted()
        viewModel.confirmRecentsLock()

        assertThat(viewModel.state.dialog).isNull()
        assertThat(viewModel.state.canContinue).isTrue()
    }

    @Test
    fun aRecentsLockStillMissingAfterConfirmationLeavesTheTileUnchecked() {
        // Le bouton ne fait que relire : sans cadenas posé, la tuile reste à faire (§13, étape 25).
        statuses = withPending(NiumiPermission.RECENTS_LOCK, ReadinessCheckId.RECENTS_LOCK)
        val viewModel = viewModel()
        viewModel.authorize(NiumiPermission.RECENTS_LOCK)

        viewModel.confirmRecentsLock()

        assertThat(viewModel.state.dialog).isNull()
        assertThat(viewModel.state.canContinue).isFalse()
    }

    @Test
    fun closingAWindowReadsTheChecksAgainAndChangesNothingElse() {
        // Croix ou toucher à côté : l'utilisateur a pu régler quelque chose entre-temps.
        statuses = withPending(NiumiPermission.RECENTS_LOCK, ReadinessCheckId.RECENTS_LOCK)
        var reads = 0
        val viewModel =
            viewModel {
                reads++
                statuses
            }
        viewModel.authorize(NiumiPermission.RECENTS_LOCK)
        val readsBefore = reads

        viewModel.dismissDialog()

        assertThat(viewModel.state.dialog).isNull()
        assertThat(viewModel.state.canContinue).isFalse()
        assertThat(reads).isEqualTo(readsBefore + 1)
    }

    @Test
    fun aDialogWhoseCheckGotFixedMeanwhileClosesItself() {
        statuses = withPending(NiumiPermission.RECENTS_LOCK, ReadinessCheckId.RECENTS_LOCK)
        val viewModel = viewModel()
        viewModel.authorize(NiumiPermission.RECENTS_LOCK)

        statuses = allGranted()
        viewModel.refresh()

        assertThat(viewModel.state.dialog).isNull()
    }

    @Test
    fun anOpenDialogStaysWhileAnotherTileAlsoMissesSomething() {
        statuses =
            withPending(NiumiPermission.RECENTS_LOCK, ReadinessCheckId.RECENTS_LOCK)
                .map {
                    if (it.permission ==
                        NiumiPermission.ALARM
                    ) {
                        it.copy(granted = false, pending = failed(ReadinessCheckId.EXACT_ALARM))
                    } else {
                        it
                    }
                }
        val viewModel = viewModel()
        viewModel.authorize(NiumiPermission.RECENTS_LOCK)

        viewModel.refresh()

        assertThat(viewModel.state.dialog).isEqualTo(PermissionDialog.RECENTS_LOCK)
    }

    /**
     * Relecture de l'étape 36 : un service d'accessibilité pas encore jugeable (fenêtre de liaison)
     * laisse la tuile à faire, et l'écran relit de lui-même jusqu'au verdict.
     */
    @Test
    fun aTileAwaitingJudgementIsReadAgainUntilItIsJudged() {
        statuses = awaitingAccessibility()
        val viewModel = viewModel()
        assertThat(viewModel.state.canContinue).isFalse()

        statuses = allGranted()
        dispatcher.scheduler.advanceTimeBy(PermissionsViewModel.AWAITING_JUDGEMENT_RECHECK_MILLIS + 1)

        assertThat(viewModel.state.canContinue).isTrue()
    }

    @Test
    fun nothingIsReadAgainOnceEveryTileIsJudged() {
        var reads = 0
        statuses = withPending(NiumiPermission.APP_BLOCKING, ReadinessCheckId.ACCESSIBILITY_SERVICE)
        viewModel {
            reads++
            statuses
        }

        dispatcher.scheduler.advanceTimeBy(PermissionsViewModel.AWAITING_JUDGEMENT_RECHECK_MILLIS * 3)

        assertThat(reads).isEqualTo(1)
    }

    @Test
    fun authorizingAnAccessibilityServiceAwaitingJudgementStillShowsTheConsentWindow() {
        statuses = awaitingAccessibility()

        val viewModel = viewModel()

        assertThat(viewModel.authorize(NiumiPermission.APP_BLOCKING)).isEqualTo(PermissionStep.None)
        assertThat(viewModel.state.dialog).isEqualTo(PermissionDialog.CONSENT)
    }

    private companion object {
        const val CHANNEL = "niumi_alarm_ringing"

        fun allGranted(): List<PermissionStatus> =
            NiumiPermission.entries.map {
                PermissionStatus(it, granted = true, pending = null)
            }

        fun withPending(
            permission: NiumiPermission,
            id: ReadinessCheckId,
        ): List<PermissionStatus> =
            NiumiPermission.entries.map {
                if (it == permission) {
                    PermissionStatus(it, granted = false, pending = failed(id))
                } else {
                    PermissionStatus(it, granted = true, pending = null)
                }
            }

        fun awaitingAccessibility(): List<PermissionStatus> =
            withPending(NiumiPermission.APP_BLOCKING, ReadinessCheckId.ACCESSIBILITY_SERVICE).map {
                if (it.permission == NiumiPermission.APP_BLOCKING) {
                    it.copy(
                        pending =
                            failed(
                                ReadinessCheckId.ACCESSIBILITY_SERVICE,
                            ).copy(outcome = ReadinessOutcome.NOT_APPLICABLE),
                    )
                } else {
                    it
                }
            }

        fun failed(id: ReadinessCheckId) =
            ReadinessCheck(
                id = id,
                severity = ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                outcome = ReadinessOutcome.FAILED,
                action =
                    when (id) {
                        ReadinessCheckId.EXACT_ALARM -> ReadinessAction.ShowExactAlarmDiagnostic
                        ReadinessCheckId.NOTIFICATIONS -> ReadinessAction.RequestNotificationPermission
                        ReadinessCheckId.ALARM_CHANNEL -> ReadinessAction.OpenChannelSettings(CHANNEL)
                        ReadinessCheckId.ACCESSIBILITY_SERVICE -> ReadinessAction.OpenAccessibilitySettings
                        ReadinessCheckId.BATTERY_OPTIMIZATION -> ReadinessAction.OpenBatterySettings
                        ReadinessCheckId.RECENTS_LOCK -> ReadinessAction.LockInRecents
                        else -> ReadinessAction.Unsupported
                    },
            )
    }
}
