package com.niumi.system.readiness

import com.niumi.core.interop.ActivationPolicyInputDto
import com.niumi.core.interop.ActivationReasonDto
import com.niumi.core.interop.ReadinessCheckInputDto

/**
 * Les trois contrôles que la politique commune traite déjà par des champs dédiés. Les copier
 * aussi dans `checks` ferait remonter deux refus pour une seule cause : un `NO_PAIRED_BOX`
 * précis et un `READINESS_BLOCKING_FOR_NIUMI_EXPERIENCE` générique (SPEC_CORE_KMP §7.4, §10).
 */
private val JOURNEY_CHECK_IDS =
    setOf(
        ReadinessCheckId.PAIRED_BOX,
        ReadinessCheckId.APP_SELECTION,
        ReadinessCheckId.FUTURE_TRIGGER,
    )

/**
 * Convertit le diagnostic Android en entrée de `NiumiCoreFacade.evaluateActivation`
 * (SPEC_ANDROID §13). Un contrôle [ReadinessOutcome.NOT_APPLICABLE] est exclu : il ne peut ni
 * bloquer ni rassurer.
 *
 * Quand aucune heure de réveil n'a encore été choisie, `triggerAtEpochMillis` reçoit l'instant
 * courant, que la politique refuse avec `TRIGGER_NOT_IN_FUTURE` — le verdict exact à ce stade
 * du parcours, une session ne pouvant pas être armée sans heure.
 */
fun ReadinessReport.toActivationPolicyInput(): ActivationPolicyInputDto =
    ActivationPolicyInputDto(
        checks =
            checks
                .filter { it.id !in JOURNEY_CHECK_IDS && it.outcome != ReadinessOutcome.NOT_APPLICABLE }
                .map { ReadinessCheckInputDto(it.id.name, it.severity, it.outcome == ReadinessOutcome.PASSED) },
        appSelectionCount = appSelectionCount,
        triggerAtEpochMillis = candidateTriggerAtEpochMillis ?: nowEpochMillis,
        nowEpochMillis = nowEpochMillis,
        hasPairedBox = hasPairedBox,
    )

/**
 * Retrouve le contrôle à l'origine d'un refus. `null` pour les causes communes
 * (`NO_PAIRED_BOX`, `INVALID_APP_SELECTION`, `TRIGGER_NOT_IN_FUTURE`), que la politique rapporte
 * sans `checkId`.
 */
fun readinessCheckIdOf(reason: ActivationReasonDto): ReadinessCheckId? =
    reason.checkId?.let { id -> ReadinessCheckId.entries.firstOrNull { it.name == id } }
