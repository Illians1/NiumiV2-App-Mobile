package com.niumi.core.diagnostics

/**
 * Sévérité d'un contrôle de préparation natif (SPEC_CORE_KMP §7.3, SPEC_ANDROID §13). Copié mot
 * pour mot de la spec commune : `DeviceReadinessChecker` (Android) convertit chaque résultat natif
 * vers ces valeurs avant que [ActivationPolicy] décide si l'activation est permise.
 */
public enum class ReadinessSeverity {
    BLOCKING_FOR_ALARM,
    BLOCKING_FOR_NIUMI_EXPERIENCE,
    WARNING,
}
