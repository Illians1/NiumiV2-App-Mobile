package com.niumi.feature.session.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.common.OperationResult

/**
 * Écran 7 dans une fenêtre du service d'accessibilité (SPEC_ANDROID §12.2, étape 39 de la refonte) :
 * un `ComposeView` plein écran, sombre, touchable, **sans minuterie** — il reste jusqu'à « Fermer »
 * ou « Ouvrir Niumi », ou jusqu'à ce que le service appelle [hide] (fin du blocage, extinction de
 * l'écran, interruption, déliaison). Remplace le bandeau non touchable de 3 s des Lots 1 à 9.
 *
 * Chaque affichage a son [OverlayViewTreeOwner], détruit au retrait : rien ne survit d'un affichage
 * à l'autre. Appelé sur le thread principal uniquement.
 */
class WindowManagerBlockOverlayController internal constructor(
    private val context: Context,
    private val host: OverlayWindowHost,
    private val onOpenNiumi: () -> Unit,
) : BlockOverlayController {
    /** [service] : le service d'accessibilité lui-même, seul porteur du jeton de fenêtre. */
    constructor(service: AccessibilityService, onOpenNiumi: () -> Unit) :
        this(service, WindowManagerOverlayWindowHost(service), onOpenNiumi)

    private class Shown(
        val view: ComposeView,
        val owner: OverlayViewTreeOwner,
    )

    private var shown: Shown? = null

    /** Nom affiché ; un état Compose, pour qu'une autre application bloquée le remplace en place. */
    private val appName = mutableStateOf<String?>(null)

    override val isShowing: Boolean
        get() = shown != null

    override fun show(appName: String?): OperationResult {
        this.appName.value = appName
        if (shown != null) return OperationResult.AlreadySatisfied
        val owner = OverlayViewTreeOwner().apply { start() }
        val view =
            ComposeView(context).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setContent {
                    NiumiTheme(dark = true) {
                        BlockScreenContent(
                            title = BlockScreenTexts.title(this@WindowManagerBlockOverlayController.appName.value),
                            onClose = ::hide,
                            onOpenNiumi = ::openNiumi,
                        )
                    }
                }
            }
        val result = host.add(view)
        if (result is OperationResult.Failure) {
            owner.destroy()
        } else {
            shown = Shown(view, owner)
        }
        return result
    }

    override fun hide() {
        val current = shown ?: return
        shown = null
        host.remove(current.view)
        current.view.disposeComposition()
        current.owner.destroy()
    }

    private fun openNiumi() {
        hide()
        onOpenNiumi()
    }
}
