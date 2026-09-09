package com.niumi.core.schedule

import com.niumi.core.domain.WakeSchedule

/** Issue du calcul de l'heure de réveil (SPEC_CORE_KMP §8.1). */
public enum class WakeScheduleStatus {
    VALID,
    INVALID_TIME,
    UNKNOWN_ZONE,
}

/**
 * Résultat typé, jamais une exception (SPEC_CORE_KMP §14) : [schedule] est non nul seulement si
 * [status] vaut `VALID`. Convention reprise de `BoxPayloadResult`/`BoxVerificationResult` (étape 2)
 * plutôt qu'un `sealed interface Success/Failure` : décision validée avec l'utilisateur le
 * 2026-09-08, voir `ETAPE-08.md`.
 */
public data class WakeScheduleResult(
    val status: WakeScheduleStatus,
    val schedule: WakeSchedule?,
)
