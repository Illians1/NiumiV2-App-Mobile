package com.niumi.database.pairing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §11.1 : un nom de boîtier fait 1 à 30 caractères après `trim()`, un nom vide ou
 * blanc est refusé (Lot 8).
 */
class PairedBoxNameTest {
    @Test
    fun `surrounding whitespace is trimmed`() {
        assertThat(PairedBoxName.sanitize(" Cuisine ")).isEqualTo("Cuisine")
    }

    @Test
    fun `an empty name is refused`() {
        assertThat(PairedBoxName.sanitize("")).isNull()
    }

    @Test
    fun `a blank name is refused`() {
        assertThat(PairedBoxName.sanitize("   ")).isNull()
    }

    @Test
    fun `thirty characters are kept`() {
        val thirtyChars = "a".repeat(PairedBoxName.MAX_LENGTH)

        assertThat(PairedBoxName.sanitize(thirtyChars)).isEqualTo(thirtyChars)
    }

    @Test
    fun `thirty one characters are refused`() {
        assertThat(PairedBoxName.sanitize("a".repeat(PairedBoxName.MAX_LENGTH + 1))).isNull()
    }

    @Test
    fun `an already clean name is unchanged`() {
        assertThat(PairedBoxName.sanitize("Salle de bain")).isEqualTo("Salle de bain")
    }
}
