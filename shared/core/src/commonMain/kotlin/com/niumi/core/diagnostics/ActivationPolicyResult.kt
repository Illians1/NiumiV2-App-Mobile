package com.niumi.core.diagnostics

/**
 * Raison codée d'un refus ou d'un avertissement (SPEC_CORE_KMP §7.3 : « le module commun ne
 * contient pas les textes affichés à l'utilisateur »). [checkId] identifie le contrôle de
 * préparation en cause, `null` pour une règle commune indépendante de tout contrôle natif.
 */
public data class ActivationReason(
    val code: String,
    val checkId: String?,
)

/**
 * Codes de [ActivationReason] (décision d'étape 8, non fixés par la spec). `READINESS_*` reprend
 * la sévérité du contrôle natif en cause ; les trois derniers portent une règle commune
 * indépendante de tout contrôle de préparation.
 */
public object ActivationReasonCode {
    public const val READINESS_BLOCKING_FOR_ALARM: String = "READINESS_BLOCKING_FOR_ALARM"
    public const val READINESS_BLOCKING_FOR_NIUMI_EXPERIENCE: String = "READINESS_BLOCKING_FOR_NIUMI_EXPERIENCE"
    public const val READINESS_WARNING: String = "READINESS_WARNING"
    public const val INVALID_APP_SELECTION: String = "INVALID_APP_SELECTION"
    public const val TRIGGER_NOT_IN_FUTURE: String = "TRIGGER_NOT_IN_FUTURE"
    public const val NO_PAIRED_BOX: String = "NO_PAIRED_BOX"
}

/**
 * Sortie de la politique d'activation (SPEC_CORE_KMP §14). [allowed] est faux dès que
 * [blockingReasons] est non vide ; [warnings] n'empêche jamais l'activation (SPEC_ANDROID §13 :
 * « l'activation reste possible, avec une information claire »).
 */
public data class ActivationPolicyResult(
    val allowed: Boolean,
    val blockingReasons: List<ActivationReason>,
    val warnings: List<ActivationReason>,
)
