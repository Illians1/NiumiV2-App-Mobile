package com.niumi.designsystem.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Emplacements d'icônes d'applications (`design-plan/SPECIFICATION.md` §8, décisions du 2026-10-22) :
 * six au plus ; au-delà, cinq icônes puis « +N » ; sur un écran trop étroit, moins d'emplacements,
 * avec la même règle. Partagés depuis l'étape 45 par l'écran 4 et l'accueil.
 */
class NiumiAppSlotsTest {
    @Test
    fun asManySlotsAsFitUpToSix() {
        assertThat(appSlotCapacity(width = 198f, slot = 28f, gap = 6f)).isEqualTo(6)
        assertThat(appSlotCapacity(width = 1000f, slot = 28f, gap = 6f)).isEqualTo(6)
        assertThat(appSlotCapacity(width = 129f, slot = 28f, gap = 6f)).isEqualTo(3)
        assertThat(appSlotCapacity(width = 10f, slot = 28f, gap = 6f)).isEqualTo(1)
    }

    @Test
    fun everyIconIsShownWhenTheyFit() {
        assertThat(appSlotLayout(count = 4, capacity = 6)).isEqualTo(AppSlotLayout(shown = 4, hidden = 0))
        assertThat(appSlotLayout(count = 6, capacity = 6)).isEqualTo(AppSlotLayout(shown = 6, hidden = 0))
    }

    @Test
    fun theLastSlotCountsTheIconsThatDoNotFit() {
        assertThat(appSlotLayout(count = 9, capacity = 6)).isEqualTo(AppSlotLayout(shown = 5, hidden = 4))
        assertThat(appSlotLayout(count = 2, capacity = 1)).isEqualTo(AppSlotLayout(shown = 0, hidden = 2))
    }
}
