package com.niumi.app.navigation

import com.niumi.system.intent.NiumiDeepLink

/**
 * Traduit l'extra de destination d'un `Intent` en route typée (SPEC_ANDROID §13.1 : « Le tap de la
 * notification ouvre `MainActivity`, qui redirige vers le diagnostic d'incident »).
 *
 * Fonction pure, hors de `MainActivity` pour être testable en JVM — un `Intent` lève `Stub!` en
 * test unitaire, une `String?` non.
 *
 * Une valeur inconnue rend `null` plutôt que de lever : un extra venant d'un `PendingIntent` créé
 * par une version antérieure de l'application doit ouvrir l'accueil, pas faire planter le
 * démarrage.
 */
fun deepLinkDestinationFor(extra: String?): NiumiRoute? =
    when (extra) {
        NiumiDeepLink.DESTINATION_INCIDENT_DIAGNOSTIC -> NiumiRoute.IncidentDiagnostic
        else -> null
    }
