package com.niumi.core

import com.niumi.core.domain.NiumiCoreVersion
import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun schemaVersionIsOne() {
        assertEquals(1, NiumiCoreVersion.SCHEMA_VERSION)
    }
}
