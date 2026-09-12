package com.niumi.feature.session.active

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.readiness.AndroidIncidentCodes
import org.junit.Test

/**
 * Textes des écrans 7, 9 et 11 (SPEC_ANDROID §15). Deux règles s'y appliquent sans exception :
 * tutoiement partout, et jamais de faux état de fiabilité.
 */
class ActiveSessionTextsTest {
    private val allTexts =
        listOf(
            ActiveSessionTexts.TITLE,
            ActiveSessionTexts.NO_SESSION,
            ActiveSessionTexts.CURRENT_ZONE_TITLE,
            ActiveSessionTexts.COMMITMENT_REMINDER,
            ActiveSessionTexts.BLOCKED_APPS_TITLE,
            ActiveSessionTexts.NO_BLOCKED_APP,
            ActiveSessionTexts.HEALTH_TITLE,
            ActiveSessionTexts.HEALTHY,
            ActiveSessionTexts.DEGRADED,
            ActiveSessionTexts.INCIDENTS_TITLE,
            ActiveSessionTexts.CRITICAL_INCIDENTS_TITLE,
            ActiveSessionTexts.MODIFY_OR_CANCEL_BUTTON,
            ScanToModifyTexts.INVITATION,
            ScanToModifyTexts.TITLE,
            ScanToModifyTexts.UNKNOWN_BOX,
            ScanToModifyTexts.UNREADABLE,
            ScanToModifyTexts.RELEASING,
            CancelledTexts.TITLE,
            CancelledTexts.BODY,
            CancelledTexts.PREPARE_AGAIN_BUTTON,
        )

    @Test
    fun noTextUsesVouvoiement() {
        allTexts.forEach { text ->
            assertThat(text.lowercase()).doesNotContain("vous")
            assertThat(text.lowercase()).doesNotContain("votre")
        }
    }

    @Test
    fun everyLabelIsNotEmpty() {
        allTexts.forEach { text -> assertThat(text).isNotEmpty() }
    }

    /** Imposé par le plan de l'étape 15, mot pour mot. */
    @Test
    fun theScanInvitationIsStatedWordForWord() {
        assertThat(ScanToModifyTexts.INVITATION)
            .isEqualTo(
                "Scanne ton boîtier Niumi pour annuler ou modifier ta session. " +
                    "Tes applications resteront bloquées jusqu'au scan.",
            )
    }

    @Test
    fun theCancelledScreenAndItsButtonAreStatedWordForWord() {
        assertThat(CancelledTexts.TITLE).isEqualTo("Session annulée")
        assertThat(CancelledTexts.PREPARE_AGAIN_BUTTON).isEqualTo("Préparer un nouveau réveil")
    }

    @Test
    fun theModifyOrCancelButtonIsStatedWordForWord() {
        assertThat(ActiveSessionTexts.MODIFY_OR_CANCEL_BUTTON).isEqualTo("Modifier ou annuler")
    }

    @Test
    fun theCommitmentReminderIsStatedWordForWord() {
        assertThat(ActiveSessionTexts.COMMITMENT_REMINDER)
            .isEqualTo("Seul le scan du boîtier terminera la session.")
    }

    /** §15 : un état dégradé ne doit pas promettre un retour à la normale qui n'arrivera pas. */
    @Test
    fun theDegradedTextPromisesNoRecovery() {
        assertThat(ActiveSessionTexts.DEGRADED.lowercase()).doesNotContain("rétabli")
        assertThat(ActiveSessionTexts.DEGRADED.lowercase()).doesNotContain("bientôt")
    }

    @Test
    fun everySessionStateHasALabel() {
        SessionStateDto.entries.forEach { state ->
            assertThat(ActiveSessionTexts.stateLabel(state)).isNotEmpty()
        }
    }

    @Test
    fun everySeverityHasALabel() {
        IncidentSeverityDto.entries.forEach { severity ->
            assertThat(ActiveSessionTexts.severityLabel(severity)).isNotEmpty()
        }
    }

    /**
     * Les dix codes que la surveillance de §13.1 et le réconciliateur peuvent produire pendant une
     * session doivent tous avoir une phrase : un code technique brut à l'écran ne dit rien à
     * l'utilisateur.
     */
    @Test
    fun everyIncidentCodeReachableDuringASessionIsTranslated() {
        val reachable =
            listOf(
                IncidentCodes.BLOCKING_PERMISSION_REVOKED,
                IncidentCodes.ALARM_PERMISSION_REVOKED,
                IncidentCodes.NFC_DISABLED,
                IncidentCodes.TIME_CHANGED,
                IncidentCodes.TIMEZONE_CHANGED,
                IncidentCodes.MISSED_TRIGGER_WINDOW,
                IncidentCodes.PROCESS_RECREATED,
                IncidentCodes.RELEASE_PARTIAL_FAILURE,
                IncidentCodes.SNAPSHOT_CORRUPTED,
                AndroidIncidentCodes.ALARM_MUTED_BY_DND,
                AndroidIncidentCodes.ALARM_VOLUME_ZERO,
                AndroidIncidentCodes.NOTIFICATIONS_REVOKED,
                AndroidIncidentCodes.FULL_SCREEN_REVOKED,
            )

        reachable.forEach { code ->
            assertThat(ActiveSessionTexts.incidentLabel(code)).isNotEqualTo(code)
        }
    }

    /** Un code inconnu reste visible tel quel : le taire présenterait la session comme saine. */
    @Test
    fun anUnknownIncidentCodeIsStillShown() {
        assertThat(ActiveSessionTexts.incidentLabel("UNE_CAUSE_INCONNUE")).isEqualTo("UNE_CAUSE_INCONNUE")
    }
}
