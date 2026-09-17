package com.niumi.feature.session.active

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.readiness.AndroidIncidentCodes
import org.junit.Test

/**
 * Textes des écrans 7, 9, 10 et 11 (SPEC_ANDROID §15). Deux règles s'y appliquent sans exception :
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
            ActiveSessionTexts.BLOCKING_START_TITLE,
            ActiveSessionTexts.blockedAppsTitle(isBlockingPending = true),
            ActiveSessionTexts.stateLabel(SessionStateDto.ARMED),
            ActiveSessionTexts.stateLabel(SessionStateDto.ARMED, blockingTimeLabel = "22:30"),
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
            CompletedTexts.TITLE,
            CompletedTexts.BODY,
            CompletedTexts.BACK_HOME_BUTTON,
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

    /**
     * Écran 10 (étape 17). Les deux écrans de fin affirment le déblocage : ils ne sont atteints
     * qu'après `RELEASE_SUCCEEDED` (§11.3), donc aucun des deux ne peut mentir.
     */
    @Test
    fun theCompletedScreenStatesTheUnblocking() {
        assertThat(CompletedTexts.TITLE).isEqualTo("Session terminée")
        assertThat(CompletedTexts.BODY).isEqualTo(CancelledTexts.BODY)
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
                // Lot 6 : un blocage appliqué en retard est un incident comme un autre (§15).
                IncidentCodes.MISSED_BLOCKING_START_WINDOW,
            )

        reachable.forEach { code ->
            assertThat(ActiveSessionTexts.incidentLabel(code)).isNotEqualTo(code)
        }
    }

    /**
     * Lot 6, SPEC_ANDROID §15 « Écran 7 » : le libellé `ARMED` se dédouble d'après
     * `blockingAppliedAtEpochMillis`, relayé ici par la présence de l'heure de début.
     */
    @Test
    fun theArmedLabelSaysWhetherApplicationsAreAlreadyBlocked() {
        assertThat(ActiveSessionTexts.stateLabel(SessionStateDto.ARMED))
            .isEqualTo("Réveil programmé · applications bloquées")
        assertThat(ActiveSessionTexts.stateLabel(SessionStateDto.ARMED, blockingTimeLabel = "22:30"))
            .isEqualTo("Réveil programmé · blocage à 22:30")
    }

    /** Les huit autres états ne portent aucune heure de début : leur libellé ne change pas. */
    @Test
    fun theBlockingStartNeverChangesTheOtherStateLabels() {
        SessionStateDto.entries
            .filter { it != SessionStateDto.ARMED }
            .forEach { state ->
                assertThat(ActiveSessionTexts.stateLabel(state, blockingTimeLabel = "22:30"))
                    .isEqualTo(ActiveSessionTexts.stateLabel(state))
            }
    }

    /** §15 : « un titre qui dit la vérité ». */
    @Test
    fun theBlockedAppsTitleTellsWhetherBlockingHasStarted() {
        assertThat(ActiveSessionTexts.blockedAppsTitle(isBlockingPending = false))
            .isEqualTo("Applications bloquées")
        assertThat(ActiveSessionTexts.blockedAppsTitle(isBlockingPending = true))
            .isEqualTo("Applications qui seront bloquées")
        assertThat(ActiveSessionTexts.blockedAppsTitle(isBlockingPending = false))
            .isEqualTo(ActiveSessionTexts.BLOCKED_APPS_TITLE)
    }

    @Test
    fun theBlockingStartLineIsTitled() {
        assertThat(ActiveSessionTexts.BLOCKING_START_TITLE).isEqualTo("Début du blocage")
    }

    /** Le retard de blocage est nommé en clair, comme tout incident (§15, écrans 7 et 12). */
    @Test
    fun theLateBlockingIncidentIsNamedInPlainWords() {
        assertThat(ActiveSessionTexts.incidentLabel(IncidentCodes.MISSED_BLOCKING_START_WINDOW))
            .isEqualTo("Le blocage a commencé en retard : Niumi n'était pas en vie à l'heure prévue.")
    }

    /** Un code inconnu reste visible tel quel : le taire présenterait la session comme saine. */
    @Test
    fun anUnknownIncidentCodeIsStillShown() {
        assertThat(ActiveSessionTexts.incidentLabel("UNE_CAUSE_INCONNUE")).isEqualTo("UNE_CAUSE_INCONNUE")
    }
}
