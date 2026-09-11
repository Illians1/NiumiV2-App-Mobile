package com.niumi.system.readiness

import android.app.NotificationManager
import com.google.common.truth.Truth.assertThat
import com.niumi.core.diagnostics.ActivationReasonCode
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.readiness.fakes.ReadinessTestSources
import com.niumi.system.session.fakes.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val NOW = 1_757_000_000_000L
private const val ANDROID_16 = 36

/**
 * Conversion du diagnostic Android vers `ActivationPolicyInputDto` (SPEC_ANDROID §13,
 * SPEC_CORE_KMP §7.4, §10). Aucune règle d'activation n'est réimplémentée ici : chaque test
 * passe par la vraie `NiumiCoreFacade`, seule autorité.
 */
class ReadinessDtoMapperTest {
    private val facade = NiumiCoreFacade()

    private suspend fun report(
        sources: ReadinessTestSources,
        candidateTriggerAtEpochMillis: Long? = NOW + 3_600_000L,
    ) = AndroidDeviceReadinessChecker(sources.build(), FakeClock(NOW), ANDROID_16)
        .check(ReadinessInput(candidateTriggerAtEpochMillis))

    @Test
    fun theThreeJourneyChecksAreNeverCopiedIntoTheChecksList() =
        runTest {
            val input = report(ReadinessTestSources()).toActivationPolicyInput()

            assertThat(input.checks.map { it.id })
                .containsNoneOf(
                    ReadinessCheckId.PAIRED_BOX.name,
                    ReadinessCheckId.APP_SELECTION.name,
                    ReadinessCheckId.FUTURE_TRIGGER.name,
                )
        }

    @Test
    fun checksWithoutObjectOnThisDeviceAreExcludedFromThePolicyInput() =
        runTest {
            val sources = ReadinessTestSources()
            sources.nfcReader.availabilityValue = NfcAvailability.ABSENT

            val input = report(sources).toActivationPolicyInput()

            // NFC_ENABLED est NOT_APPLICABLE sans matériel : l'inclure ferait porter deux refus
            // à une seule cause, déjà signalée par NFC_PRESENT.
            assertThat(input.checks.map { it.id }).doesNotContain(ReadinessCheckId.NFC_ENABLED.name)
            assertThat(input.checks.map { it.id }).contains(ReadinessCheckId.NFC_PRESENT.name)
        }

    @Test
    fun anAllGreenReportWithAFutureTriggerAllowsActivation() =
        runTest {
            val result = facade.evaluateActivation(report(ReadinessTestSources()).toActivationPolicyInput())

            assertThat(result.allowed).isTrue()
            assertThat(result.blockingReasons).isEmpty()
            assertThat(result.warnings).isEmpty()
        }

    @Test
    fun aDoNotDisturbWarningAloneStillAllowsActivation() =
        runTest {
            val sources = ReadinessTestSources()
            sources.interruptionFilterSource.filter = NotificationManager.INTERRUPTION_FILTER_ALARMS

            val result = facade.evaluateActivation(report(sources).toActivationPolicyInput())

            assertThat(result.allowed).isTrue()
            assertThat(result.warnings.map { it.checkId }).containsExactly(ReadinessCheckId.DND_OTHER_MODE.name)
        }

    @Test
    fun aBlockingCheckRefusesActivationAndNamesTheCheckThatCausedIt() =
        runTest {
            val sources = ReadinessTestSources()
            sources.alarmVolumeSource.volume = 0

            val result = facade.evaluateActivation(report(sources).toActivationPolicyInput())

            assertThat(result.allowed).isFalse()
            assertThat(result.blockingReasons).hasSize(1)
            assertThat(result.blockingReasons.first().code)
                .isEqualTo(ActivationReasonCode.READINESS_BLOCKING_FOR_ALARM)
            assertThat(result.blockingReasons.first().checkId).isEqualTo(ReadinessCheckId.ALARM_VOLUME.name)
        }

    @Test
    fun theJourneyCausesAreReportedOnceWithTheirOwnCodesRatherThanAsReadinessFailures() =
        runTest {
            val sources = ReadinessTestSources()
            sources.pairedBoxStore.clear()
            sources.appSelectionSource.count = 0

            val result = facade.evaluateActivation(report(sources).toActivationPolicyInput())

            assertThat(result.allowed).isFalse()
            assertThat(result.blockingReasons.map { it.code })
                .containsExactly(ActivationReasonCode.INVALID_APP_SELECTION, ActivationReasonCode.NO_PAIRED_BOX)
            assertThat(result.blockingReasons.map { it.checkId }).containsExactly(null, null)
        }

    @Test
    fun aDiagnosticRunBeforeAnyWakeTimeHasBeenChosenIsRefusedForTheTriggerInstant() =
        runTest {
            val input = report(ReadinessTestSources(), candidateTriggerAtEpochMillis = null).toActivationPolicyInput()

            val result = facade.evaluateActivation(input)

            assertThat(result.allowed).isFalse()
            assertThat(result.blockingReasons.map { it.code })
                .containsExactly(ActivationReasonCode.TRIGGER_NOT_IN_FUTURE)
        }

    @Test
    fun theCheckIdOfABlockingReasonCanBeTracedBackToItsControl() =
        runTest {
            val sources = ReadinessTestSources()
            sources.accessibilityServiceStatus.enabled = false

            val result = facade.evaluateActivation(report(sources).toActivationPolicyInput())

            assertThat(readinessCheckIdOf(result.blockingReasons.first()))
                .isEqualTo(ReadinessCheckId.ACCESSIBILITY_SERVICE)
        }
}
