package com.niumi.designsystem.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * Aspect d'une rangée de roue selon sa distance au milieu (demande de Mehdi du 2026-10-31) : il passe
 * en continu du choisi (34) au voisin (22) puis à l'éloigné (18), sans saut pendant le défilement.
 */
class NiumiWheelRowLookTest {
    @Test
    fun theMiddleRowIsFullySelected() {
        val selected = WheelRowLook(34f, selectedAlpha = 1f, nearAlpha = 0f, farAlpha = 0f)
        assertThat(wheelRowLook(0f)).isEqualTo(selected)
    }

    @Test
    fun oneRowAwayIsFullyNear() {
        val near = WheelRowLook(22f, selectedAlpha = 0f, nearAlpha = 1f, farAlpha = 0f)
        assertThat(wheelRowLook(1f)).isEqualTo(near)
    }

    @Test
    fun halfwayBlendsSelectedAndNear() {
        val halfway = WheelRowLook(28f, selectedAlpha = 0.5f, nearAlpha = 0.5f, farAlpha = 0f)
        assertThat(wheelRowLook(0.5f)).isEqualTo(halfway)
    }

    @Test
    fun twoRowsAwayAndBeyondAreFar() {
        val far = WheelRowLook(18f, selectedAlpha = 0f, nearAlpha = 0f, farAlpha = 1f)
        assertThat(wheelRowLook(2f)).isEqualTo(far)
        assertThat(wheelRowLook(5f)).isEqualTo(far)
    }

    /** Pas de saut : un petit pas de défilement ne change l'aspect que d'un peu, et les parts font 1. */
    @Test
    fun theLookChangesContinuously() {
        val steps = (0..200).map { wheelRowLook(it / 100f) }
        steps.zipWithNext().forEach { (a, b) ->
            assertThat(abs(a.fontSize - b.fontSize)).isLessThan(0.2f)
            assertThat(abs(a.selectedAlpha - b.selectedAlpha)).isLessThan(0.02f)
        }
        steps.forEach { assertThat(it.selectedAlpha + it.nearAlpha + it.farAlpha).isWithin(1e-5f).of(1f) }
    }
}
