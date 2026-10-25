package com.niumi.designsystem.drawing

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin

/** Rectangle d'une étiquette du cadran, en pixels, dans le repère de la zone du cadran. */
data class LabelBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    /** Recouvrement strict : deux boîtes qui se touchent ne se chevauchent pas. */
    fun intersects(other: LabelBox): Boolean =
        left < other.right && other.left < right && top < other.bottom && other.top < bottom

    /** Distance du point ([x], [y]) au point le plus proche de la boîte (zéro à l'intérieur). */
    fun distanceFrom(
        x: Float,
        y: Float,
    ): Float {
        val dx = maxOf(left - x, 0f, x - right)
        val dy = maxOf(top - y, 0f, y - bottom)
        return hypot(dx, dy)
    }

    fun inside(space: DialLabelSpace): Boolean =
        left >= 0f && top >= 0f && right <= space.width && bottom <= space.height
}

/**
 * La zone où placer les étiquettes : ses dimensions, le centre de l'anneau, la distance au centre en
 * deçà de laquelle une étiquette toucherait l'anneau ([clearRadius] : rayon, demi-trait et écart), et
 * les boîtes à éviter (icône des réglages).
 */
data class DialLabelSpace(
    val width: Float,
    val height: Float,
    val centerX: Float,
    val centerY: Float,
    val clearRadius: Float,
    val obstacles: List<LabelBox> = emptyList(),
)

/**
 * Placement des étiquettes « Coucher 22:30 » et « Réveil 7:00 » autour du cadran (essai sur appareil
 * du 2026-10-25, décision de Mehdi) : chacune près de sa poignée, à l'extérieur de l'anneau, jamais
 * sur l'anneau, sur l'autre étiquette ni sur un obstacle, toujours dans la zone.
 *
 * Une étiquette est alignée vers l'extérieur (à droite de son ancrage sur la moitié droite, à gauche
 * sur la moitié gauche, centrée en haut et en bas), centrée verticalement sur son ancrage, et son
 * ancrage est poussé vers l'extérieur juste assez pour qu'elle ne touche pas l'anneau. Quand cette
 * place est prise ou hors de la zone, l'ancrage glisse le long de l'anneau par crans de
 * [SLIDE_STEP_DEGREES], en s'écartant d'abord de l'autre étiquette ; faute de place, l'étiquette est
 * ramenée dans la zone.
 */
object DialLabelLayout {
    private const val SLIDE_STEP_DEGREES = 2f
    private const val MAX_SLIDE_DEGREES = 90f
    private const val RADIUS_SEARCH_STEPS = 16
    private const val MAX_EXTRA_RADIUS = 200f
    private const val CENTERED_SINE = 0.3f
    private const val HALF_TURN = 180f
    private const val FULL_TURN = 360f

    fun place(
        degrees: Float,
        size: Pair<Float, Float>,
        space: DialLabelSpace,
        avoid: List<LabelBox> = emptyList(),
        awayFromDegrees: Float? = null,
    ): LabelBox {
        val away = awayFromDegrees?.let { sign(signedDelta(it, degrees)).takeIf { s -> s != 0f } } ?: 1f
        val blocked = space.obstacles + avoid
        val steps = (MAX_SLIDE_DEGREES / SLIDE_STEP_DEGREES).toInt()
        val offsets =
            listOf(0f) +
                (1..steps).flatMap { k -> listOf(away * k * SLIDE_STEP_DEGREES, -away * k * SLIDE_STEP_DEGREES) }
        return offsets
            .asSequence()
            .map { offset -> boxAt(degrees + offset, size, space) }
            .firstOrNull { box -> box.inside(space) && blocked.none(box::intersects) }
            ?: clamped(boxAt(degrees, size, space), space)
    }

    /**
     * Les deux étiquettes ensemble. Chacune prend d'abord sa place seule ; si elles se chevauchent
     * ou s'inversent, elles s'écartent ensemble, chacune de son côté : celle du coucher recule, celle
     * du réveil avance dans le sens qui va du coucher au réveil. Elles ne peuvent donc jamais
     * s'inverser (essai sur appareil du 2026-10-25). Le plus petit écartement total gagne, partagé
     * au plus près de l'égalité entre les deux.
     */
    fun placeBoth(
        bedtimeDegrees: Float,
        bedtimeSize: Pair<Float, Float>,
        wakeDegrees: Float,
        wakeSize: Pair<Float, Float>,
        space: DialLabelSpace,
    ): Pair<LabelBox, LabelBox> {
        val forward = sign(signedDelta(bedtimeDegrees, wakeDegrees)).takeIf { it != 0f } ?: 1f
        val bedtime = place(bedtimeDegrees, bedtimeSize, space, awayFromDegrees = wakeDegrees)
        val wake = place(wakeDegrees, wakeSize, space, awayFromDegrees = bedtimeDegrees)
        if (fits(bedtime, wake, forward, space)) return bedtime to wake
        val steps = (MAX_SLIDE_DEGREES / SLIDE_STEP_DEGREES).toInt()
        val bedtimeBoxes =
            (0..steps).map { k ->
                boxAt(bedtimeDegrees - forward * k * SLIDE_STEP_DEGREES, bedtimeSize, space)
            }
        val wakeBoxes = (0..steps).map { k -> boxAt(wakeDegrees + forward * k * SLIDE_STEP_DEGREES, wakeSize, space) }
        return (0..2 * steps)
            .asSequence()
            .flatMap { total ->
                splitsOf(total, steps).asSequence().map { i -> bedtimeBoxes[i] to wakeBoxes[total - i] }
            }.firstOrNull { (b, w) -> fits(b, w, forward, space) }
            ?: (
                bedtime to
                    place(wakeDegrees, wakeSize, space, avoid = listOf(bedtime), awayFromDegrees = bedtimeDegrees)
            )
    }

    /** Partages d'un écartement [total] entre les deux étiquettes, du plus équilibré au moins équilibré. */
    private fun splitsOf(
        total: Int,
        steps: Int,
    ): List<Int> =
        (0..total)
            .filter { it <= steps && total - it <= steps }
            .sortedBy { abs(2 * it - total) }

    /** Deux étiquettes bien placées, disjointes, et dans l'ordre de leurs poignées. */
    private fun fits(
        bedtime: LabelBox,
        wake: LabelBox,
        forward: Float,
        space: DialLabelSpace,
    ): Boolean =
        listOf(bedtime, wake).all { box -> box.inside(space) && space.obstacles.none(box::intersects) } &&
            !bedtime.intersects(wake) &&
            signedDelta(centerAngle(bedtime, space), centerAngle(wake, space)) * forward > 0f

    private fun centerAngle(
        box: LabelBox,
        space: DialLabelSpace,
    ): Float =
        DialGeometry.degreesOfPoint(
            (box.left + box.right) / 2 - space.centerX,
            (box.top + box.bottom) / 2 - space.centerY,
        )

    /**
     * Boîte ancrée sur l'angle [degrees], au plus près de l'anneau sans le toucher : le rayon
     * d'ancrage est cherché par dichotomie (la boîte s'éloigne du centre quand il grandit).
     */
    private fun boxAt(
        degrees: Float,
        size: Pair<Float, Float>,
        space: DialLabelSpace,
    ): LabelBox {
        val radians = Math.toRadians(degrees.toDouble())
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()

        fun boxAtRadius(radius: Float) =
            boxFor(space.centerX + radius * dx, space.centerY + radius * dy, dx, size.first, size.second)
        var low = space.clearRadius
        var high = space.clearRadius + MAX_EXTRA_RADIUS
        if (boxAtRadius(low).distanceFrom(space.centerX, space.centerY) >= space.clearRadius) return boxAtRadius(low)
        repeat(RADIUS_SEARCH_STEPS) {
            val middle = (low + high) / 2
            if (boxAtRadius(middle).distanceFrom(space.centerX, space.centerY) >=
                space.clearRadius
            ) {
                high = middle
            } else {
                low = middle
            }
        }
        return boxAtRadius(high)
    }

    private fun boxFor(
        anchorX: Float,
        anchorY: Float,
        horizontal: Float,
        width: Float,
        height: Float,
    ): LabelBox {
        val left =
            when {
                abs(horizontal) < CENTERED_SINE -> anchorX - width / 2
                horizontal < 0 -> anchorX - width
                else -> anchorX
            }
        val top = anchorY - height / 2
        return LabelBox(left, top, left + width, top + height)
    }

    private fun clamped(
        box: LabelBox,
        space: DialLabelSpace,
    ): LabelBox {
        val width = box.right - box.left
        val height = box.bottom - box.top
        val left = box.left.coerceIn(0f, (space.width - width).coerceAtLeast(0f))
        val top = box.top.coerceIn(0f, (space.height - height).coerceAtLeast(0f))
        return LabelBox(left, top, left + width, top + height)
    }

    /** Écart signé le plus court de [from] à [to], en degrés. */
    private fun signedDelta(
        from: Float,
        to: Float,
    ): Float {
        var delta = (to - from) % FULL_TURN
        if (delta > HALF_TURN) delta -= FULL_TURN
        if (delta < -HALF_TURN) delta += FULL_TURN
        return delta
    }
}
