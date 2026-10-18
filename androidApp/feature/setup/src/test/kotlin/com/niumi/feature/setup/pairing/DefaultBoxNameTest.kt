package com.niumi.feature.setup.pairing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** SPEC_ANDROID §15 (Lot 8, décision du 2026-10-17) : « Boîtier {n} », n le plus petit entier libre. */
class DefaultBoxNameTest {
    @Test
    fun withoutAnyBoxTheProposalIsBoitier1() {
        assertThat(DefaultBoxName.next(emptyList())).isEqualTo("Boîtier 1")
    }

    @Test
    fun theProposalFollowsTheNamesAlreadyTaken() {
        assertThat(DefaultBoxName.next(listOf("Boîtier 1", "Boîtier 2"))).isEqualTo("Boîtier 3")
    }

    /** Le cas qui a motivé la règle : « Boîtier 2 » retiré parmi trois, jamais un second « Boîtier 3 ». */
    @Test
    fun aGapLeftByARemovalIsFilledFirst() {
        assertThat(DefaultBoxName.next(listOf("Boîtier 1", "Boîtier 3"))).isEqualTo("Boîtier 2")
    }

    @Test
    fun freeNamesDoNotTakeANumber() {
        assertThat(DefaultBoxName.next(listOf("Cuisine", "Salle de bain"))).isEqualTo("Boîtier 1")
    }
}
