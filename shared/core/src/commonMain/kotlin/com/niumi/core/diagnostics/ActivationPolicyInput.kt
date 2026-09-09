package com.niumi.core.diagnostics

/**
 * Résultat d'un contrôle de préparation natif, converti vers le DTO commun avant l'appel à
 * [ActivationPolicy] (SPEC_ANDROID §13). [id] identifie le contrôle (ex. `"nfc_enabled"`) pour que
 * le natif puisse retrouver quelle action proposer en cas de refus.
 */
public data class ReadinessCheckInput(
    val id: String,
    val severity: ReadinessSeverity,
    val passed: Boolean,
)

/**
 * Entrée de la politique d'activation (SPEC_CORE_KMP §7.4, SPEC_ANDROID §13). [checks] porte les
 * contrôles de préparation déjà évalués par le natif ; [appSelectionCount], [triggerAtEpochMillis]
 * et [hasPairedBox] portent les règles communes indépendantes de la plateforme.
 */
public data class ActivationPolicyInput(
    val checks: List<ReadinessCheckInput>,
    val appSelectionCount: Int,
    val triggerAtEpochMillis: Long,
    val nowEpochMillis: Long,
    val hasPairedBox: Boolean,
)
