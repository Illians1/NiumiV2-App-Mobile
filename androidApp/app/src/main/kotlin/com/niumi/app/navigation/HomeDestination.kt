package com.niumi.app.navigation

import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.isSessionInProgress

/**
 * Destination de l'ouverture de Niumi (étape 45 ; `design-plan/SPECIFICATION.md` §3) : l'accueil dès
 * que la mise en route est finie ([setupCompleted]), sinon la mise en route là où elle en est.
 *
 * Une session non finale mène **toujours** à l'accueil, mise en route finie ou non : c'est de là que
 * l'on annule le réveil, et SPEC_ANDROID §10.4 fait de l'accès depuis le lanceur la seconde garantie
 * d'accès au scan, indépendante de la notification. Pendant la sonnerie, `MainActivity` passe
 * l'écran de réveil devant, quelle que soit la destination ([requiresAlarmScreen]).
 *
 * [storageUnreadable] prend le pas sur tout le reste (étape 20) : si Niumi ne peut plus lire son
 * état enregistré, ni l'état d'une session en cours ni la mise en route ne sont fiables — l'écran de
 * diagnostic est la seule destination honnête, et le blocage n'est jamais retiré par ce chemin (§18).
 *
 * Fonction pure : elle prend l'état seul et non le snapshot entier, pour être prouvée sans fixture.
 */
fun homeDestinationFor(
    state: SessionStateDto?,
    onboardingAcknowledged: Boolean,
    setupCompleted: Boolean,
    storageUnreadable: Boolean = false,
): NiumiRoute =
    when {
        storageUnreadable -> NiumiRoute.IncidentDiagnostic
        state.isSessionInProgress() || setupCompleted -> NiumiRoute.Home
        onboardingAcknowledged -> NiumiRoute.Permissions
        else -> NiumiRoute.Presentation
    }
