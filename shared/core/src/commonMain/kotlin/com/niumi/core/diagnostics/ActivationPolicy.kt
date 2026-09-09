package com.niumi.core.diagnostics

import com.niumi.core.domain.AppSelectionSummary

/**
 * Décide si une activation est permise à partir des contrôles de préparation natifs et des règles
 * communes (SPEC_CORE_KMP §7.4, SPEC_ANDROID §13). Refuse si un contrôle `BLOCKING_*` échoue, si
 * `appSelectionCount` est hors de 1..50, si `triggerAtEpochMillis` n'est pas strictement futur, ou
 * si aucun boîtier n'est associé. Un `WARNING` seul n'empêche jamais l'activation.
 */
public object ActivationPolicy {
    public fun evaluate(input: ActivationPolicyInput): ActivationPolicyResult {
        val blocking = readinessBlockingReasons(input.checks) + commonBlockingReasons(input)
        val warnings = readinessWarnings(input.checks)
        return ActivationPolicyResult(
            allowed = blocking.isEmpty(),
            blockingReasons = blocking,
            warnings = warnings,
        )
    }

    private fun readinessBlockingReasons(checks: List<ReadinessCheckInput>): List<ActivationReason> {
        val blockingChecks = checks.filter { !it.passed && it.severity != ReadinessSeverity.WARNING }
        return blockingChecks.map { check -> ActivationReason(blockingCodeFor(check.severity), check.id) }
    }

    private fun blockingCodeFor(severity: ReadinessSeverity): String =
        when (severity) {
            ReadinessSeverity.BLOCKING_FOR_ALARM -> {
                ActivationReasonCode.READINESS_BLOCKING_FOR_ALARM
            }

            ReadinessSeverity.BLOCKING_FOR_NIUMI_EXPERIENCE -> {
                ActivationReasonCode.READINESS_BLOCKING_FOR_NIUMI_EXPERIENCE
            }

            ReadinessSeverity.WARNING -> {
                error("filtré ci-dessus : un contrôle WARNING n'est jamais bloquant")
            }
        }

    private fun readinessWarnings(checks: List<ReadinessCheckInput>): List<ActivationReason> {
        val warningChecks = checks.filter { !it.passed && it.severity == ReadinessSeverity.WARNING }
        return warningChecks.map { check -> ActivationReason(ActivationReasonCode.READINESS_WARNING, check.id) }
    }

    private fun commonBlockingReasons(input: ActivationPolicyInput): List<ActivationReason> {
        val reasons = mutableListOf<ActivationReason>()
        val count = input.appSelectionCount
        if (count < AppSelectionSummary.MIN_COUNT || count > AppSelectionSummary.MAX_COUNT) {
            reasons += ActivationReason(ActivationReasonCode.INVALID_APP_SELECTION, checkId = null)
        }
        if (input.triggerAtEpochMillis <= input.nowEpochMillis) {
            reasons += ActivationReason(ActivationReasonCode.TRIGGER_NOT_IN_FUTURE, checkId = null)
        }
        if (!input.hasPairedBox) {
            reasons += ActivationReason(ActivationReasonCode.NO_PAIRED_BOX, checkId = null)
        }
        return reasons
    }
}
