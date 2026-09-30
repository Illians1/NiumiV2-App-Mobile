package com.niumi.system.readiness

/**
 * Action proposée à l'utilisateur pour lever un contrôle (colonne « Action proposée » de
 * SPEC_ANDROID §13). Description pure : `:core:system` ne construit aucun `Intent` de réglages,
 * l'écran de diagnostic traduit ces cas.
 *
 * Aucune action ne mène à « Alarmes et rappels » : Niumi déclare `USE_EXACT_ALARM` et jamais
 * `SCHEDULE_EXACT_ALARM` (§13, §14). Un `canScheduleExactAlarms()` faux est un état anormal
 * ([ShowExactAlarmDiagnostic]), pas une permission à demander.
 */
sealed interface ReadinessAction {
    data object OpenNfcSettings : ReadinessAction

    data object StartPairing : ReadinessAction

    data object OpenAppPicker : ReadinessAction

    data object ShowExactAlarmDiagnostic : ReadinessAction

    data object OpenFullScreenIntentSettings : ReadinessAction

    data object RequestNotificationPermission : ReadinessAction

    data class OpenChannelSettings(
        val channelId: String,
    ) : ReadinessAction

    data object OpenSoundSettings : ReadinessAction

    data object OpenDndSettings : ReadinessAction

    data object OpenAccessibilitySettings : ReadinessAction

    data object FixTime : ReadinessAction

    /**
     * Exemption d'énergie : ouvrir la liste système des optimisations de batterie (§13), où
     * l'utilisateur choisit « Sans restriction ». C'est cette liste blanche qui empêche le gel,
     * y compris sur HyperOS (mesuré le 2026-09-28).
     */
    data object OpenBatterySettings : ReadinessAction

    /**
     * Verrouiller Niumi dans les applications récentes (étape 25). Aucun réglage système à ouvrir :
     * le geste se fait dans le panneau des récents lui-même, et le bouton ne sert qu'à rejouer le
     * diagnostic une fois le cadenas posé.
     */
    data object LockInRecents : ReadinessAction

    /** Aucun recours : l'appareil ne possède pas le matériel requis. */
    data object Unsupported : ReadinessAction
}
