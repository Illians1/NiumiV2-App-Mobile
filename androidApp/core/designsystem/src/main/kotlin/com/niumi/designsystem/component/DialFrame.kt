package com.niumi.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import com.niumi.designsystem.drawing.AmberSpans
import com.niumi.designsystem.drawing.DialGeometry
import com.niumi.designsystem.drawing.DialHandle
import com.niumi.designsystem.drawing.DialLabelLayout
import com.niumi.designsystem.drawing.DialLabelSpace
import com.niumi.designsystem.drawing.DialMarks
import com.niumi.designsystem.format.NiumiTimeFormat
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiPalette
import com.niumi.designsystem.ui.theme.NiumiType
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Repère et dessin du cadran de l’accueil (VALEURS-DE-DESIGN.md §5), séparés de son composant `NiumiDial`.

/**
 * Repère du cadran dans une zone de [width] pixels : centre horizontal, centre vertical à 158 × [scale],
 * rayon et trait réduits avec la zone. [density] convertit les dp des valeurs de design.
 */
internal class DialFrame(
    width: Float,
    private val scale: Float,
    private val density: Float,
) {
    val centerX = width / 2
    val centerY = px(NiumiDimens.dialCenterY)
    val radius = px(NiumiDimens.dialRadius)
    val stroke = px(NiumiDimens.dialStroke)

    fun px(dp: Dp): Float = dp.value * density * scale

    /** Point de l'anneau à l'heure [minutes] ; [centerX] nul pour un repère centré sur le cadran. */
    fun pointAt(
        minutes: Int,
        centerX: Float = this.centerX,
        radius: Float = this.radius,
    ): Offset {
        val radians = Math.toRadians(DialGeometry.degreesOf(minutes).toDouble())
        return Offset(centerX + radius * sin(radians).toFloat(), centerY - radius * cos(radians).toFloat())
    }

    fun degreesAt(position: Offset): Float = DialGeometry.degreesOfPoint(position.x - centerX, position.y - centerY)

    /** La poignée la plus proche dans le rayon de saisie, ou `null`. */
    fun handleAt(
        position: Offset,
        bedtime: Int,
        wake: Int,
    ): DialHandle? {
        val grab = NiumiDimens.dialHandleGrabRadius.value * density
        val toBedtime = (position - pointAt(bedtime)).getDistance()
        val toWake = (position - pointAt(wake)).getDistance()
        return when {
            toWake <= grab && toWake <= toBedtime -> DialHandle.WAKE
            toBedtime <= grab -> DialHandle.BEDTIME
            else -> null
        }
    }
}

internal fun DrawScope.drawDial(
    frame: DialFrame,
    state: DialState,
    bedtime: Int,
    wake: Int,
    palette: NiumiPalette,
) {
    val center = Offset(frame.centerX, frame.centerY)
    val ring = Size(frame.radius * 2, frame.radius * 2)
    val topLeft = Offset(frame.centerX - frame.radius, frame.centerY - frame.radius)
    val stroke = Stroke(width = frame.stroke, cap = StrokeCap.Round)
    val startAngle = DialGeometry.degreesOf(bedtime) - QUARTER_TURN
    val sweep = DialGeometry.periodMinutes(bedtime, wake) * DEGREES_PER_MINUTE
    val arcColor = if (state is DialState.Disabled) palette.disabled else palette.ink

    drawCircle(color = palette.track, radius = frame.radius, center = center, style = Stroke(width = frame.stroke))
    drawArc(arcColor, startAngle, sweep, useCenter = false, topLeft = topLeft, size = ring, style = stroke)
    drawQuarterMarks(frame, state.nowMinutes, palette.disabled)

    val dotRadius = frame.px(NiumiDimens.dialNowDotRadius)
    val now = frame.pointAt(state.nowMinutes)
    when (state) {
        is DialState.InSession -> {
            val elapsed = min(DialGeometry.elapsedMinutes(bedtime, state.nowMinutes) * DEGREES_PER_MINUTE, sweep)
            drawAmberFill(frame, bedtime, elapsed, sweep, palette.amber)
            drawCircle(palette.background, dotRadius, now)
            drawNowArrow(frame, state.nowMinutes, palette.ink)
        }

        is DialState.Scheduled, is DialState.Disabled -> {
            // Sur l'arc de la nuit, le point prend la couleur des poignées : gris sur gris, il
            // disparaissait en « Réveil désactivé » (essai du 2026-10-25).
            val greyed = state is DialState.Disabled
            val onArc = DialGeometry.isWithinPeriod(bedtime, wake, state.nowMinutes)
            val dot =
                when {
                    onArc -> palette.surface
                    greyed -> palette.disabled
                    else -> palette.textSecondary
                }
            drawCircle(dot, dotRadius, now)
            drawNowArrow(frame, state.nowMinutes, if (greyed) palette.border else palette.ink)
            val handleRadius = frame.px(NiumiDimens.dialHandleRadius)
            val wakeColor = if (greyed) palette.surface else palette.amber
            drawCircle(palette.surface, handleRadius, frame.pointAt(bedtime))
            drawCircle(wakeColor, handleRadius, frame.pointAt(wake))
        }
    }
}

/**
 * Repères de 0 h, 6 h, 12 h et 18 h : traits à l'intérieur de l'anneau, alignés sur la flèche, qui
 * s'effacent quand elle passe dessus (`DialMarks.isHidden`, décision de Mehdi du 2026-10-26).
 */
private fun DrawScope.drawQuarterMarks(
    frame: DialFrame,
    nowMinutes: Int,
    color: Color,
) {
    val outer = frame.radius - frame.stroke / 2 - frame.px(NiumiDimens.dialNowArrowGap)
    val inner = outer - frame.px(NiumiDimens.dialMarkLength)
    val width = frame.px(NiumiDimens.dialMarkWidth)
    DialMarks.QUARTER_MARKS
        .filterNot { DialMarks.isHidden(it, nowMinutes) }
        .forEach { minutes ->
            drawLine(color, frame.pointAt(minutes, radius = outer), frame.pointAt(minutes, radius = inner), width)
        }
}

/** Petit triangle plein à l'intérieur de l'anneau, pointé vers le point de l'heure actuelle. */
private fun DrawScope.drawNowArrow(
    frame: DialFrame,
    nowMinutes: Int,
    color: Color,
) {
    val radians = Math.toRadians(DialGeometry.degreesOf(nowMinutes).toDouble())
    val outward = Offset(sin(radians).toFloat(), -cos(radians).toFloat())
    val side = Offset(-outward.y, outward.x)
    val center = Offset(frame.centerX, frame.centerY)
    val tipRadius = frame.radius - frame.stroke / 2 - frame.px(NiumiDimens.dialNowArrowGap)
    val tip = center + outward * tipRadius
    val base = center + outward * (tipRadius - frame.px(NiumiDimens.dialNowArrowLength))
    val halfWidth = frame.px(NiumiDimens.dialNowArrowWidth) / 2
    val path =
        Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(base.x + side.x * halfWidth, base.y + side.y * halfWidth)
            lineTo(base.x - side.x * halfWidth, base.y - side.y * halfWidth)
            close()
        }
    drawPath(path, color)
}

/**
 * Ambre plein du coucher jusqu'au point de l'heure actuelle, puis fondu droit sur 12° à partir du
 * point (`DialGeometry.FADE_DEGREES`, décision du 2026-10-25), là où les maquettes empilent des
 * segments de 1,5°. Deux tracés : la partie pleine, au bout rond côté coucher, puis le fondu, avec
 * son propre dégradé tourné vers le point. Un seul dégradé sur un tour ne tenait pas au-delà de
 * 338° écoulés (période de 24 h, relecture du 2026-10-25) : découpés, ils tiennent quelle que soit
 * la période (`AmberSpans.of`).
 */
private fun DrawScope.drawAmberFill(
    frame: DialFrame,
    bedtime: Int,
    elapsedDegrees: Float,
    sweep: Float,
    amber: Color,
) {
    val spans = AmberSpans.of(elapsedDegrees, sweep)
    val center = Offset(frame.centerX, frame.centerY)
    val topLeft = Offset(frame.centerX - frame.radius, frame.centerY - frame.radius)
    val ring = Size(frame.radius * 2, frame.radius * 2)
    val butt = Stroke(width = frame.stroke, cap = StrokeCap.Butt)
    val start = DialGeometry.degreesOf(bedtime) - QUARTER_TURN
    // Bout rond côté coucher ; côté point, l'ambre s'arrête net et le fondu prend le relais.
    drawCircle(amber, frame.stroke / 2, frame.pointAt(bedtime))
    if (spans.solid >
        0f
    ) {
        drawArc(amber, start, spans.solid, useCenter = false, topLeft = topLeft, size = ring, style = butt)
    }
    if (spans.fade <= 0f) return
    val stops =
        buildList {
            add(0f to amber)
            add(fraction(SEAM_DEGREES) to amber)
            for (step in 1..FADE_SAMPLES) {
                val past = DialGeometry.FADE_DEGREES * step / FADE_SAMPLES
                add(fraction(SEAM_DEGREES + past) to amber.copy(alpha = DialGeometry.fadeAlpha(past)))
            }
            add(1f to amber.copy(alpha = 0f))
        }.toTypedArray()
    // Le dégradé commence un peu avant le point, pour que son raccord (fin du tour contre début)
    // ne tombe jamais sur le tracé.
    rotate(start + spans.solid - SEAM_DEGREES, pivot = center) {
        drawArc(
            brush = Brush.sweepGradient(colorStops = stops, center = center),
            startAngle = SEAM_DEGREES,
            sweepAngle = spans.fade,
            useCenter = false,
            topLeft = topLeft,
            size = ring,
            style = butt,
        )
    }
}

private fun fraction(degrees: Float): Float = (degrees / FULL_TURN).coerceIn(0f, 1f)

/**
 * « Coucher 22:30 » et « Réveil 7:00 » à l'extérieur de l'anneau, près de leur poignée : alignés à
 * droite sur la moitié gauche du cadran, à gauche sur la moitié droite, centrés en haut et en bas ;
 * jamais hors de la zone.
 */
@Suppress("LongParameterList")
internal fun DrawScope.drawLabels(
    frame: DialFrame,
    measurer: TextMeasurer,
    texts: DialTexts,
    state: DialState,
    times: Pair<Int, Int>,
    palette: NiumiPalette,
) {
    val captionColor = if (state is DialState.Scheduled) palette.textSecondary else palette.border
    val hourColor = if (state is DialState.Scheduled) palette.ink else palette.border
    val blocks =
        listOf(texts.bedtime to times.first, texts.wake to times.second).map { (caption, minutes) ->
            measurer.measure(caption, NiumiType.caption13.copy(color = captionColor)) to
                measurer.measure(
                    NiumiTimeFormat.clock(minutes.toLocalTime()),
                    NiumiType.dialHour17.copy(color = hourColor),
                )
        }
    val sizes =
        blocks.map { (caption, hour) ->
            maxOf(caption.size.width, hour.size.width).toFloat() to (caption.size.height + hour.size.height).toFloat()
        }
    val space =
        DialLabelSpace(
            width = size.width,
            height = size.height,
            centerX = frame.centerX,
            centerY = frame.centerY,
            clearRadius = frame.radius + frame.stroke / 2 + frame.px(NiumiDimens.dialLabelGap),
        )
    val (bedtimeBox, wakeBox) =
        DialLabelLayout.placeBoth(
            DialGeometry.degreesOf(times.first),
            sizes[0],
            DialGeometry.degreesOf(times.second),
            sizes[1],
            space,
        )
    listOf(bedtimeBox, wakeBox).forEachIndexed { index, box ->
        val (caption, hour) = blocks[index]
        // Lignes alignées sur le bord de la boîte tourné vers l'anneau, centrées en haut et en bas.
        val center = (box.left + box.right) / 2
        val alignment =
            when {
                box.right <= frame.centerX -> 1f
                box.left >= frame.centerX -> 0f
                else -> CENTERED
            }

        fun xOf(lineWidth: Int) =
            when (alignment) {
                1f -> box.right - lineWidth
                0f -> box.left
                else -> center - lineWidth / 2f
            }
        drawText(caption, topLeft = Offset(xOf(caption.size.width), box.top))
        drawText(hour, topLeft = Offset(xOf(hour.size.width), box.top + caption.size.height))
    }
}

internal fun Int.toLocalTime(): LocalTime = LocalTime.of(this / MINUTES_PER_HOUR, this % MINUTES_PER_HOUR)

private const val MINUTES_PER_HOUR = 60
private const val QUARTER_TURN = 90f
private const val FULL_TURN = 360f
private const val DEGREES_PER_MINUTE = FULL_TURN / DialGeometry.MINUTES_PER_DAY
private const val SEAM_DEGREES = 1f
private const val FADE_SAMPLES = 4
private const val CENTERED = 0.5f
