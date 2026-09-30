package com.niumi.system.readiness

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import org.junit.Test

/** SPEC_ANDROID §15 (étape 25) : quel contrôle rejouer pour savoir si un incident est rétabli. */
class IncidentReadinessChecksTest {
    @Test
    fun everyMonitoredIncidentCodeMapsBackToItsOwnCheck() {
        MonitoredReadinessChecks.incidentCodes.forEach { (checkId, code) ->
            assertThat(IncidentReadinessChecks.checkFor(code)).isEqualTo(checkId)
        }
    }

    /** Produit hors de §13.1 (`SessionRuntimeReconciler`), mais le contrôle `NFC_ENABLED` le juge. */
    @Test
    fun theNfcIncidentIsJudgedByTheNfcCheck() {
        assertThat(IncidentReadinessChecks.checkFor(IncidentCodes.NFC_DISABLED))
            .isEqualTo(ReadinessCheckId.NFC_ENABLED)
    }

    /** Un fait passé ou un défaut interne n'a pas de contrôle : jamais présumé rétabli. */
    @Test
    fun aPastFactOrAnInternalDefectHasNoCheckToReplay() {
        listOf(
            IncidentCodes.TIME_CHANGED,
            IncidentCodes.TIMEZONE_CHANGED,
            IncidentCodes.MISSED_TRIGGER_WINDOW,
            IncidentCodes.MISSED_BLOCKING_START_WINDOW,
            IncidentCodes.PROCESS_RECREATED,
            IncidentCodes.RELEASE_PARTIAL_FAILURE,
            IncidentCodes.SNAPSHOT_CORRUPTED,
        ).forEach { code -> assertThat(IncidentReadinessChecks.checkFor(code)).isNull() }
    }

    /** Même périmètre que la remédiation : ce qui se répare depuis un réglage peut aussi se rétablir. */
    @Test
    fun resolvableCodesAreExactlyTheRemediableOnes() {
        val remediable = MonitoredReadinessChecks.incidentCodes.values + IncidentCodes.NFC_DISABLED
        remediable.forEach { code ->
            assertThat(IncidentRemediation.actionFor(code)).isNotNull()
            assertThat(IncidentReadinessChecks.checkFor(code)).isNotNull()
        }
    }
}
