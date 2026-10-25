package com.niumi.core.schedule

import com.niumi.core.domain.BlockingSchedule
import com.niumi.core.domain.WakeSchedule

/**
 * Issue du calcul de la prochaine occurrence (SPEC_CORE_KMP §8.4). [EMPTY_PERIOD] : le changement
 * d'heure de printemps confond le coucher et le réveil, la période serait vide. Depuis le
 * 2026-10-25, aucun jour coché n'est plus un refus (réveil ponctuel) et deux heures identiques
 * forment une période de 24 h.
 */
public enum class NextOccurrenceStatus {
    VALID,
    INVALID_TIME,
    UNKNOWN_ZONE,
    EMPTY_PERIOD,
}

/**
 * Prochaine session à armer : [wakeSchedule] et [blockingSchedule] sont prêts pour un
 * `ActivationRequest`. [blockingStartsImmediately] vaut vrai quand le coucher est déjà atteint : la
 * période est en cours et [blockingSchedule] vaut alors [BlockingSchedule.IMMEDIATE].
 */
public data class NextOccurrence(
    val wakeSchedule: WakeSchedule,
    val blockingSchedule: BlockingSchedule,
    val blockingStartsImmediately: Boolean,
)

/**
 * Résultat typé, jamais une exception (SPEC_CORE_KMP §14) : [occurrence] est non nul seulement si
 * [status] vaut `VALID`. Même convention que [WakeScheduleResult].
 */
public data class NextOccurrenceResult(
    val status: NextOccurrenceStatus,
    val occurrence: NextOccurrence?,
)
