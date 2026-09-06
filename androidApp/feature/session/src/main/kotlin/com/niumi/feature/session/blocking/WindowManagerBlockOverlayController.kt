package com.niumi.feature.session.blocking

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.niumi.designsystem.ui.theme.NiumiColors
import com.niumi.feature.session.R
import com.niumi.system.common.OperationResult

private const val OVERLAY_DISPLAY_DURATION_MS = 3_000L
private const val CORNER_RADIUS_DP = 12f
private const val HORIZONTAL_PADDING_DP = 20
private const val VERTICAL_PADDING_DP = 14
private const val TOP_MARGIN_DP = 48

/**
 * `TYPE_ACCESSIBILITY_OVERLAY` non focusable et non touchable (SPEC_ANDROID §12.2) : un
 * bandeau en haut d'écran, jamais plein écran, pour que « le téléphone reste utilisable »
 * pendant son affichage. Retrait automatique après 3 s, ou plus tôt sur [hide] explicite dès
 * que le package bloqué n'est plus au premier plan. Couleurs de
 * `docs/CHARTE_GRAPHIQUE_APP_MOBILE.md` (fond Surface, texte clair, rayon de coin modéré,
 * sans capsule ni Terracotta : le blocage est le fonctionnement attendu, pas une alerte).
 *
 * [context] **doit être le `AccessibilityService` lui-même**, jamais l'`@ApplicationContext` :
 * seul le service porte le token de fenêtre autorisant `TYPE_ACCESSIBILITY_OVERLAY`. Avec le
 * contexte d'application, `addView` lève `BadTokenException` (« token null is not valid »),
 * ce qui faisait planter l'application et désactiver le service (voir ETAPE-05.md).
 */
class WindowManagerBlockOverlayController(
    private val context: Context,
) : BlockOverlayController {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { removeView() }
    private var overlayView: TextView? = null

    override val isShowing: Boolean
        get() = overlayView != null

    override fun show(displayName: String): OperationResult {
        mainHandler.removeCallbacks(hideRunnable)
        val existing = overlayView
        if (existing != null) {
            existing.text = context.getString(R.string.niumi_block_overlay_text, displayName)
            mainHandler.postDelayed(hideRunnable, OVERLAY_DISPLAY_DURATION_MS)
            return OperationResult.AlreadySatisfied
        }
        val view = createOverlayView()
        view.text = context.getString(R.string.niumi_block_overlay_text, displayName)
        return try {
            windowManager.addView(view, layoutParams())
            overlayView = view
            mainHandler.postDelayed(hideRunnable, OVERLAY_DISPLAY_DURATION_MS)
            OperationResult.Success
        } catch (error: WindowManager.BadTokenException) {
            // Le système refuse la fenêtre (token invalide, politique OEM). Ne jamais laisser
            // remonter : une exception ici tue l'application et fait désactiver le service.
            OperationResult.Failure("ANDROID_OVERLAY_REJECTED", error)
        }
    }

    override fun hide() {
        mainHandler.removeCallbacks(hideRunnable)
        removeView()
    }

    private fun removeView() {
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // Fenêtre déjà retirée par le système (service débranché, écran détruit) : rien à
            // faire, et surtout pas propager — même raison que dans show().
        }
    }

    private fun createOverlayView(): TextView {
        val density = context.resources.displayMetrics.density
        return TextView(context).apply {
            setTextColor(NiumiColors.TextPrincipal.toArgb())
            setPadding(
                (HORIZONTAL_PADDING_DP * density).toInt(),
                (VERTICAL_PADDING_DP * density).toInt(),
                (HORIZONTAL_PADDING_DP * density).toInt(),
                (VERTICAL_PADDING_DP * density).toInt(),
            )
            background =
                GradientDrawable().apply {
                    setColor(NiumiColors.Surface.toArgb())
                    cornerRadius = CORNER_RADIUS_DP * density
                }
        }
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val density = context.resources.displayMetrics.density
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT,
            )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = (TOP_MARGIN_DP * density).toInt()
        return params
    }
}
