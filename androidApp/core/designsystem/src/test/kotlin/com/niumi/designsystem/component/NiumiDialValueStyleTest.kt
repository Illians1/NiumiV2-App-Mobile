package com.niumi.designsystem.component

import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.ui.theme.NiumiType
import org.junit.Test

/** Valeur du centre du cadran réduite avec l'anneau (essai de Mehdi du 2026-10-30). */
class NiumiDialValueStyleTest {
    @Test
    fun aFullRingKeepsTheSizeOfTheDesign() {
        val style = dialValueStyle(NiumiType.dial42, ringScale = 1f)

        assertThat(style.fontSize).isEqualTo(42.sp)
        assertThat(style.letterSpacing).isEqualTo((-1).sp)
    }

    @Test
    fun aReducedRingReducesTheValueInTheSameProportion() {
        val style = dialValueStyle(NiumiType.dial42, ringScale = 0.75f)

        assertThat(style.fontSize.value).isWithin(1e-4f).of(31.5f)
        assertThat(style.letterSpacing.value).isWithin(1e-4f).of(-0.75f)
        assertThat(style.fontWeight).isEqualTo(NiumiType.dial42.fontWeight)
    }
}
