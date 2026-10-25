package com.niumi.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niumi.designsystem.R
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Icône d'un onglet : un vecteur teinté par la barre, au trait ([id]) et plein quand l'onglet est
 * choisi ([selectedId], sinon le même), ou l'horloge de l'onglet « Réveil ».
 */
sealed interface NiumiTabIcon {
    data class Vector(
        @param:DrawableRes val id: Int,
        @param:DrawableRes val selectedId: Int = id,
    ) : NiumiTabIcon

    /** Horloge pleine à aiguilles claires quand l'onglet est choisi, au trait sinon (maquette 16). */
    data object Clock : NiumiTabIcon
}

/**
 * Un onglet de la barre. Avec un [badge] (« Bientôt »), l'onglet n'est pas encore disponible : grisé,
 * sans action, annoncé désactivé. [accessibilityLabel] est ce que lit TalkBack (« Suivi, bientôt
 * disponible ») ; il vient du `Texts` de l'écran, comme [label] et [badge].
 */
data class NiumiTabBarItem(
    val label: String,
    val icon: NiumiTabIcon,
    val badge: String? = null,
    val accessibilityLabel: String = label,
) {
    val available: Boolean get() = badge == null
}

/**
 * Barre d'onglets du bas (maquette 16, décision de Mehdi du 2026-10-25), bord à bord : fond
 * « Surface », ombre légère, onglets de 90 centrés, chacun centré verticalement dans 66 ; son fond
 * couvre la barre système. L'onglet [selectedIndex] est à l'encre (libellé 12/600) ; les autres à la
 * bordure (12/500) ; un onglet « Bientôt » est en « Désactivé » avec son badge en pilule « Tuile
 * grise ». Les couleurs sont celles du thème : en session, la barre est sombre comme l'écran.
 *
 * [onSelect] reçoit l'index d'un onglet disponible touché, choisi ou non ; jamais celui d'un onglet
 * « Bientôt », qui refuse le toucher par un tremblement et une vibration ([Refusal]). Se pose dans l'emplacement `navigationBar` de [NiumiScreenScaffold].
 */
@Composable
fun NiumiTabBar(
    items: List<NiumiTabBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(NiumiDimens.tabBarElevation)
                .background(palette.surface)
                .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(NiumiDimens.tabBarHeight).selectableGroup(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                Tab(item = item, selected = index == selectedIndex, onClick = { onSelect(index) })
            }
        }
    }
}

@Composable
private fun Tab(
    item: NiumiTabBarItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalNiumiPalette.current
    val color =
        when {
            selected -> palette.ink
            item.available -> palette.border
            else -> palette.disabled
        }
    val refusal = rememberRefusal()
    val action =
        if (item.available) {
            Modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick)
        } else {
            // Un geste, pas une action : TalkBack n'y voit aucun clic et l'onglet reste désactivé.
            Modifier.pointerInput(Unit) { detectTapGestures(onTap = { refusal.play() }) }
        }
    Column(
        modifier =
            Modifier
                .width(NiumiDimens.tabBarItemWidth)
                .fillMaxHeight()
                .then(action)
                .clearAndSetSemantics {
                    role = Role.Tab
                    this.selected = selected
                    contentDescription = item.accessibilityLabel
                    if (!item.available) disabled()
                }.graphicsLayer { translationX = refusal.offset.dp.toPx() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NiumiDimens.tabBarLabelGap, Alignment.CenterVertically),
    ) {
        TabIcon(item.icon, selected, color)
        Text(
            text = item.label,
            style = if (selected) NiumiType.tabBarLabelSelected12 else NiumiType.tabBarLabel12,
            color = color,
            maxLines = 1,
        )
        item.badge?.let { badge -> Badge(badge) }
    }
}

/**
 * Refus d'un onglet « Bientôt » touché (décision de Mehdi du 2026-10-25) : l'onglet tremble de
 * gauche à droite, de [NiumiDimens.tabBarShake] au plus en s'amortissant, et le téléphone vibre
 * « refus ». Rien ne s'ouvre : on voit que l'onglet a été touché et qu'il ne peut pas l'être.
 */
private class Refusal(
    private val shake: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
    private val haptics: HapticFeedback,
    private val amplitude: Float,
) {
    val offset: Float get() = shake.value

    fun play() {
        haptics.performHapticFeedback(HapticFeedbackType.Reject)
        scope.launch {
            shake.snapTo(0f)
            shake.animateTo(0f, refusalShake(amplitude))
        }
    }
}

@Composable
private fun rememberRefusal(): Refusal {
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    return remember(shake, scope, haptics) { Refusal(shake, scope, haptics, NiumiDimens.tabBarShake.value) }
}

/**
 * Deux allers-retours amortis, 3 puis 1,5, avant le repos, en [SHAKE_MILLIS] (raccourci et adouci à
 * la demande de Mehdi le 2026-10-25 : trois allers-retours de 6 en 360 ms se voyaient trop).
 */
private fun refusalShake(amplitude: Float) =
    keyframes {
        durationMillis = SHAKE_MILLIS
        SHAKE_STEPS.forEachIndexed { index, factor -> amplitude * factor at SHAKE_STEP_MILLIS * (index + 1) }
    }

@Composable
private fun TabIcon(
    icon: NiumiTabIcon,
    selected: Boolean,
    color: Color,
) {
    when (icon) {
        is NiumiTabIcon.Vector -> {
            Icon(
                painter = painterResource(if (selected) icon.selectedId else icon.id),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(NiumiDimens.tabBarIcon),
            )
        }

        NiumiTabIcon.Clock -> {
            ClockIcon(
                filled = selected,
                color = color,
                hands = if (selected) LocalNiumiPalette.current.onInk else color,
            )
        }
    }
}

/**
 * Horloge de l'onglet « Réveil », tracé de la maquette 16 (viewBox 24) : cercle de 9,5 plein et
 * aiguilles de 1,8 « sur encre » quand l'onglet est choisi ; cercle et aiguilles au trait de 1,7
 * sinon (non dessiné : quand « Réglages » est choisi, choix du 2026-10-25).
 */
@Composable
private fun ClockIcon(
    filled: Boolean,
    color: Color,
    hands: Color,
) {
    Canvas(modifier = Modifier.size(NiumiDimens.tabBarIcon)) {
        val unit = size.minDimension / CLOCK_VIEWBOX
        val center = Offset(size.width / 2, size.height / 2)
        val stroke = (if (filled) CLOCK_HANDS_STROKE else CLOCK_STROKE) * unit
        if (filled) {
            drawCircle(color = color, radius = CLOCK_RADIUS * unit, center = center)
        } else {
            drawCircle(color = color, radius = CLOCK_RADIUS * unit, center = center, style = Stroke(width = stroke))
        }
        val path =
            Path().apply {
                moveTo(CLOCK_CENTER * unit, HAND_TOP * unit)
                lineTo(CLOCK_CENTER * unit, CLOCK_CENTER * unit)
                lineTo(HAND_END_X * unit, HAND_END_Y * unit)
            }
        drawPath(
            path = path,
            color = hands,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * Badge « Bientôt » : pilule de 13 de haut au moins (elle grandit avec la police), 6 de marge, 2 sous
 * le libellé comme le libellé sous l'icône.
 */
@Composable
private fun Badge(text: String) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            Modifier
                .heightIn(min = NiumiDimens.tabBarBadgeHeight)
                .clip(NiumiShapes.pill)
                .background(palette.tileGrey)
                .padding(horizontal = NiumiDimens.tabBarBadgePadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = NiumiType.badge9, color = palette.border, maxLines = 1)
    }
}

private const val SHAKE_MILLIS = 200
private const val SHAKE_STEP_MILLIS = 40
private val SHAKE_STEPS = listOf(-1f, 1f, -0.5f, 0.5f)
private const val CLOCK_VIEWBOX = 24f
private const val CLOCK_CENTER = 12f
private const val CLOCK_RADIUS = 9.5f
private const val CLOCK_STROKE = 1.7f
private const val CLOCK_HANDS_STROKE = 1.8f
private const val HAND_TOP = 7.5f
private const val HAND_END_X = 15f
private const val HAND_END_Y = 14f

private val previewItems =
    listOf(
        NiumiTabBarItem("Réveil", NiumiTabIcon.Clock),
        NiumiTabBarItem("Blocage seul", NiumiTabIcon.Vector(R.drawable.ic_tab_shield), badge = "Bientôt"),
        NiumiTabBarItem("Suivi", NiumiTabIcon.Vector(R.drawable.ic_tab_tracking), badge = "Bientôt"),
        NiumiTabBarItem("Réglages", NiumiTabIcon.Vector(R.drawable.ic_tab_settings, R.drawable.ic_tab_settings_filled)),
    )

@Preview(name = "Clair", widthDp = 390)
@Composable
private fun NiumiTabBarLightPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiTabBar(items = previewItems, selectedIndex = 0, onSelect = {})
        NiumiTabBar(items = previewItems, selectedIndex = 3, onSelect = {})
    }
}

@Preview(name = "Sombre", widthDp = 390)
@Composable
private fun NiumiTabBarDarkPreview() {
    NiumiPreviewFrame(dark = true) {
        NiumiTabBar(items = previewItems, selectedIndex = 0, onSelect = {})
        NiumiTabBar(items = previewItems, selectedIndex = 3, onSelect = {})
    }
}
