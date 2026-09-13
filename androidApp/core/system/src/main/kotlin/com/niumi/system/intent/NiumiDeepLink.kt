package com.niumi.system.intent

/**
 * Destinations que `MainActivity` sait ouvrir directement à partir d'un `Intent` (SPEC_ANDROID
 * §13.1 : « Le tap de la notification ouvre `MainActivity`, qui redirige vers le diagnostic
 * d'incident »).
 *
 * La clé et les valeurs vivent dans `:core:system` parce que les deux extrémités en dépendent :
 * `AndroidSessionWarningNotifier` écrit l'extra, `:app` le relit. Une constante dupliquée de part et
 * d'autre se serait désynchronisée en silence — un extra jamais reconnu ne produit aucune erreur,
 * seulement une redirection qui n'a pas lieu.
 */
object NiumiDeepLink {
    const val EXTRA_DESTINATION = "com.niumi.extra.DESTINATION"

    /** Écran 12 (§15), livré à l'étape 16. */
    const val DESTINATION_INCIDENT_DIAGNOSTIC = "incident_diagnostic"

    /**
     * Écrans 10 et 11 (§15), atteints depuis `AlarmActivity` à l'étape 17. L'écran de réveil vit
     * dans sa propre tâche, hors du `NavHost` : il ne peut pas naviguer, seulement rouvrir
     * `MainActivity` sur la bonne destination.
     */
    const val DESTINATION_SESSION_COMPLETED = "session_completed"

    const val DESTINATION_SESSION_CANCELLED = "session_cancelled"
}
