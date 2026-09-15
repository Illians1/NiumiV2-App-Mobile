package com.niumi.system.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dernière raison connue pour laquelle la persistance est illisible (SPEC_ANDROID §18, §20 ; étape
 * 20), ou `null` si elle est lisible. Alimentée par [SessionReconciler] — seule classe qui voit
 * `LoadResult.Unreadable` en production — sur chacun de ses deux chemins de `gateway.load()`, et
 * lue par l'accueil (`:app`) pour rediriger vers l'écran de diagnostic sans jamais retirer le
 * blocage, et par l'écran de diagnostic lui-même pour afficher la limite plutôt que rester en
 * chargement indéfini.
 *
 * `@Inject constructor()` direct plutôt qu'un `@Provides` : `SessionModule` est au plafond detekt
 * `TooManyFunctions` (11), et cette classe n'a besoin d'aucune dépendance à construire.
 */
@Singleton
class StorageIntegrityState
    @Inject
    constructor() {
        private val state = MutableStateFlow<String?>(null)
        val failure: StateFlow<String?> = state

        fun reportUnreadable(reason: String) {
            state.value = reason
        }

        fun reportReadable() {
            state.value = null
        }
    }
