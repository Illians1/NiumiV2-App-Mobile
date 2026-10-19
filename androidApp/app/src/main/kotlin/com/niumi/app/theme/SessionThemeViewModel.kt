package com.niumi.app.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.system.session.SessionThemeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Thème de `MainActivity` d'après la session persistée (étape 33). `null` tant que la persistance
 * n'a pas répondu : l'activité ne compose rien plutôt qu'un écran clair qui virerait au sombre.
 * Survit à la rotation, comme tout `ViewModel`.
 */
@HiltViewModel
class SessionThemeViewModel
    @Inject
    constructor(
        source: SessionThemeSource,
    ) : ViewModel() {
        val dark: StateFlow<Boolean?> = source.dark.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }
