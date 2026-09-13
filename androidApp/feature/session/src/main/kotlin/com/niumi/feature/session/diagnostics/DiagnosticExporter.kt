package com.niumi.feature.session.diagnostics

import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.logging.DeviceContext
import com.niumi.database.logging.MAX_TECHNICAL_EVENTS
import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.system.readiness.ReadinessCheck
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Nombre de caractères de `boxId` conservés à l'export (SPEC_ANDROID §17). */
private const val BOX_ID_VISIBLE_CHARS = 8

private val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/**
 * Ce que l'export a le droit de contenir (SPEC_ANDROID §17 : « les 200 événements locaux et les
 * résultats du contrôle de santé »).
 *
 * **Le hash du token n'y figure pas, et c'est délibéré.** `AndroidSessionExtras` porte
 * `boxTokenSha256Hex` ; le rapport ne le reprend jamais. Un secret qu'on ne transporte pas ne peut
 * pas fuiter par un format d'affichage oublié. [boxId] est transporté entier et tronqué par
 * l'exporteur, seul endroit où la règle des huit caractères s'applique.
 */
data class DiagnosticReport(
    val deviceContext: DeviceContext,
    val sessionId: String?,
    val state: SessionStateDto?,
    val health: SessionHealthDto?,
    val boxId: String?,
    val checks: List<ReadinessCheck>,
    val incidents: List<SessionIncidentDto>,
    val events: List<TechnicalEventEntry>,
)

/**
 * Rend le diagnostic en texte brut (SPEC_ANDROID §17). Produit **le texte seul** : l'`ACTION_SEND`
 * est construit par l'écran, comme tout `Intent` (§13).
 *
 * Le contexte d'appareil est en tête. Une ligne d'événement ne le répète que s'il diffère de
 * celui-là — ce qui n'arrive qu'après une mise à jour de l'application ou du système pendant la
 * fenêtre des 200 événements. Sans cette règle, 200 lignes répéteraient la même constante et
 * l'export deviendrait illisible ; sans le contexte par ligne, un journal enjambant une mise à jour
 * attribuerait la nouvelle version aux événements antérieurs.
 */
object DiagnosticExporter {
    fun export(
        report: DiagnosticReport,
        zoneId: ZoneId,
    ): String =
        buildString {
            appendLine(IncidentDiagnosticTexts.EXPORT_TITLE)
            appendHeader(report.deviceContext)
            appendLine()
            appendSession(report)
            appendLine()
            appendChecks(report.checks)
            appendLine()
            appendIncidents(report.incidents, zoneId)
            appendLine()
            appendEvents(report, zoneId)
        }

    private fun StringBuilder.appendHeader(context: DeviceContext) {
        appendLine("Appareil : ${context.deviceModel}")
        appendLine("Android : ${context.androidVersion}")
        appendLine("Application : ${context.appVersion}")
    }

    private fun StringBuilder.appendSession(report: DiagnosticReport) {
        appendLine(IncidentDiagnosticTexts.SESSION_TITLE)
        if (report.sessionId == null) {
            appendLine(IncidentDiagnosticTexts.NO_SESSION)
            return
        }
        appendLine("- Identifiant : ${report.sessionId}")
        report.state?.let { appendLine("- État : ${IncidentDiagnosticTexts.stateLabel(it)}") }
        report.health?.let { appendLine("- Santé : ${IncidentDiagnosticTexts.healthLabel(it)}") }
        report.boxId?.let { appendLine("- ${IncidentDiagnosticTexts.BOX_TITLE} : ${mask(it)}") }
    }

    private fun StringBuilder.appendChecks(checks: List<ReadinessCheck>) {
        appendLine(IncidentDiagnosticTexts.CHECKS_TITLE)
        checks.forEach { check ->
            val label = IncidentDiagnosticTexts.checkLabel(check.id)
            appendLine("- $label : ${IncidentDiagnosticTexts.outcomeLabel(check.outcome)}")
        }
    }

    private fun StringBuilder.appendIncidents(
        incidents: List<SessionIncidentDto>,
        zoneId: ZoneId,
    ) {
        appendLine(IncidentDiagnosticTexts.INCIDENTS_TITLE)
        if (incidents.isEmpty()) {
            appendLine(IncidentDiagnosticTexts.NO_INCIDENT)
            return
        }
        incidents.forEach { incident ->
            val severity = IncidentDiagnosticTexts.severityLabel(incident.severity)
            val at = format(incident.occurredAtEpochMillis, zoneId)
            appendLine("- $at — $severity — ${IncidentDiagnosticTexts.incidentLabel(incident.code)}")
        }
    }

    private fun StringBuilder.appendEvents(
        report: DiagnosticReport,
        zoneId: ZoneId,
    ) {
        appendLine(IncidentDiagnosticTexts.EVENTS_TITLE)
        val events = report.events.take(MAX_TECHNICAL_EVENTS)
        if (events.isEmpty()) {
            appendLine(IncidentDiagnosticTexts.NO_EVENT)
            return
        }
        events.forEach { event ->
            append(format(event.occurredAtEpochMillis, zoneId))
            append(' ')
            append(event.type.name)
            event.sessionId?.let {
                append(' ')
                append(it)
            }
            event.detailsJson?.let {
                append(' ')
                append(it)
            }
            contextSuffix(event, report.deviceContext)?.let(::append)
            appendLine()
        }
    }

    /**
     * Le contexte de l'événement, seulement quand il diffère de l'en-tête. `""` (les lignes
     * antérieures à la v2 de la base, cf. `MIGRATION_1_2`) n'est pas une divergence à signaler :
     * c'est une absence, et l'en-tête reste la meilleure information disponible.
     */
    private fun contextSuffix(
        event: TechnicalEventEntry,
        header: DeviceContext,
    ): String? {
        val differing =
            listOfNotNull(
                event.deviceModel.takeIf { it.isNotEmpty() && it != header.deviceModel },
                event.androidVersion.takeIf { it.isNotEmpty() && it != header.androidVersion },
                event.appVersion.takeIf { it.isNotEmpty() && it != header.appVersion },
            )
        return differing.takeIf { it.isNotEmpty() }?.joinToString(prefix = " [", separator = " / ", postfix = "]")
    }

    /**
     * §17 : « Masquer le token, son hash complet et tout identifiant matériel. » Le `boxId` est un
     * UUID d'association, pas un secret, mais l'export est destiné à sortir de l'appareil : huit
     * caractères suffisent à distinguer deux boîtiers dans un échange d'assistance.
     */
    private fun mask(boxId: String): String = boxId.take(BOX_ID_VISIBLE_CHARS) + "…"

    private fun format(
        epochMillis: Long,
        zoneId: ZoneId,
    ): String = TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))
}
