package com.niumi.app.navigation

import com.niumi.system.intent.NiumiDeepLink

/**
 * Traduit l'extra de destination d'un `Intent` en route typée (SPEC_ANDROID §13.1 : « Le tap de la
 * notification ouvre `MainActivity`, qui redirige vers le diagnostic d'incident »).
 *
 * Étape 17 : `AlarmActivity` emprunte le même chemin pour la fin de session. Elle vit dans sa
 * propre tâche, hors du `NavHost`, et ne peut donc pas naviguer elle-même (§15). Depuis l'étape
 * 46, seule une session terminée a sa destination, l'écran 11 de cette session ([sessionId]) ;
 * une annulation ramène à l'accueil.
 *
 * Fonction pure, hors de `MainActivity` pour être testable en JVM — un `Intent` lève `Stub!` en
 * test unitaire, une `String?` non.
 *
 * Une valeur inconnue rend `null` plutôt que de lever : un extra venant d'un `PendingIntent` créé
 * par une version antérieure de l'application doit ouvrir l'accueil, pas faire planter le
 * démarrage.
 */
fun deepLinkDestinationFor(
    extra: String?,
    sessionId: String? = null,
): NiumiRoute? =
    when (extra) {
        NiumiDeepLink.DESTINATION_INCIDENT_DIAGNOSTIC -> NiumiRoute.IncidentDiagnostic
        NiumiDeepLink.DESTINATION_SESSION_COMPLETED -> sessionId?.let(NiumiRoute::Success)
        else -> null
    }

/**
 * L'`Intent` de lancement n'est lu qu'à un démarrage neuf (étape 46). Une activité recréée,
 * après un changement de configuration ([restoredState]) ou une mort du processus suivie d'une
 * réouverture depuis les récents ([launchedFromHistory]), restaure sa pile de navigation : relire
 * l'extra rouvrirait l'écran 11 d'un réveil déjà vu, des heures plus tard.
 */
fun readsLaunchDeepLink(
    restoredState: Boolean,
    launchedFromHistory: Boolean,
): Boolean = !restoredState && !launchedFromHistory
