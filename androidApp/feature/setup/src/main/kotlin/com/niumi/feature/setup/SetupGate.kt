package com.niumi.feature.setup

import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.isSessionInProgress

/**
 * Garde du parcours de préparation (SPEC_CORE_KMP §4 et §10, SPEC_ANDROID §12.1) : le sélecteur
 * d'applications est fermé tant qu'une session est en cours — une modification de la sélection
 * désynchroniserait le blocage appliqué de ce que l'écran affiche. Depuis l'étape 43, l'association
 * des Niumi Spots n'est plus fermée (SPEC_CORE_KMP §2, décisions 11 et 16) : l'écran 3 ne lit cette
 * garde que pour masquer « Continuer ».
 *
 * Fonction pure plutôt que méthode d'un ViewModel, pour être prouvée sur tous les états sans
 * fixture. `SetupNavigation.kt` prévu par le plan MVP n'existe pas — un module `feature` ne peut
 * pas dépendre de `:app` (§6) : la garde est appliquée par les `Route` composables, qui
 * remontent le refus par une lambda de navigation.
 */
fun isSetupEditable(state: SessionStateDto?): Boolean = !state.isSessionInProgress()
