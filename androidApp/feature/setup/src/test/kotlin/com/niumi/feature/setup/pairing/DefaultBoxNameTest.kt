package com.niumi.feature.setup.pairing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** SPEC_ANDROID §15 (Lot 8, décision du 2026-10-17) : « Niumi Spot {n} », n le plus petit entier libre. */
class DefaultBoxNameTest {
    @Test
    fun withoutAnyBoxTheProposalIsNiumiSpot1() {
        assertThat(DefaultBoxName.next(emptyList())).isEqualTo("Niumi Spot 1")
    }

    @Test
    fun theProposalFollowsTheNamesAlreadyTaken() {
        assertThat(DefaultBoxName.next(listOf("Niumi Spot 1", "Niumi Spot 2"))).isEqualTo("Niumi Spot 3")
    }

    /** Le cas qui a motivé la règle : « Niumi Spot 2 » retiré parmi trois, jamais un second « Niumi Spot 3 ». */
    @Test
    fun aGapLeftByARemovalIsFilledFirst() {
        assertThat(DefaultBoxName.next(listOf("Niumi Spot 1", "Niumi Spot 3"))).isEqualTo("Niumi Spot 2")
    }

    @Test
    fun freeNamesDoNotTakeANumber() {
        assertThat(DefaultBoxName.next(listOf("Cuisine", "Salle de bain"))).isEqualTo("Niumi Spot 1")
    }
}
