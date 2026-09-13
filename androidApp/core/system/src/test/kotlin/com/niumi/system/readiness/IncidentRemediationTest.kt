package com.niumi.system.readiness

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import org.junit.Test

/**
 * SPEC_ANDROID §15, « Remédiation des incidents sur l'écran 7 » : un incident remédiable porte
 * l'action de la colonne « Action proposée » de §13, au minimum « Ouvrir les réglages
 * d'accessibilité » pour `BLOCKING_PERMISSION_REVOKED`.
 */
class IncidentRemediationTest {
    @Test
    fun theAccessibilityIncidentAlwaysOffersTheAccessibilitySettings() {
        assertThat(IncidentRemediation.actionFor(IncidentCodes.BLOCKING_PERMISSION_REVOKED))
            .isEqualTo(ReadinessAction.OpenAccessibilitySettings)
    }

    /**
     * Chaque contrôle surveillé par §13.1 doit rester remédiable : la table est l'inverse de
     * [MonitoredReadinessChecks.incidentCodes], et un contrôle ajouté là sans recours ici serait
     * présenté sans action sur l'écran 7.
     */
    @Test
    fun everyMonitoredIncidentCodeHasARecourse() {
        val withoutRecourse =
            MonitoredReadinessChecks.incidentCodes.values.filter { IncidentRemediation.actionFor(it) == null }

        assertThat(withoutRecourse).isEmpty()
    }

    @Test
    fun eachMonitoredCodeMapsToTheActionOfItsOwnReadinessCheck() {
        assertThat(IncidentRemediation.actionFor(AndroidIncidentCodes.ALARM_VOLUME_ZERO))
            .isEqualTo(ReadinessAction.OpenSoundSettings)
        assertThat(IncidentRemediation.actionFor(AndroidIncidentCodes.ALARM_MUTED_BY_DND))
            .isEqualTo(ReadinessAction.OpenDndSettings)
        assertThat(IncidentRemediation.actionFor(AndroidIncidentCodes.FULL_SCREEN_REVOKED))
            .isEqualTo(ReadinessAction.OpenFullScreenIntentSettings)
        assertThat(IncidentRemediation.actionFor(AndroidIncidentCodes.NOTIFICATIONS_REVOKED))
            .isInstanceOf(ReadinessAction.OpenChannelSettings::class.java)
        assertThat(IncidentRemediation.actionFor(IncidentCodes.NFC_DISABLED))
            .isEqualTo(ReadinessAction.OpenNfcSettings)
    }

    /**
     * §13 : un `canScheduleExactAlarms()` faux n'affiche qu'une explication. L'action existe donc,
     * mais elle ne produit aucun `Intent` — la garde est dans `ReadinessSettingsIntentsTest`.
     */
    @Test
    fun theExactAlarmIncidentOffersAnExplanationRatherThanASettingsScreen() {
        assertThat(IncidentRemediation.actionFor(IncidentCodes.ALARM_PERMISSION_REVOKED))
            .isEqualTo(ReadinessAction.ShowExactAlarmDiagnostic)
    }

    /**
     * Un incident qui décrit un fait passé ou un défaut interne n'a aucun recours : proposer un
     * bouton y afficherait un faux espoir d'action (§15, « ne jamais afficher un faux état de
     * fiabilité »).
     */
    @Test
    fun incidentsWithoutAnyRecourseExposeNoAction() {
        listOf(
            IncidentCodes.TIME_CHANGED,
            IncidentCodes.TIMEZONE_CHANGED,
            IncidentCodes.PROCESS_RECREATED,
            IncidentCodes.MISSED_TRIGGER_WINDOW,
            IncidentCodes.RELEASE_PARTIAL_FAILURE,
            IncidentCodes.SNAPSHOT_CORRUPTED,
        ).forEach { code ->
            assertThat(IncidentRemediation.actionFor(code)).isNull()
        }
    }

    @Test
    fun anUnknownCodeExposesNoAction() {
        assertThat(IncidentRemediation.actionFor("ANDROID_SOMETHING_NOBODY_MAPPED")).isNull()
    }
}
