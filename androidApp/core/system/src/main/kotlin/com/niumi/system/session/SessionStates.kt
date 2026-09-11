package com.niumi.system.session

import com.niumi.core.interop.SessionStateDto

/**
 * États finaux de la machine commune. `FINAL_STATES` existe déjà dans `:shared:core`
 * (`ReducerSupport.kt`) mais est `internal` à `commonMain`, inaccessible depuis Android — redéfini
 * ici pour les mêmes trois valeurs (SPEC_CORE_KMP §5.1).
 *
 * Public depuis l'étape 13 : trois modules avaient besoin de la même règle (le coordinateur ici,
 * la destination de l'accueil dans `:app`, la garde du parcours de préparation dans
 * `:feature:setup`). Une seule définition, prouvée exhaustive sur `SessionStateDto.entries` par
 * `SessionStatesTest`, vaut mieux que trois copies à maintenir ensemble.
 */
val SESSION_FINAL_STATES: Set<SessionStateDto> =
    setOf(SessionStateDto.COMPLETED, SessionStateDto.CANCELLED, SessionStateDto.FAILED)

/**
 * Une session dans un état non final tient encore l'appareil : alarme programmée ou blocage
 * appliqué. `null` (aucune session) n'est pas un état en cours.
 */
fun SessionStateDto?.isSessionInProgress(): Boolean = this != null && this !in SESSION_FINAL_STATES
