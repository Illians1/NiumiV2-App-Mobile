package com.niumi.designsystem.drawing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Cadran de 24 heures de l'accueil (VALEURS-DE-DESIGN.md §5) : une heure vaut 15°, minuit en haut,
 * sens horaire. Décisions de Mehdi du 2026-10-25 : glisser une poignée avance de 5 en 5 minutes ; une
 * minute d'écart au moins ; deux heures identiques forment une période de 24 h.
 */
class DialGeometryTest {
    private fun hm(
        hours: Int,
        minutes: Int,
    ) = hours * 60 + minutes

    @Test
    fun midnightIsAtTheTopAndAnHourIsFifteenDegrees() {
        assertThat(DialGeometry.degreesOf(0)).isEqualTo(0f)
        assertThat(DialGeometry.degreesOf(hm(6, 0))).isEqualTo(90f)
        assertThat(DialGeometry.degreesOf(hm(22, 30))).isEqualTo(337.5f)
    }

    @Test
    fun aTouchIsReadClockwiseFromTheTop() {
        assertThat(DialGeometry.degreesOfPoint(dx = 0f, dy = -1f)).isEqualTo(0f)
        assertThat(DialGeometry.degreesOfPoint(dx = 1f, dy = 0f)).isEqualTo(90f)
        assertThat(DialGeometry.degreesOfPoint(dx = 0f, dy = 1f)).isEqualTo(180f)
        assertThat(DialGeometry.degreesOfPoint(dx = -1f, dy = 0f)).isEqualTo(270f)
    }

    @Test
    fun crossingMidnightIsTheShortestMovement() {
        assertThat(DialGeometry.signedDeltaMinutes(fromDegrees = 350f, toDegrees = 10f)).isEqualTo(80f)
        assertThat(DialGeometry.signedDeltaMinutes(fromDegrees = 10f, toDegrees = 350f)).isEqualTo(-80f)
    }

    @Test
    fun thePeriodRunsFromBedtimeToWakeAndEqualTimesMakeTwentyFourHours() {
        assertThat(DialGeometry.periodMinutes(bedtime = hm(22, 30), wake = hm(7, 0))).isEqualTo(510)
        assertThat(DialGeometry.periodMinutes(bedtime = hm(1, 0), wake = hm(7, 0))).isEqualTo(360)
        assertThat(DialGeometry.periodMinutes(bedtime = hm(7, 0), wake = hm(7, 0))).isEqualTo(1440)
    }

    @Test
    fun aDraggedHandleMovesByFiveMinutes() {
        val wake = hm(7, 0)
        val bedtime = hm(22, 30)

        assertThat(DialGeometry.dragged(DialHandle.WAKE, wake, bedtime, deltaMinutes = 7f)).isEqualTo(hm(7, 5))
        assertThat(DialGeometry.dragged(DialHandle.WAKE, wake, bedtime, deltaMinutes = 2f)).isEqualTo(wake)
        assertThat(DialGeometry.dragged(DialHandle.BEDTIME, bedtime, wake, deltaMinutes = -14f)).isEqualTo(hm(22, 15))
    }

    @Test
    fun aHandleChosenToTheMinuteFallsBackOnTheGridWhenDragged() {
        assertThat(
            DialGeometry.dragged(DialHandle.BEDTIME, hm(22, 31), hm(7, 0), deltaMinutes = 3f),
        ).isEqualTo(hm(22, 35))
    }

    @Test
    fun theDraggedHandleStopsJustBeforeTheOtherOnTheNightSide() {
        // Le coucher avance vers le réveil : il s'arrête au dernier cran avant lui.
        assertThat(
            DialGeometry.dragged(DialHandle.BEDTIME, hm(22, 30), hm(7, 0), deltaMinutes = 600f),
        ).isEqualTo(hm(6, 55))
        // Le réveil recule vers le coucher : il s'arrête au premier cran après lui.
        assertThat(
            DialGeometry.dragged(DialHandle.WAKE, hm(7, 0), hm(22, 30), deltaMinutes = -600f),
        ).isEqualTo(hm(22, 35))
    }

    @Test
    fun theDraggedHandleReachesTheOtherFromTheDaySideForTwentyFourHours() {
        // Le coucher recule jusqu'à rejoindre le réveil par l'autre côté : 24 h, jamais au-delà.
        assertThat(
            DialGeometry.dragged(DialHandle.BEDTIME, hm(22, 30), hm(7, 0), deltaMinutes = -900f),
        ).isEqualTo(hm(7, 30))
        assertThat(
            DialGeometry.dragged(DialHandle.BEDTIME, hm(22, 30), hm(7, 0), deltaMinutes = -960f),
        ).isEqualTo(hm(7, 0))
        assertThat(
            DialGeometry.dragged(DialHandle.WAKE, hm(7, 0), hm(22, 30), deltaMinutes = 1000f),
        ).isEqualTo(hm(22, 30))
    }

    /** Relecture : seul un toucher immobile ouvre le sélecteur ; un glissé hors des poignées ne fait rien. */
    @Test
    fun onlyAStillTouchOpensThePicker() {
        assertThat(
            DialGeometry.tapTarget(grabbed = DialHandle.BEDTIME, travelled = 2f, touchSlop = 8f),
        ).isEqualTo(DialHandle.BEDTIME)
        assertThat(DialGeometry.tapTarget(grabbed = null, travelled = 2f, touchSlop = 8f)).isEqualTo(DialHandle.WAKE)
        assertThat(
            DialGeometry.tapTarget(grabbed = DialHandle.WAKE, travelled = 2f, touchSlop = 8f),
        ).isEqualTo(DialHandle.WAKE)
        assertThat(DialGeometry.tapTarget(grabbed = null, travelled = 30f, touchSlop = 8f)).isNull()
    }

    @Test
    fun theElapsedNightIsMeasuredFromBedtime() {
        assertThat(DialGeometry.elapsedMinutes(bedtime = hm(22, 30), now = hm(23, 10))).isEqualTo(40)
        assertThat(DialGeometry.elapsedMinutes(bedtime = hm(22, 30), now = hm(1, 30))).isEqualTo(180)
    }

    /** Essai du 2026-10-25 : le point de l'heure actuelle doit se voir même sur l'arc de la nuit. */
    @Test
    fun theCurrentTimeKnowsWhetherItIsWithinTheNight() {
        assertThat(DialGeometry.isWithinPeriod(bedtime = hm(22, 30), wake = hm(7, 0), now = hm(23, 10))).isTrue()
        assertThat(DialGeometry.isWithinPeriod(bedtime = hm(22, 30), wake = hm(7, 0), now = hm(3, 0))).isTrue()
        assertThat(DialGeometry.isWithinPeriod(bedtime = hm(22, 30), wake = hm(7, 0), now = hm(22, 30))).isTrue()
        assertThat(DialGeometry.isWithinPeriod(bedtime = hm(22, 30), wake = hm(7, 0), now = hm(7, 0))).isFalse()
        assertThat(DialGeometry.isWithinPeriod(bedtime = hm(22, 30), wake = hm(7, 0), now = hm(12, 0))).isFalse()
        // Période de 24 h : toujours dedans.
        assertThat(DialGeometry.isWithinPeriod(bedtime = hm(7, 0), wake = hm(7, 0), now = hm(12, 0))).isTrue()
    }

    /**
     * Relecture du 2026-10-25 : un seul dégradé sur un tour ne tenait pas au-delà de 338° écoulés
     * (période de 24 h). L'ambre se découpe en une partie pleine et un fondu qui commence au point,
     * chacun dessiné à part : ensemble, ils ne dépassent jamais la période.
     */
    @Test
    fun theAmberIsSplitIntoASolidPartAndAFadeStartingAtTheDot() {
        assertThat(
            AmberSpans.of(elapsedDegrees = 40f, sweepDegrees = 128f),
        ).isEqualTo(AmberSpans(solid = 40f, fade = 12f))
    }

    @Test
    fun theFadeIsShortenedNearTheEndOfTheNight() {
        assertThat(
            AmberSpans.of(elapsedDegrees = 120f, sweepDegrees = 128f),
        ).isEqualTo(AmberSpans(solid = 120f, fade = 8f))
    }

    @Test
    fun aTwentyFourHourNightKeepsItsFadeUntilTheLastMinutes() {
        // 23 h 20 écoulées sur 24 h : le fondu entier tenait mal dans l'ancien dégradé unique.
        assertThat(
            AmberSpans.of(elapsedDegrees = 345f, sweepDegrees = 360f),
        ).isEqualTo(AmberSpans(solid = 345f, fade = 12f))
        assertThat(
            AmberSpans.of(elapsedDegrees = 355f, sweepDegrees = 360f),
        ).isEqualTo(AmberSpans(solid = 355f, fade = 5f))
    }

    @Test
    fun theAmberNeverGoesBeyondThePeriod() {
        assertThat(
            AmberSpans.of(elapsedDegrees = 200f, sweepDegrees = 128f),
        ).isEqualTo(AmberSpans(solid = 128f, fade = 0f))
        assertThat(
            AmberSpans.of(elapsedDegrees = -5f, sweepDegrees = 128f),
        ).isEqualTo(AmberSpans(solid = 0f, fade = 12f))
    }

    /** Décision du 2026-10-26 : un repère s'efface quand la flèche de l'heure actuelle est à moins de 4° (16 min). */
    @Test
    fun aQuarterMarkHidesUnderTheCurrentTimeArrow() {
        assertThat(DialMarks.QUARTER_MARKS).containsExactly(0, hm(6, 0), hm(12, 0), hm(18, 0)).inOrder()
        assertThat(DialMarks.isHidden(markMinutes = 0, nowMinutes = 0)).isTrue()
        assertThat(DialMarks.isHidden(markMinutes = 0, nowMinutes = 15)).isTrue()
        assertThat(DialMarks.isHidden(markMinutes = 0, nowMinutes = 16)).isFalse()
        // Passer minuit n'est pas un tour complet : 23:45 est à 15 minutes du repère du haut.
        assertThat(DialMarks.isHidden(markMinutes = 0, nowMinutes = hm(23, 45))).isTrue()
        assertThat(DialMarks.isHidden(markMinutes = hm(6, 0), nowMinutes = hm(6, 10))).isTrue()
        assertThat(DialMarks.isHidden(markMinutes = hm(6, 0), nowMinutes = 0)).isFalse()
    }

    /** Décision du 2026-10-25 : fondu court (12°) et franc dès le point, au lieu du demi-cosinus sur 34°. */
    @Test
    fun theAmberFadesOutStraightOverTwelveDegreesFromTheDot() {
        assertThat(DialGeometry.fadeAlpha(degreesPastNow = -5f)).isEqualTo(1f)
        assertThat(DialGeometry.fadeAlpha(degreesPastNow = 0f)).isEqualTo(1f)
        assertThat(DialGeometry.fadeAlpha(degreesPastNow = 3f)).isWithin(1e-6f).of(0.75f)
        assertThat(DialGeometry.fadeAlpha(degreesPastNow = 6f)).isWithin(1e-6f).of(0.5f)
        assertThat(DialGeometry.fadeAlpha(degreesPastNow = 12f)).isWithin(1e-6f).of(0f)
        assertThat(DialGeometry.fadeAlpha(degreesPastNow = 40f)).isEqualTo(0f)
    }
}
