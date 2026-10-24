package com.niumi.system.insight

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Durée déclarée pour sortir du lit (`design-plan/SPECIFICATION.md` §7, étape 43) : onze valeurs,
 * « 1 heure et + » comptant pour 60 (D3). Aucune valeur présélectionnée n'est décidée (D4, étape 50) :
 * rien d'écrit se lit « pas de durée », jamais un défaut inventé.
 */
class TimeToGetUpPreferencesTest {
    @Test
    fun theElevenAllowedDurationsAreThoseOfTheSpecification() {
        assertThat(DeclaredMinutes.ALLOWED).containsExactly(1, 2, 3, 4, 5, 10, 15, 20, 30, 45, 60).inOrder()
    }

    @Test
    fun nothingWrittenReadsAsNoDuration() {
        assertThat(decodeDeclaredMinutes(null)).isNull()
    }

    @Test
    fun everyAllowedDurationRoundTrips() {
        DeclaredMinutes.ALLOWED.forEach { minutes -> assertThat(decodeDeclaredMinutes(minutes)).isEqualTo(minutes) }
    }

    /** Une valeur stockée hors liste (liste resserrée plus tard, fichier altéré) n'est jamais propagée. */
    @Test
    fun aStoredValueOutsideTheListReadsAsNoDuration() {
        listOf(0, 6, 61, -1).forEach { minutes -> assertThat(decodeDeclaredMinutes(minutes)).isNull() }
    }
}
