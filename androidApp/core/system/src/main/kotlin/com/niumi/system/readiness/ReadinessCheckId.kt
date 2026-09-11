package com.niumi.system.readiness

/**
 * Les quatorze contrôles du tableau de SPEC_ANDROID §13, **dans l'ordre du tableau** : l'écran de
 * diagnostic présente le premier blocage d'abord, cet ordre est donc porteur de sens et ne doit
 * pas être réarrangé. Le mode Ne pas déranger en compte deux depuis la mesure de l'étape 6
 * (silence total bloquant, autres modes en avertissement).
 */
enum class ReadinessCheckId {
    NFC_PRESENT,
    NFC_ENABLED,
    PAIRED_BOX,
    APP_SELECTION,
    EXACT_ALARM,
    FULL_SCREEN_INTENT,
    NOTIFICATIONS,
    ALARM_CHANNEL,
    ALARM_VOLUME,
    DND_TOTAL_SILENCE,
    DND_OTHER_MODE,
    ACCESSIBILITY_SERVICE,
    FUTURE_TRIGGER,
    BATTERY_OPTIMIZATION,
}
