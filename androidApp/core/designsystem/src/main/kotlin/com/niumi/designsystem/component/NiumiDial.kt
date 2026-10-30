package com.niumi.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.niumi.designsystem.drawing.DialGeometry
import com.niumi.designsystem.drawing.DialHandle
import com.niumi.designsystem.drawing.DialLabelLayout
import com.niumi.designsystem.format.NiumiTimeFormat
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiPalette
import com.niumi.designsystem.ui.theme.NiumiType
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Cadran de 24 heures de l'accueil (VALEURS-DE-DESIGN.md §5, maquettes 15 à 20 ; étape 45).
 *
 * Gestes (décisions de Mehdi du 2026-10-25) : poser le doigt sur une poignée et bouger la fait
 * glisser de 5 en 5 minutes, dès que le mouvement dépasse le seuil du système ; [onTimeChanged] reçoit
 * l'heure au relâcher, jamais pendant le glisser. Un toucher bref ouvre le sélecteur d'heure :
 * [onTap] reçoit [DialHandle.BEDTIME] sur la poignée du coucher, [DialHandle.WAKE] partout ailleurs.
 * En session, pas de poignées : tout toucher appelle [onTap] avec [DialHandle.WAKE].
 *
 * Accessibilité : le centre se lit d'un bloc (« Réveil dans, 9 h 26 ») ; chaque poignée est un
 * réglage (« Coucher », « 22:30 ») que TalkBack fait avancer de 5 minutes, et dont le double toucher
 * ouvre le sélecteur ; en session, sans poignées, le cadran annonce ses deux heures.
 *
 * Zone de la largeur de sa carte de section (maquettes du 2026-10-30 : plus de débordement sur les
 * marges). L'anneau s'y réduit juste assez pour que l'étiquette la plus large tienne à côté d'une
 * poignée posée sur un côté, à 6 du bord, sans descendre sous les trois quarts de sa taille
 * (décision de Mehdi du 2026-10-30, après essai) ; la bande des étiquettes, au-dessus et au-dessous,
 * garde sa hauteur. Les étiquettes ne touchent jamais l'anneau ni l'autre étiquette, et restent
 * dans la zone : `DialLabelLayout`.
 */
@Suppress("LongParameterList") // Convention Compose : les paramètres d'un composant sont son API.
@Composable
fun NiumiDial(
    state: DialState,
    texts: DialTexts,
    onTimeChanged: (DialHandle, Int) -> Unit,
    onTap: (DialHandle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val labelWidth = remember(texts, measurer) { widestLabel(measurer, texts) }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val scale =
            with(LocalDensity.current) {
                min(
                    maxWidth / NiumiDimens.dialFitWidth,
                    DialLabelLayout.ringScale(
                        zoneWidth = maxWidth.toPx(),
                        labelWidth = labelWidth,
                        ringOuterRadius = (NiumiDimens.dialRadius + NiumiDimens.dialStroke / 2).toPx(),
                        gap = (NiumiDimens.dialLabelGap + NiumiDimens.dialLabelSideGap).toPx(),
                        edgeMargin = NiumiDimens.dialLabelEdgeMargin.toPx(),
                        minScale = NiumiDimens.DIAL_MIN_RING_SCALE,
                    ),
                )
            }
        DialBody(state, texts, scale, measurer, onTimeChanged, onTap)
    }
}

/**
 * Largeur, en pixels, de l'étiquette la plus large possible : le plus long des deux libellés, ou
 * l'heure la plus longue (chiffres de même largeur en IBM Plex Sans). Elle ne dépend pas de l'heure,
 * pour que l'anneau ne change pas de taille pendant un glisser.
 */
private fun widestLabel(
    measurer: TextMeasurer,
    texts: DialTexts,
): Float =
    maxOf(
        measurer.measure(texts.bedtime, NiumiType.caption13).size.width,
        measurer.measure(texts.wake, NiumiType.caption13).size.width,
        measurer.measure(WIDEST_CLOCK, NiumiType.dialHour17).size.width,
    ).toFloat()

/** Heure de cinq caractères, la plus longue que le cadran écrit. */
private const val WIDEST_CLOCK = "00:00"

/** Glisser en cours : la poignée et l'heure qu'elle montre. */
private data class DialDrag(
    val handle: DialHandle,
    val minutes: Int,
)

@Suppress("LongParameterList")
@Composable
private fun DialBody(
    state: DialState,
    texts: DialTexts,
    scale: Float,
    measurer: TextMeasurer,
    onTimeChanged: (DialHandle, Int) -> Unit,
    onTap: (DialHandle) -> Unit,
) {
    val palette = LocalNiumiPalette.current
    val density = LocalDensity.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    var drag by remember { mutableStateOf<DialDrag?>(null) }
    val current by rememberUpdatedState(state)
    val changed by rememberUpdatedState(onTimeChanged)
    val tapped by rememberUpdatedState(onTap)

    val bedtime = drag?.takeIf { it.handle == DialHandle.BEDTIME }?.minutes ?: state.bedtimeMinutes
    val wake = drag?.takeIf { it.handle == DialHandle.WAKE }?.minutes ?: state.wakeMinutes
    val height = NiumiDimens.dialCenterYFor(scale) * 2
    val hasHandles = state !is DialState.InSession

    Box(modifier = Modifier.fillMaxWidth().height(height)) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clearAndSetSemantics {
                        if (!hasHandles) {
                            contentDescription =
                                listOf(
                                    "${texts.bedtime} ${NiumiTimeFormat.clock(bedtime.toLocalTime())}",
                                    "${texts.wake} ${NiumiTimeFormat.clock(wake.toLocalTime())}",
                                ).joinToString(", ")
                            onClick(label = texts.chooseTime) {
                                tapped(DialHandle.WAKE)
                                true
                            }
                        }
                    }.dialGestures(
                        scale = scale,
                        touchSlop = touchSlop,
                        state = { current },
                        onDrag = { drag = it },
                        onRelease = { released -> changed(released.handle, released.minutes) },
                        onTap = { tapped(it) },
                    ),
        ) {
            val frame = DialFrame(size.width, scale, density.density)
            drawDial(frame, state, bedtime, wake, palette)
            drawLabels(frame, measurer, texts, state, bedtime to wake, palette)
        }
        DialCenter(state, scale, palette, Modifier.align(Alignment.TopCenter))
        if (hasHandles) {
            HandleNode(DialHandle.BEDTIME, texts.bedtime, bedtime, wake, scale, texts.chooseTime, onTimeChanged, onTap)
            HandleNode(DialHandle.WAKE, texts.wake, wake, bedtime, scale, texts.chooseTime, onTimeChanged, onTap)
        }
    }
}

/**
 * Gestes du cadran : un toucher sur une poignée suivi d'un mouvement au-delà de [touchSlop] la fait
 * glisser ([onDrag] à chaque cran, [onRelease] au relâcher) ; un toucher sans mouvement appelle
 * [onTap] avec la poignée du coucher si elle est sous le doigt, celle du réveil sinon.
 */
@Suppress("LongParameterList")
private fun Modifier.dialGestures(
    scale: Float,
    touchSlop: Float,
    state: () -> DialState,
    onDrag: (DialDrag?) -> Unit,
    onRelease: (DialDrag) -> Unit,
    onTap: (DialHandle) -> Unit,
): Modifier =
    pointerInput(scale) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val frame = DialFrame(size.width.toFloat(), scale, density)
            val start = state()
            val handle =
                if (start is DialState.InSession) {
                    null
                } else {
                    frame.handleAt(
                        down.position,
                        start.bedtimeMinutes,
                        start.wakeMinutes,
                    )
                }
            var travelled = 0f
            var lastDegrees = frame.degreesAt(down.position)
            var delta = 0f
            var dragged: DialDrag? = null
            var pressed = true
            try {
                while (pressed) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    pressed = change?.pressed == true
                    if (change != null && pressed) {
                        travelled += change.positionChange().getDistance()
                        if (handle != null && (dragged != null || travelled > touchSlop)) {
                            change.consume()
                            val degrees = frame.degreesAt(change.position)
                            delta += DialGeometry.signedDeltaMinutes(lastDegrees, degrees)
                            lastDegrees = degrees
                            dragged = DialDrag(handle, start.dragged(handle, delta))
                            onDrag(dragged)
                        }
                    }
                }
                val released = dragged
                if (released != null) {
                    onRelease(released)
                } else {
                    DialGeometry.tapTarget(handle, travelled, touchSlop)?.let(onTap)
                }
            } finally {
                // Geste annulé (défilement parent, fenêtre perdue) : la poignée revient à l'heure enregistrée.
                onDrag(null)
            }
        }
    }

/** Heure de [handle] déplacée de [delta] minutes depuis cet état, l'autre poignée fixe. */
private fun DialState.dragged(
    handle: DialHandle,
    delta: Float,
): Int =
    if (handle == DialHandle.BEDTIME) {
        DialGeometry.dragged(handle, bedtimeMinutes, wakeMinutes, delta)
    } else {
        DialGeometry.dragged(handle, wakeMinutes, bedtimeMinutes, delta)
    }

/** Centre du cadran : libellé 14 au-dessus de la valeur 42 (ou 30 grisée pour « Désactivé »). */
@Composable
private fun DialCenter(
    state: DialState,
    scale: Float,
    palette: NiumiPalette,
    modifier: Modifier,
) {
    val disabled = state is DialState.Disabled
    val labelColor = if (disabled) palette.border else palette.textSecondary
    val valueColor = if (disabled) palette.border else palette.ink
    // Le centre de l'anneau est à 158 du haut de la zone ; le bloc de texte s'y centre.
    Column(
        modifier =
            modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val centerY = NiumiDimens.dialCenterYFor(scale).roundToPx()
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, centerY - placeable.height / 2)
                    }
                }.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = state.centerLabel, style = NiumiType.secondary14, color = labelColor, textAlign = TextAlign.Center)
        if (state.centerValue.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NiumiDimens.dialCenterLabelGap))
            Text(
                text = state.centerValue,
                style = if (disabled) NiumiType.dialDisabled30 else NiumiType.dial42,
                color = valueColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Nœud d'accessibilité d'une poignée, invisible, posé sur elle : un réglage de 5 en 5 minutes
 * (TalkBack, balayage vers le haut ou le bas) et un double toucher qui ouvre le sélecteur.
 */
@Suppress("LongParameterList")
@Composable
private fun BoxScope.HandleNode(
    handle: DialHandle,
    label: String,
    minutes: Int,
    other: Int,
    scale: Float,
    chooseTime: String,
    onTimeChanged: (DialHandle, Int) -> Unit,
    onTap: (DialHandle) -> Unit,
) {
    val density = LocalDensity.current
    val size = NiumiDimens.touchTarget
    val position =
        with(density) {
            val frame = DialFrame(0f, scale, density.density)
            val point = frame.pointAt(minutes, centerX = 0f)
            IntOffset(
                // Aligné en haut au centre : le nœud est déjà centré sur l'axe du cadran.
                point.x.roundToInt(),
                (point.y - size.toPx() / 2).roundToInt(),
            )
        }
    Box(
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .offset { position }
                .size(size)
                .semantics {
                    contentDescription = label
                    stateDescription = NiumiTimeFormat.clock(minutes.toLocalTime())
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(
                            current = minutes.toFloat(),
                            range = 0f..(DialGeometry.MINUTES_PER_DAY - DialGeometry.DRAG_STEP_MINUTES).toFloat(),
                            steps = DialGeometry.MINUTES_PER_DAY / DialGeometry.DRAG_STEP_MINUTES - 2,
                        )
                    setProgress { target ->
                        val delta = target - minutes
                        onTimeChanged(handle, DialGeometry.dragged(handle, minutes, other, delta))
                        true
                    }
                    onClick(label = chooseTime) {
                        onTap(handle)
                        true
                    }
                },
    )
}

@Preview(name = "Réveil prévu", widthDp = 390)
@Composable
private fun NiumiDialScheduledPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiSectionCard(contentPadding = NiumiDimens.sectionCardDialPadding) {
            NiumiDial(
                state = DialState.Scheduled(PREVIEW_BEDTIME, PREVIEW_WAKE, PREVIEW_EVENING, "Réveil dans", "9 h 26"),
                texts = PREVIEW_TEXTS,
                onTimeChanged = { _, _ -> },
                onTap = {},
            )
        }
    }
}

@Preview(name = "Réveil désactivé", widthDp = 390)
@Composable
private fun NiumiDialDisabledPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiSectionCard(contentPadding = NiumiDimens.sectionCardDialPadding) {
            NiumiDial(
                state = DialState.Disabled(PREVIEW_BEDTIME, PREVIEW_WAKE, PREVIEW_EVENING, "Réveil", "Désactivé"),
                texts = PREVIEW_TEXTS,
                onTimeChanged = { _, _ -> },
                onTap = {},
            )
        }
    }
}

@Preview(name = "Session en cours", widthDp = 390)
@Composable
private fun NiumiDialInSessionPreview() {
    NiumiPreviewFrame(dark = true) {
        NiumiSectionCard(contentPadding = NiumiDimens.sectionCardDialPadding) {
            NiumiDial(
                state = DialState.InSession(PREVIEW_BEDTIME, PREVIEW_WAKE, PREVIEW_NIGHT, "Réveil dans", "7 h 50"),
                texts = PREVIEW_TEXTS,
                onTimeChanged = { _, _ -> },
                onTap = {},
            )
        }
    }
}

/** 22:30, 7:00, 21:34 et 23:10 : les heures des maquettes 16 et 17. */
private const val PREVIEW_BEDTIME = 1350
private const val PREVIEW_WAKE = 420
private const val PREVIEW_EVENING = 1294
private const val PREVIEW_NIGHT = 1390

private val PREVIEW_TEXTS = DialTexts(bedtime = "Coucher", wake = "Réveil", chooseTime = "Choisir l’heure")
