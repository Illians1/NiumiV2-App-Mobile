package com.niumi.designsystem.drawing

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.niumi.designsystem.ui.theme.NiumiDimens
import org.junit.Test

/**
 * Étiquettes « Coucher 22:30 » et « Réveil 7:00 » du cadran (essai sur appareil du 2026-10-25,
 * décision de Mehdi) : jamais sur l'anneau, jamais l'une sur l'autre, toujours dans la zone.
 * Mesures en dp, densité 1 : zone de 366 × 340, centre (183, 170), anneau de rayon 112 et de trait
 * 28, écart de 4. L'icône des réglages, que les étiquettes évitaient, a quitté l'accueil pour la
 * barre d'onglets le 2026-10-25 (décision de Mehdi).
 */
class DialLabelLayoutTest {
    private val space =
        DialLabelSpace(
            width = 366f,
            height = NiumiDimens.dialHeight.value,
            centerX = 183f,
            centerY = NiumiDimens.dialCenterY.value,
            clearRadius = 112f + 14f + NiumiDimens.dialLabelGap.value,
        )

    private fun angleOf(minutes: Int) = DialGeometry.degreesOf(minutes)

    private fun assertWellPlaced(
        label: String,
        box: LabelBox,
    ) {
        assertWithMessage("$label dans la zone").that(box.left >= 0f && box.top >= 0f).isTrue()
        assertWithMessage("$label dans la zone").that(box.right <= space.width && box.bottom <= space.height).isTrue()
        assertWithMessage(
            "$label hors de l'anneau",
        ).that(box.distanceFrom(space.centerX, space.centerY)).isAtLeast(space.clearRadius - 0.5f)
    }

    @Test
    fun aLabelAtNoonSitsEntirelyBelowTheRing() {
        val box = DialLabelLayout.place(angleOf(12 * 60), WIDTH to HEIGHT, space)

        assertWellPlaced("12:00", box)
        assertThat(box.top).isAtLeast(space.centerY + space.clearRadius - 0.5f)
    }

    @Test
    fun aLabelAtMidnightSitsEntirelyAboveTheRing() {
        val box = DialLabelLayout.place(angleOf(0), WIDTH to HEIGHT, space)

        assertWellPlaced("0:00", box)
        assertThat(box.bottom).isAtMost(space.centerY - space.clearRadius + 0.5f)
    }

    @Test
    fun twoCloseHandlesNeverHaveOverlappingLabels() {
        val (bedtime, wake) =
            DialLabelLayout.placeBoth(
                angleOf(22 * 60 + 30),
                WIDTH to HEIGHT,
                angleOf(22 * 60 + 40),
                WIDTH to HEIGHT,
                space,
            )

        assertWellPlaced("coucher", bedtime)
        assertWellPlaced("réveil", wake)
        assertThat(bedtime.intersects(wake)).isFalse()
    }

    @Test
    fun everyTimeOfDayGetsAWellPlacedLabel() {
        for (minutes in 0 until 24 * 60 step 5) {
            assertWellPlaced(
                "${minutes / 60}:${minutes % 60}",
                DialLabelLayout.place(angleOf(minutes), WIDTH to HEIGHT, space),
            )
        }
    }

    @Test
    fun everyPairOfHandlesFiveMinutesApartGetsDisjointLabels() {
        for (minutes in 0 until 24 * 60 step 15) {
            val (bedtime, wake) =
                DialLabelLayout.placeBoth(
                    angleOf(minutes),
                    WIDTH to HEIGHT,
                    angleOf(minutes + 5),
                    WIDTH to HEIGHT,
                    space,
                )
            assertWellPlaced("coucher $minutes", bedtime)
            assertWellPlaced("réveil $minutes", wake)
            assertWithMessage("étiquettes disjointes à $minutes").that(bedtime.intersects(wake)).isFalse()
        }
    }

    /**
     * Essai sur appareil du 2026-10-25 : quand les poignées sont très proches, l'étiquette du coucher
     * passait parfois devant celle du réveil. Dans le sens de l'horloge, l'étiquette du coucher reste
     * toujours avant celle du réveil, comme leurs poignées.
     */
    @Test
    fun closeLabelsKeepTheOrderOfTheirHandles() {
        for (gap in listOf(1, 5, 10)) {
            for (minutes in 0 until 24 * 60 step 5) {
                val (bedtime, wake) =
                    DialLabelLayout.placeBoth(
                        angleOf(minutes),
                        WIDTH to HEIGHT,
                        angleOf(minutes + gap),
                        WIDTH to HEIGHT,
                        space,
                    )
                assertWellPlaced("coucher $minutes+$gap", bedtime)
                assertWellPlaced("réveil $minutes+$gap", wake)
                assertWithMessage(
                    "ordre à $minutes, écart $gap",
                ).that(DialGeometry.signedDeltaMinutes(angleOfBox(bedtime), angleOfBox(wake))).isGreaterThan(0f)
            }
        }
    }

    private fun angleOfBox(box: LabelBox) =
        DialGeometry.degreesOfPoint(
            (box.left + box.right) / 2 - space.centerX,
            (box.top + box.bottom) / 2 - space.centerY,
        )

    /**
     * Dans la carte de 342 (décision de Mehdi du 2026-10-30) : l'anneau se réduit juste assez pour
     * qu'une étiquette de 50 tienne à côté de lui, entre l'écart de 4 et 4 sur un côté, et la marge
     * de 6 au bord.
     */
    @Test
    fun theRingShrinksJustEnoughForALabelBesideIt() {
        val scale = cardScale()

        assertThat(scale).isWithin(0.001f).of((CARD / 2 - WIDTH - GAP - SIDE_GAP - MARGIN) / RING_OUTER)
        assertThat(
            DialLabelLayout.ringScale(CARD, 1f, RING_OUTER, GAP + SIDE_GAP, MARGIN, NiumiDimens.DIAL_MIN_RING_SCALE),
        ).isEqualTo(1f)
        assertThat(
            DialLabelLayout.ringScale(200f, WIDTH, RING_OUTER, GAP + SIDE_GAP, MARGIN, NiumiDimens.DIAL_MIN_RING_SCALE),
        ).isEqualTo(NiumiDimens.DIAL_MIN_RING_SCALE)
    }

    /**
     * Essais sur appareil du 2026-10-30 : une poignée sur un côté, dans la carte, laissait son
     * étiquette glisser contre elle et contre le bord ; à 18 h pile, l'étiquette semblait collée à la
     * poignée. Elle se pose maintenant à côté, à sa hauteur, 4 plus loin qu'au-dessus ou au-dessous.
     */
    @Test
    fun inTheCardALabelOnASideSitsBesideItsHandle() {
        val scale = cardScale()
        val centerY = NiumiDimens.dialCenterYFor(scale).value
        val card =
            DialLabelSpace(
                width = CARD,
                height = centerY * 2,
                centerX = CARD / 2,
                centerY = centerY,
                clearRadius = RING_OUTER * scale + GAP,
                horizontalMargin = MARGIN,
                sideGap = SIDE_GAP,
            )

        val left = DialLabelLayout.place(angleOf(18 * 60), WIDTH to HEIGHT, card)
        val right = DialLabelLayout.place(angleOf(6 * 60), WIDTH to HEIGHT, card)

        assertThat(left.right).isWithin(0.5f).of(card.centerX - card.clearRadius - SIDE_GAP)
        assertThat(left.left).isAtLeast(MARGIN - 0.5f)
        assertThat((left.top + left.bottom) / 2).isWithin(0.5f).of(centerY)
        assertThat(right.left).isWithin(0.5f).of(card.centerX + card.clearRadius + SIDE_GAP)
        assertThat(right.right).isAtMost(CARD - MARGIN + 0.5f)
        assertThat((right.top + right.bottom) / 2).isWithin(0.5f).of(centerY)
    }

    private fun cardScale() =
        DialLabelLayout.ringScale(CARD, WIDTH, RING_OUTER, GAP + SIDE_GAP, MARGIN, NiumiDimens.DIAL_MIN_RING_SCALE)

    private companion object {
        const val WIDTH = 50f
        const val HEIGHT = 40f
        const val CARD = 342f
        const val RING_OUTER = 126f
        const val GAP = 4f
        const val MARGIN = 6f
        const val SIDE_GAP = 4f
    }
}
