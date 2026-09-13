package com.niumi.feature.session.diagnostics

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.DeviceContext
import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheck
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome
import org.junit.Test
import java.time.ZoneId

/**
 * SPEC_ANDROID §16 et §17 : l'export est un texte produit après action explicite de l'utilisateur,
 * qui masque le token, son hash complet et tout identifiant matériel, et ne contient que les 200
 * événements locaux et les résultats du contrôle de santé.
 */
class DiagnosticExporterTest {
    private val deviceContext =
        DeviceContext(deviceModel = "Pixel Test", androidVersion = "16 (API 36)", appVersion = "1.0.0 (1)")

    private val zone = ZoneId.of("Europe/Paris")

    private val boxId = "550e8400-e29b-41d4-a716-446655440000"

    private fun event(
        type: TechnicalEventType = TechnicalEventType.SESSION_ARMED,
        occurredAtEpochMillis: Long = 1_700_000_000_000L,
        context: DeviceContext = deviceContext,
    ) = TechnicalEventEntry(
        type = type,
        sessionId = "session-1",
        detailsJson = null,
        occurredAtEpochMillis = occurredAtEpochMillis,
        deviceModel = context.deviceModel,
        androidVersion = context.androidVersion,
        appVersion = context.appVersion,
    )

    private fun report(
        boxId: String? = this.boxId,
        checks: List<ReadinessCheck> = emptyList(),
        incidents: List<SessionIncidentDto> = emptyList(),
        events: List<TechnicalEventEntry> = emptyList(),
    ) = DiagnosticReport(
        deviceContext = deviceContext,
        sessionId = "session-1",
        state = SessionStateDto.ARMED,
        health = SessionHealthDto.DEGRADED,
        boxId = boxId,
        checks = checks,
        incidents = incidents,
        events = events,
    )

    private fun export(report: DiagnosticReport) = DiagnosticExporter.export(report, zone)

    /** §17 : « Masquer le token, son hash complet et tout identifiant matériel. » */
    @Test
    fun theBoxIdIsTruncatedToItsFirstEightCharacters() {
        val text = export(report())

        assertThat(text).contains("550e8400")
        assertThat(text).doesNotContain(boxId)
        assertThat(text).doesNotContain("e29b-41d4")
    }

    /**
     * Le hash du token n'est pas seulement absent du texte : il n'entre jamais dans
     * [DiagnosticReport]. On ne peut pas divulguer ce qu'on ne reçoit pas — la garde est
     * structurelle, ce test en fige l'effet observable.
     */
    @Test
    fun noTokenHashCanReachTheExportBecauseTheReportNeverCarriesOne() {
        val text = export(report())

        assertThat(text).doesNotContain("a".repeat(64))
        assertThat(text.lowercase()).doesNotContain("sha256")
        assertThat(text.lowercase()).doesNotContain("token")
    }

    @Test
    fun aSessionWithoutAnyPairedBoxExportsNoBoxLineAtAll() {
        val text = export(report(boxId = null))

        assertThat(text).doesNotContain("550e8400")
    }

    /** §17 : « L'export ne doit contenir que les 200 événements locaux ». */
    @Test
    fun atMostTwoHundredEventsAreExported() {
        val events = (1..250).map { event(occurredAtEpochMillis = 1_700_000_000_000L + it) }

        val text = export(report(events = events))

        val eventLines = text.lines().filter { it.contains(TechnicalEventType.SESSION_ARMED.name) }
        assertThat(eventLines).hasSize(200)
    }

    @Test
    fun theHeaderCarriesTheDeviceContextOfSeventeen() {
        val text = export(report())

        assertThat(text).contains("Pixel Test")
        assertThat(text).contains("16 (API 36)")
        assertThat(text).contains("1.0.0 (1)")
    }

    /**
     * Le contexte est porté par événement précisément pour qu'un journal enjambant une mise à jour
     * reste juste : une ligne dont le contexte diffère de l'en-tête le rappelle, les autres non —
     * sans quoi 200 lignes répéteraient la même constante.
     */
    @Test
    fun anEventFromAnotherAppVersionCarriesItsOwnContext() {
        val older = deviceContext.copy(appVersion = "0.9.0 (7)")

        val text = export(report(events = listOf(event(context = older), event())))

        assertThat(text).contains("0.9.0 (7)")
        assertThat(text.lines().filter { it.contains("0.9.0 (7)") }).hasSize(1)
    }

    @Test
    fun checksAreExportedWithTheirOutcome() {
        val checks =
            listOf(
                ReadinessCheck(
                    id = ReadinessCheckId.ACCESSIBILITY_SERVICE,
                    severity = ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE,
                    outcome = ReadinessOutcome.FAILED,
                    action = ReadinessAction.OpenAccessibilitySettings,
                ),
            )

        val text = export(report(checks = checks))

        assertThat(text).contains(IncidentDiagnosticTexts.checkLabel(ReadinessCheckId.ACCESSIBILITY_SERVICE))
        assertThat(text).contains(IncidentDiagnosticTexts.OUTCOME_FAILED)
    }

    @Test
    fun incidentsAreExportedWithTheirCodeAndSeverity() {
        val incidents =
            listOf(
                SessionIncidentDto(
                    code = "BLOCKING_PERMISSION_REVOKED",
                    severity = IncidentSeverityDto.CRITICAL,
                    occurredAtEpochMillis = 1_700_000_000_000L,
                    platform = PlatformDto.ANDROID,
                ),
            )

        val text = export(report(incidents = incidents))

        assertThat(text).contains("BLOCKING_PERMISSION_REVOKED")
        assertThat(text).contains(IncidentDiagnosticTexts.severityLabel(IncidentSeverityDto.CRITICAL))
    }

    /** Les horodatages sont lus par un humain : format local, fuseau injecté pour rester stable. */
    @Test
    fun timestampsAreFormattedInTheGivenZone() {
        val text = export(report(events = listOf(event(occurredAtEpochMillis = 1_700_000_000_000L))))

        // 1 700 000 000 s UTC = 22:13:20 UTC, soit 23:13:20 à Paris (UTC+1 en novembre).
        assertThat(text).contains("2023-11-14 23:13:20")
    }

    @Test
    fun anEmptyDiagnosticStillProducesAReadableReport() {
        val text = export(report(boxId = null))

        assertThat(text).contains(IncidentDiagnosticTexts.EXPORT_TITLE)
        assertThat(text).contains(IncidentDiagnosticTexts.NO_EVENT)
    }
}
