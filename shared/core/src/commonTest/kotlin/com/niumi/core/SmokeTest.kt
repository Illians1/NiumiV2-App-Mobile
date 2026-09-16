package com.niumi.core

import com.niumi.core.domain.NiumiCoreVersion
import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun schemaVersionIsTwo() {
        // Contrat 1.3 : les DTO exposés changent de façon incompatible pour Swift (blocage différé).
        assertEquals(2, NiumiCoreVersion.SCHEMA_VERSION)
    }
}
