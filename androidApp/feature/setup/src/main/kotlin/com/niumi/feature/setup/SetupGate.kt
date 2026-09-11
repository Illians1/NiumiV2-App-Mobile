package com.niumi.feature.setup

import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.isSessionInProgress

/**
 * Garde du parcours de préparation (SPEC_CORE_KMP §2 point 11 et §10, SPEC_ANDROID §11.1 et
 * §12.1) : l'association du boîtier et le sélecteur d'applications sont fermés tant qu'une
 * session est en cours. Sans cette garde, une ré-association remplacerait le boîtier attendu par
 * la session active et rendrait sa fin impossible ; une modification de la sélection
 * désynchroniserait le blocage appliqué de ce que l'écran affiche.
 *
 * Fonction pure plutôt que méthode d'un ViewModel, pour être prouvée sur tous les états sans
 * fixture. `SetupNavigation.kt` prévu par le plan MVP n'existe pas — un module `feature` ne peut
 * pas dépendre de `:app` (§6) : la garde est appliquée par les `Route` composables, qui
 * remontent le refus par une lambda de navigation.
 */
fun isSetupEditable(state: SessionStateDto?): Boolean = !state.isSessionInProgress()
