package com.niumi.feature.setup.readiness.fakes

import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheck
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome
import com.niumi.system.readiness.ReadinessReport
import com.niumi.system.setup.SetupPreferences

/**
 * Sévérités et actions du tableau de SPEC_ANDROID §13, réécrites ici indépendamment de
 * `AndroidDeviceReadinessChecker` : si les deux divergent, c'est la spec qui tranche, et le test
 * du ViewModel ne doit pas hériter d'une erreur du contrôleur.
 */
fun severityOf(id: ReadinessCheckId): ReadinessSeverityDto =
    when (id) {
        ReadinessCheckId.EXACT_ALARM,
        ReadinessCheckId.ALARM_VOLUME,
        ReadinessCheckId.DND_TOTAL_SILENCE,
        ReadinessCheckId.FUTURE_TRIGGER,
        -> ReadinessSeverityDto.BLOCKING_FOR_ALARM

        ReadinessCheckId.DND_OTHER_MODE -> ReadinessSeverityDto.WARNING

        else -> ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE
    }

fun actionOf(id: ReadinessCheckId): ReadinessAction =
    when (id) {
        ReadinessCheckId.NFC_PRESENT -> ReadinessAction.Unsupported
        ReadinessCheckId.NFC_ENABLED -> ReadinessAction.OpenNfcSettings
        ReadinessCheckId.PAIRED_BOX -> ReadinessAction.StartPairing
        ReadinessCheckId.APP_SELECTION -> ReadinessAction.OpenAppPicker
        ReadinessCheckId.EXACT_ALARM -> ReadinessAction.ShowExactAlarmDiagnostic
        ReadinessCheckId.FULL_SCREEN_INTENT -> ReadinessAction.OpenFullScreenIntentSettings
        ReadinessCheckId.NOTIFICATIONS -> ReadinessAction.RequestNotificationPermission
        ReadinessCheckId.ALARM_CHANNEL -> ReadinessAction.OpenChannelSettings("niumi_alarm_ringing")
        ReadinessCheckId.ALARM_VOLUME -> ReadinessAction.OpenSoundSettings
        ReadinessCheckId.DND_TOTAL_SILENCE, ReadinessCheckId.DND_OTHER_MODE -> ReadinessAction.OpenDndSettings
        ReadinessCheckId.ACCESSIBILITY_SERVICE -> ReadinessAction.OpenAccessibilitySettings
        ReadinessCheckId.FUTURE_TRIGGER -> ReadinessAction.FixTime
        ReadinessCheckId.BATTERY_OPTIMIZATION -> ReadinessAction.OpenBatterySettings(aospExemptionGranted = false)
    }

const val NOW_EPOCH_MILLIS = 1_800_000_000_000L
private const val ONE_HOUR_MILLIS = 3_600_000L

/**
 * Rapport dont tous les contrôles passent, sauf ceux nommés dans [failing] et ceux de
 * [notApplicable]. Les contrôles sont produits dans l'ordre de `ReadinessCheckId.entries`, qui est
 * celui du tableau de §13 — l'ordre porte le sens, le premier échec étant l'action principale.
 */
fun reportWith(
    failing: Set<ReadinessCheckId> = emptySet(),
    notApplicable: Set<ReadinessCheckId> = emptySet(),
    appSelectionCount: Int = 1,
    hasPairedBox: Boolean = true,
    candidateTriggerAtEpochMillis: Long? = NOW_EPOCH_MILLIS + ONE_HOUR_MILLIS,
): ReadinessReport =
    ReadinessReport(
        checks =
            ReadinessCheckId.entries.map { id ->
                ReadinessCheck(
                    id = id,
                    severity = severityOf(id),
                    outcome =
                        when (id) {
                            in notApplicable -> ReadinessOutcome.NOT_APPLICABLE
                            in failing -> ReadinessOutcome.FAILED
                            else -> ReadinessOutcome.PASSED
                        },
                    action = actionOf(id),
                )
            },
        appSelectionCount = appSelectionCount,
        hasPairedBox = hasPairedBox,
        candidateTriggerAtEpochMillis = candidateTriggerAtEpochMillis,
        nowEpochMillis = NOW_EPOCH_MILLIS,
    )

class FakeSetupPreferences(
    private var onboardingAcknowledged: Boolean = true,
    var batteryExemptionConfirmed: Boolean = false,
    private var lastWakeTimeIsoValue: String? = null,
) : SetupPreferences {
    var batteryWrites = 0
        private set

    override suspend fun isOnboardingAcknowledged(): Boolean = onboardingAcknowledged

    override suspend fun acknowledgeOnboarding() {
        onboardingAcknowledged = true
    }

    override suspend fun isBatteryExemptionConfirmed(): Boolean = batteryExemptionConfirmed

    override suspend fun setBatteryExemptionConfirmed(confirmed: Boolean) {
        batteryExemptionConfirmed = confirmed
        batteryWrites++
    }

    override suspend fun lastWakeTimeIso(): String? = lastWakeTimeIsoValue

    override suspend fun setLastWakeTimeIso(value: String) {
        lastWakeTimeIsoValue = value
    }
}
