package com.niumi.designsystem.component

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.lerp
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Roue de valeurs (sélecteur d'heure, maquettes 19 et 20 ; étape 45) : cinq rangées visibles, la
 * valeur du milieu en 34/600 à l'encre, ses voisines en 22 grisées, les plus éloignées en 18 plus
 * claires. Pendant le défilement, une valeur passe d'un aspect à l'autre en continu selon sa distance
 * au milieu ([wheelRowLook], demande de Mehdi du 2026-10-31 ; d'un coup avant). La roue tourne sans
 * fin (après 23 vient 0) et s'arrête toujours sur une valeur.
 *
 * [onSelectedIndexChange] est appelé à chaque changement de la valeur centrée. Pour TalkBack, la roue est un réglage
 * nommé [contentDescription] (« Heures ») qui avance d'une valeur par geste.
 *
 * Les rangées ont toutes 42 de haut ; les deux traits de la valeur choisie sont dessinés par l'écran
 * qui assemble les roues.
 */
@Composable
fun NiumiWheelPicker(
    values: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val count = values.size
    val base = (REPEAT / 2) * count
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = base + selectedIndex - VISIBLE_ABOVE)
    val centered by remember(listState) { derivedStateOf { centeredItem(listState) } }
    val onChange by rememberUpdatedState(onSelectedIndexChange)
    val scope = rememberCoroutineScope()

    // La valeur centrée est publiée dès qu'elle change, pendant le défilement aussi : « Valider »
    // touché pendant l'accroche enregistre ce qui est affiché au centre (relecture de l'étape 45).
    LaunchedEffect(listState, count) {
        snapshotFlow { centered.mod(count) }
            .distinctUntilChanged()
            .collect { onChange(it) }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .width(NiumiDimens.wheelColumnWidth)
                .height(NiumiDimens.wheelRowHeight * VISIBLE_ROWS)
                .clearAndSetSemantics {
                    this.contentDescription = contentDescription
                    stateDescription = values[centered.mod(count)]
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(
                            centered.mod(count).toFloat(),
                            0f..(count - 1).toFloat(),
                            steps = count - 2,
                        )
                    setProgress { target ->
                        val index = target.toInt().coerceIn(0, count - 1)
                        scope.launch {
                            listState.scrollToItem(base + index - VISIBLE_ABOVE)
                            onChange(index)
                        }
                        true
                    }
                },
    ) {
        items(count = count * REPEAT) { item ->
            WheelRow(text = values[item.mod(count)], distance = { listState.distanceFromMiddle(item) })
        }
    }
}

/** Une rangée : les trois aspects superposés, dosés et mis à l'échelle au dessin, sans recomposer. */
@Composable
private fun WheelRow(
    text: String,
    distance: () -> Float,
) {
    val palette = LocalNiumiPalette.current
    Box(modifier = Modifier.fillMaxWidth().height(NiumiDimens.wheelRowHeight), contentAlignment = Alignment.Center) {
        WheelText(text, NiumiType.wheelSelected34, palette.ink, distance) { it.selectedAlpha }
        WheelText(text, NiumiType.wheelNear22, palette.border, distance) { it.nearAlpha }
        WheelText(text, NiumiType.wheelFar18, palette.disabled, distance) { it.farAlpha }
    }
}

@Composable
private fun WheelText(
    text: String,
    style: TextStyle,
    color: Color,
    distance: () -> Float,
    alpha: (WheelRowLook) -> Float,
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier =
            Modifier.wrapContentHeight(unbounded = true).graphicsLayer {
                val look = wheelRowLook(distance())
                this.alpha = alpha(look)
                scaleX = look.fontSize / style.fontSize.value
                scaleY = scaleX
            },
    )
}

/**
 * Aspect d'une rangée à [distance] rangées du milieu (0 au milieu, fractionnaire pendant le
 * défilement) : taille du texte, de 34 au milieu à 22 à une rangée et 18 à deux, et part de chaque
 * aspect (choisi, voisin, éloigné), qui passent de l'un à l'autre en fondu.
 */
internal data class WheelRowLook(
    val fontSize: Float,
    val selectedAlpha: Float,
    val nearAlpha: Float,
    val farAlpha: Float,
)

internal fun wheelRowLook(distance: Float): WheelRowLook {
    val selected = NiumiType.wheelSelected34.fontSize.value
    val near = NiumiType.wheelNear22.fontSize.value
    val far = NiumiType.wheelFar18.fontSize.value
    val d = distance.coerceIn(0f, 2f)
    return if (d <= 1f) {
        WheelRowLook(lerp(selected, near, d), selectedAlpha = 1f - d, nearAlpha = d, farAlpha = 0f)
    } else {
        WheelRowLook(lerp(near, far, d - 1f), selectedAlpha = 0f, nearAlpha = 2f - d, farAlpha = d - 1f)
    }
}

/** Distance, en rangées, entre le milieu de l'élément [index] et celui de la roue. */
private fun LazyListState.distanceFromMiddle(index: Int): Float {
    val layout = layoutInfo
    val item = layout.visibleItemsInfo.firstOrNull { it.index == index } ?: return OUT_OF_VIEW
    val middle = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
    return abs(item.offset + item.size / 2f - middle) / item.size
}

/** L'élément dont le milieu est le plus près du milieu de la roue. */
private fun centeredItem(state: LazyListState): Int {
    val layout = state.layoutInfo
    val middle = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
    return layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index
        ?: (state.firstVisibleItemIndex + VISIBLE_ABOVE)
}

private const val VISIBLE_ROWS = 5
private const val VISIBLE_ABOVE = 2
private const val OUT_OF_VIEW = 2f

/** Nombre de tours virtuels : assez pour qu'on n'en voie jamais le bout. */
private const val REPEAT = 1000

@Preview(name = "Clair", widthDp = 200)
@Composable
private fun NiumiWheelPickerPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiWheelPicker(
            values = (0..23).map { it.toString() },
            selectedIndex = 22,
            onSelectedIndexChange = {},
            contentDescription = "Heures",
        )
    }
}
