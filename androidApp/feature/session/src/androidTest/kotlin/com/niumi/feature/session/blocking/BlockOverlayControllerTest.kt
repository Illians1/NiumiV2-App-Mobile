package com.niumi.feature.session.blocking

import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.system.common.OperationResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Contrôleur de l'écran 7 (étape 39), derrière la couture [OverlayWindowHost] : la vraie fenêtre
 * `TYPE_ACCESSIBILITY_OVERLAY` exige le jeton du service relié, qu'aucune instrumentation ne peut
 * obtenir (SPEC_ANDROID §19.2) ; la vue est donc posée dans une **fenêtre à part** de l'activité de
 * test (`TYPE_APPLICATION_PANEL`), qui n'hérite d'aucun propriétaire de l'activité : la composition
 * n'y réussit que si le contrôleur pose lui-même les trois. Cela prouve la composition hors activité,
 * le cycle de vie et l'absence de fuite. La vraie fenêtre
 * est vérifiée sur appareil par `tools/validate_blocking.sh`. Remplace le
 * `BlockOverlayInstrumentedTest` prévu par le plan, irréalisable tel quel.
 */
@RunWith(AndroidJUnit4::class)
class BlockOverlayControllerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val host = ActivityWindowHost()
    private var openNiumiCalls = 0
    private var showingWhenNiumiOpened: Boolean? = null
    private lateinit var controller: WindowManagerBlockOverlayController

    private fun createController(windowHost: OverlayWindowHost = host) {
        composeRule.runOnUiThread {
            controller =
                WindowManagerBlockOverlayController(composeRule.activity, windowHost) {
                    openNiumiCalls++
                    showingWhenNiumiOpened = controller.isShowing
                }
        }
    }

    private fun show(appName: String? = "YouTube"): OperationResult {
        var result: OperationResult? = null
        composeRule.runOnUiThread { result = controller.show(appName) }
        composeRule.waitForIdle()
        return requireNotNull(result)
    }

    @Test
    fun showComposesTheBlockScreenWithAResumedLifecycle() {
        createController()

        assertThat(show()).isEqualTo(OperationResult.Success)

        composeRule.onNodeWithText("L’application YouTube est bloquée").assertExists()
        assertThat(controller.isShowing).isTrue()
        val view = host.added.single()
        assertThat(host.lifecycleOf(view)).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(view.findViewTreeSavedStateRegistryOwner()).isInstanceOf(OverlayViewTreeOwner::class.java)
        assertThat(view.findViewTreeViewModelStoreOwner()).isInstanceOf(OverlayViewTreeOwner::class.java)
    }

    /** Une autre application bloquée sous l'écran affiché : son nom remplace l'ancien, sans seconde fenêtre. */
    @Test
    fun aSecondShowRenamesTheScreenWithoutASecondWindow() {
        createController()
        show("YouTube")

        assertThat(show("Instagram")).isEqualTo(OperationResult.AlreadySatisfied)

        assertThat(host.added).hasSize(1)
        composeRule.onNodeWithText("L’application Instagram est bloquée").assertExists()
    }

    @Test
    fun withoutANameTheScreenShowsTheTitleOfTheMockup() {
        createController()

        show(appName = null)

        composeRule.onNodeWithText(BlockScreenTexts.FALLBACK_TITLE).assertExists()
    }

    @Test
    fun closeRemovesTheWindowAndDestroysItsLifecycle() {
        createController()
        show()
        val view = host.added.single()

        composeRule.onNodeWithText(BlockScreenTexts.CLOSE).performClick()
        composeRule.waitForIdle()

        assertThat(controller.isShowing).isFalse()
        assertThat(host.removed).containsExactly(view)
        assertThat(host.lifecycleOf(view)).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(view.isAttachedToWindow).isFalse()
    }

    @Test
    fun openNiumiRemovesTheWindowBeforeOpeningNiumi() {
        createController()
        show()

        composeRule.onNodeWithText(BlockScreenTexts.OPEN_NIUMI).performClick()
        composeRule.waitForIdle()

        assertThat(openNiumiCalls).isEqualTo(1)
        assertThat(showingWhenNiumiOpened).isFalse()
        assertThat(host.removed).hasSize(1)
    }

    @Test
    fun hideTwiceRemovesTheWindowOnce() {
        createController()
        show()

        composeRule.runOnUiThread {
            controller.hide()
            controller.hide()
        }

        assertThat(host.removed).hasSize(1)
    }

    @Test
    fun aRejectedWindowLeavesNothingBehind() {
        val rejecting = RejectingWindowHost()
        createController(rejecting)

        val result = show()

        assertThat(result).isInstanceOf(OperationResult.Failure::class.java)
        assertThat(controller.isShowing).isFalse()
        assertThat(
            rejecting.offered
                .single()
                .findViewTreeLifecycleOwner()
                ?.lifecycle
                ?.currentState,
        ).isEqualTo(Lifecycle.State.DESTROYED)
    }

    /** Pose la vue dans sa propre fenêtre, sur le jeton de l'activité, comme le service la pose sur le sien. */
    private inner class ActivityWindowHost : OverlayWindowHost {
        val added = mutableListOf<View>()
        val removed = mutableListOf<View>()

        override fun add(view: View): OperationResult {
            val activity = composeRule.activity
            val params =
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.OPAQUE,
                )
            params.token = activity.window.decorView.windowToken
            activity.windowManager.addView(view, params)
            added += view
            return OperationResult.Success
        }

        override fun remove(view: View) {
            composeRule.activity.windowManager.removeViewImmediate(view)
            removed += view
        }

        fun lifecycleOf(view: View): Lifecycle.State? = view.findViewTreeLifecycleOwner()?.lifecycle?.currentState
    }

    private class RejectingWindowHost : OverlayWindowHost {
        val offered = mutableListOf<View>()

        override fun add(view: View): OperationResult {
            offered += view
            return OperationResult.Failure("ANDROID_OVERLAY_REJECTED")
        }

        override fun remove(view: View) = Unit
    }
}
