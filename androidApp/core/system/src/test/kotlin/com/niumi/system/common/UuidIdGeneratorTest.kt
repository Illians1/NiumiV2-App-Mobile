package com.niumi.system.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UuidIdGeneratorTest {
    private val generator = UuidIdGenerator()

    @Test
    fun newIdIsCanonicalLowercaseUuidV4() {
        val id = generator.newId()

        assertThat(id).matches(
            "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}",
        )
    }

    @Test
    fun successiveCallsProduceDistinctIds() {
        val first = generator.newId()
        val second = generator.newId()

        assertThat(first).isNotEqualTo(second)
    }
}
