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

    private companion object {
        const val WIDTH = 50f
        const val HEIGHT = 40f
    }
}
