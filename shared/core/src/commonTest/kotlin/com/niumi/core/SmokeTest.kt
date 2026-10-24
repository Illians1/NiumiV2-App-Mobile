package com.niumi.core

import com.niumi.core.domain.NiumiCoreVersion
import com.niumi.core.domain.SessionSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun schemaVersionIsThree() {
        // Contrat 1.5 : les DTO exposés changent de façon incompatible pour Swift (désarmement,
        // moyen de libération, prochaine occurrence d'un planning).
        assertEquals(3, NiumiCoreVersion.SCHEMA_VERSION)
        assertEquals(3, SessionSnapshot.SCHEMA_VERSION)
    }
}
