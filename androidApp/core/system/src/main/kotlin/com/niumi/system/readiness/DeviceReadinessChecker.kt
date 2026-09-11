package com.niumi.system.readiness

/**
 * Diagnostic avant activation (SPEC_ANDROID §13). Conserve la détection et les actions Android ;
 * la décision d'autoriser ou non l'activation revient exclusivement à
 * `NiumiCoreFacade.evaluateActivation`, nourri par [ReadinessDtoMapper].
 *
 * §13.1 impose de rejouer ce diagnostic pendant une session armée : le contrôle est
 * délibérément sans état, il relit ses sources à chaque appel.
 */
fun interface DeviceReadinessChecker {
    suspend fun check(input: ReadinessInput): ReadinessReport
}

/**
 * [candidateTriggerAtEpochMillis] est `null` tant qu'aucune heure de réveil n'a été choisie
 * (l'écran de diagnostic précède le choix de l'heure dans le parcours) : le contrôle
 * [ReadinessCheckId.FUTURE_TRIGGER] est alors [ReadinessOutcome.NOT_APPLICABLE]. L'étape 14 le
 * renseigne depuis `computeWakeSchedule`.
 */
data class ReadinessInput(
    val candidateTriggerAtEpochMillis: Long? = null,
)

/**
 * [checks] contient les quatorze contrôles de §13, dans l'ordre du tableau. Les trois contrôles
 * de parcours (boîtier, sélection, instant de réveil) y figurent pour l'affichage, mais
 * [ReadinessDtoMapper] les route vers les champs dédiés d'`ActivationPolicyInputDto` plutôt que
 * vers sa liste `checks` : la politique commune les traite déjà avec des codes précis
 * (`NO_PAIRED_BOX`, `INVALID_APP_SELECTION`, `TRIGGER_NOT_IN_FUTURE`), les verser deux fois
 * produirait un double refus pour une même cause.
 */
data class ReadinessReport(
    val checks: List<ReadinessCheck>,
    val appSelectionCount: Int,
    val hasPairedBox: Boolean,
    val candidateTriggerAtEpochMillis: Long?,
    val nowEpochMillis: Long,
) {
    fun check(id: ReadinessCheckId): ReadinessCheck = checks.first { it.id == id }

    /** Premier contrôle en échec dans l'ordre de §13, celui que l'écran doit traiter d'abord. */
    fun firstFailure(): ReadinessCheck? = checks.firstOrNull { it.outcome == ReadinessOutcome.FAILED }
}
