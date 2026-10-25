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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Roue de valeurs (sélecteur d'heure, maquettes 19 et 20 ; étape 45) : cinq rangées visibles, la
 * valeur du milieu en 34/600 à l'encre, ses voisines en 22 grisées, les plus éloignées en 18 plus
 * claires. La roue tourne sans fin (après 23 vient 0) et s'arrête toujours sur une valeur.
 *
 * [onSelectedIndexChange] est appelé à chaque changement de la valeur centrée. Pour TalkBack, la roue est un réglage
 * nommé [contentDescription] (« Heures ») qui avance d'une valeur par geste.
 *
 * Écart aux maquettes : rangées de hauteur égale (42), là où les maquettes mettent 36, 40, 56, 40 et
 * 36 ; les deux traits de la valeur choisie sont dessinés par l'écran qui assemble les roues.
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
            WheelRow(text = values[item.mod(count)], distance = abs(item - centered))
        }
    }
}

@Composable
private fun WheelRow(
    text: String,
    distance: Int,
) {
    val palette = LocalNiumiPalette.current
    val (style, color) =
        when (distance) {
            0 -> NiumiType.wheelSelected34 to palette.ink
            1 -> NiumiType.wheelNear22 to palette.border
            else -> NiumiType.wheelFar18 to palette.disabled
        }
    Box(modifier = Modifier.fillMaxWidth().height(NiumiDimens.wheelRowHeight), contentAlignment = Alignment.Center) {
        Text(text = text, style = style, color = color, modifier = Modifier.wrapContentHeight(unbounded = true))
    }
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
