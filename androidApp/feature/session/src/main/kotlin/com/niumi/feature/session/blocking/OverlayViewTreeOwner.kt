package com.niumi.feature.session.blocking

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Propriétaires de cycle de vie d'un `ComposeView` posé hors de toute activité (point de vigilance 5
 * du plan de refonte) : une fenêtre ajoutée par un service n'a ni `LifecycleOwner`, ni
 * `SavedStateRegistryOwner`, ni `ViewModelStoreOwner`, et Compose refuse de composer sans les deux
 * premiers. Un propriétaire par affichage : [start] à l'ajout de la fenêtre, [destroy] à son
 * retrait ; il n'est jamais réutilisé.
 */
internal class OverlayViewTreeOwner :
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val viewModelStore = ViewModelStore()

    fun start() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /**
     * Idempotent ; sans [start] préalable, seul le `ViewModelStore` est vidé : le passage direct de
     * `INITIALIZED` à `DESTROYED` est interdit par `LifecycleRegistry`.
     */
    fun destroy() {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        viewModelStore.clear()
    }
}
