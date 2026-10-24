package com.niumi.feature.session.diagnostics

import com.google.common.truth.Truth.assertThat
import com.niumi.system.readiness.ReadinessCheckId
import org.junit.Test

/** SPEC_ANDROID §15 (Lot 8) : l'écran 12 nomme le contrôle comme l'écran 2, au pluriel. */
class IncidentDiagnosticTextsTest {
    @Test
    fun thePairedBoxCheckIsNamedInThePlural() {
        assertThat(IncidentDiagnosticTexts.checkLabel(ReadinessCheckId.PAIRED_BOX)).isEqualTo("Niumi Spots associés")
    }
}
