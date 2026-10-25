package com.niumi.designsystem.ui.theme

import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.drawing.DialGeometry
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/** Cadran de l'accueil (VALEURS-DE-DESIGN.md §5, `valeurs-de-design.json` → `cadran`, étape 45). */
class NiumiDialDimensTest {
    private val dial = DesignValues.obj("cadran")
    private val mockup = dial.getValue("dansLesMaquettes").jsonObject

    private fun mockupNumber(key: String): Float =
        mockup
            .getValue(key)
            .jsonPrimitive.content
            .toFloat()

    private fun mockupPair(key: String): List<Float> =
        mockup.getValue(key).jsonArray.map {
            it.jsonPrimitive.content.toFloat()
        }

    @Test
    fun theWidthTheRadiusAndTheStrokeAreThoseOfTheMockups() {
        assertThat(NiumiDimens.dialWidth.value).isEqualTo(mockupPair("zone")[0])
        assertThat(NiumiDimens.dialWidth.value / 2).isEqualTo(mockupPair("centre")[0])
        assertThat(NiumiDimens.dialRadius.value).isEqualTo(mockupNumber("rayon"))
        assertThat(NiumiDimens.dialStroke.value).isEqualTo(mockupNumber("epaisseur"))
    }

    /**
     * Écart décidé le 2026-10-25 après essai : 40 au-dessus et au-dessous de l'anneau pour les
     * étiquettes, plus 4 d'écart (44 et 6 au premier essai, jugés trop larges), là où les maquettes
     * laissent 32 et 6 (zone de 290, centre à 158).
     */
    @Test
    fun theZoneKeepsALabelBandAboveAndBelowTheRing() {
        val ringOuter = NiumiDimens.dialRadius.value + NiumiDimens.dialStroke.value / 2

        assertThat(NiumiDimens.dialCenterY.value).isEqualTo(NiumiDimens.dialHeight.value / 2)
        assertThat(
            NiumiDimens.dialCenterY.value - ringOuter,
        ).isEqualTo(NiumiDimens.dialLabelGap.value + NiumiDimens.dialLabelBand.value)
        assertThat(NiumiDimens.dialHeight.value).isEqualTo(340f)
    }

    @Test
    fun anHourIsFifteenDegreesAndTheFadeLastsThirtyFourDegrees() {
        assertThat(
            DialGeometry.degreesOf(60),
        ).isEqualTo(
            dial
                .getValue("degresParHeure")
                .jsonPrimitive.content
                .toFloat(),
        )
        val fade =
            dial
                .getValue("session")
                .jsonObject
                .getValue("fonduDevantLePoint")
                .jsonObject
        assertThat(DialGeometry.FADE_DEGREES).isEqualTo(
            fade
                .getValue("degres")
                .jsonPrimitive.content
                .toFloat(),
        )
    }

    @Test
    fun theCurrentTimeDotHasARadiusOfFour() {
        val dot =
            dial
                .getValue("session")
                .jsonObject
                .getValue("pointHeureActuelle")
                .jsonObject
        assertThat(NiumiDimens.dialNowDotRadius.value).isEqualTo(
            dot
                .getValue("rayon")
                .jsonPrimitive.content
                .toFloat(),
        )
    }
}
