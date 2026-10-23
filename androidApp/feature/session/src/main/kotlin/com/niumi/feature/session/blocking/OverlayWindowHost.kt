package com.niumi.feature.session.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.niumi.system.common.OperationResult

/**
 * Fenêtre qui porte l'écran 7. Couture de test : la vraie fenêtre `TYPE_ACCESSIBILITY_OVERLAY`
 * exige le jeton du service d'accessibilité relié, qu'aucun test instrumenté ne peut obtenir (toute
 * instrumentation débranche le service, SPEC_ANDROID §19.2). `BlockOverlayControllerTest` pose
 * donc la vue dans une activité de test ; la vraie fenêtre est vérifiée sur appareil par
 * `tools/validate_blocking.sh`.
 */
internal interface OverlayWindowHost {
    fun add(view: View): OperationResult

    fun remove(view: View)
}

/**
 * `TYPE_ACCESSIBILITY_OVERLAY` plein écran, **touchable** (`FLAG_NOT_FOCUSABLE` : la touche retour
 * et le clavier restent à l'application de dessous ; aucun `FLAG_NOT_TOUCHABLE`), étendu sous les
 * barres système et l'encoche (`FLAG_LAYOUT_IN_SCREEN`, aucun inset imposé) ; l'écran pose lui-même
 * ses marges (`NiumiScreenScaffold`, `safeDrawing`). La fenêtre porte le titre [WINDOW_TITLE], que
 * `tools/validate_blocking.sh` cherche dans `dumpsys window` pour ne compter qu'elle.
 *
 * [service] **doit être le service d'accessibilité lui-même**, jamais l'`@ApplicationContext` :
 * seul le service porte le jeton de fenêtre autorisant `TYPE_ACCESSIBILITY_OVERLAY`. Avec le
 * contexte d'application, `addView` lève `BadTokenException` (« token null is not valid »),
 * ce qui faisait planter l'application et désactiver le service (voir ETAPE-05.md).
 */
internal class WindowManagerOverlayWindowHost(
    service: AccessibilityService,
) : OverlayWindowHost {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    override fun add(view: View): OperationResult =
        try {
            windowManager.addView(view, layoutParams())
            OperationResult.Success
        } catch (error: WindowManager.BadTokenException) {
            // Le système refuse la fenêtre (jeton invalide, politique OEM). Ne jamais laisser
            // remonter : une exception ici tue l'application et fait désactiver le service.
            OperationResult.Failure("ANDROID_OVERLAY_REJECTED", error)
        } catch (error: WindowManager.InvalidDisplayException) {
            OperationResult.Failure("ANDROID_OVERLAY_REJECTED", error)
        }

    override fun remove(view: View) {
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // Fenêtre déjà retirée par le système (service débranché, écran détruit) : rien à
            // faire, et surtout pas propager — même raison que dans add().
        }
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE,
            )
        params.gravity = Gravity.TOP or Gravity.START
        params.title = WINDOW_TITLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.fitInsetsTypes = 0
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        return params
    }
}

/** Titre de la fenêtre de l'écran 7, lu par `tools/validate_blocking.sh`. */
internal const val WINDOW_TITLE = "NiumiBlockScreen"
