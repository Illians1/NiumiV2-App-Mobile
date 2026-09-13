package com.niumi.feature.setup.readiness

import com.niumi.system.readiness.ReadinessAction

/**
 * Texte affiché à la place d'un réglage quand il n'existe aucun recours système (SPEC_ANDROID
 * §13).
 *
 * La traduction d'une [ReadinessAction] en `Intent` vit désormais dans `:core:system`
 * (`com.niumi.system.readiness.settingsIntentFor`) : l'écran 7 en a besoin depuis l'étape 16 et
 * `:feature:session` ne peut pas dépendre de `:feature:setup` (§6). Les textes, eux, restent ici —
 * `:core:system` reste sans interface.
 */
fun explanationFor(action: ReadinessAction): String? =
    when (action) {
        ReadinessAction.ShowExactAlarmDiagnostic -> ReadinessMessages.EXACT_ALARM_DIAGNOSTIC
        ReadinessAction.Unsupported -> ReadinessMessages.UNSUPPORTED
        else -> null
    }
