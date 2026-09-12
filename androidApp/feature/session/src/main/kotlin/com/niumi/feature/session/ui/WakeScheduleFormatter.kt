package com.niumi.feature.session.ui

import com.niumi.core.interop.WakeScheduleDto
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Formate un [WakeScheduleDto] pour l'affichage (SPEC_ANDROID §8, §15). Lit exclusivement
 * `triggerAtEpochMillis`, jamais `localDateIso`/`localTimeIso` : lors d'un trou d'heure d'été,
 * `WakeScheduleCalculator` (`:shared:core`) conserve la saisie d'origine dans `localTimeIso` alors
 * que `triggerAtEpochMillis` porte l'instant réellement programmé — afficher la saisie serait le
 * faux état interdit par §15.
 *
 * Toutes les locales sont passées explicitement (jamais `Locale.getDefault()`) pour que l'affichage
 * ne dépende pas de la locale par défaut de l'appareil (chiffres arabo-indiens sur un appareil en
 * `ar-EG`, par exemple).
 */
object WakeScheduleFormatter {
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE)
    private val TIME_FORMATTER_24H = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    private val TIME_FORMATTER_12H = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    fun format(
        schedule: WakeScheduleDto,
        nowEpochMillis: Long,
        displayZoneId: String = schedule.zoneIdAtActivation,
        use24Hour: Boolean = true,
    ): WakeScheduleDisplay {
        val zone = ZoneId.of(displayZoneId)
        val triggerZoned = Instant.ofEpochMilli(schedule.triggerAtEpochMillis).atZone(zone)
        val nowZoned = Instant.ofEpochMilli(nowEpochMillis).atZone(zone)
        val daysUntilTrigger = ChronoUnit.DAYS.between(nowZoned.toLocalDate(), triggerZoned.toLocalDate())

        return WakeScheduleDisplay(
            relativeDayLabel =
                when (daysUntilTrigger) {
                    0L -> "Aujourd'hui"
                    1L -> "Demain"
                    else -> null
                },
            fullDateLabel = DATE_FORMATTER.format(triggerZoned),
            timeLabel = (if (use24Hour) TIME_FORMATTER_24H else TIME_FORMATTER_12H).format(triggerZoned),
            zoneLabel = displayZoneId,
            shiftedFromLocalTime = shiftedFromLocalTime(schedule, displayZoneId),
        )
    }

    /**
     * Non nul seulement quand le fuseau affiché est celui de l'activation : un fuseau d'affichage
     * différent (§8, heure recalculée dans le fuseau courant) n'est pas un trou d'heure d'été et
     * ne doit pas déclencher l'explication.
     */
    private fun shiftedFromLocalTime(
        schedule: WakeScheduleDto,
        displayZoneId: String,
    ): String? {
        val savedLocalTime =
            schedule.localTimeIso
                .takeIf { displayZoneId == schedule.zoneIdAtActivation }
                ?.let(::parseLocalTimeOrNull)
        val actualLocalTime =
            Instant
                .ofEpochMilli(schedule.triggerAtEpochMillis)
                .atZone(ZoneId.of(schedule.zoneIdAtActivation))
                .toLocalTime()
        return schedule.localTimeIso.takeIf { savedLocalTime != null && savedLocalTime != actualLocalTime }
    }

    /** Un horaire persisté peut être illisible ; l'écart n'est alors simplement pas explicable. */
    private fun parseLocalTimeOrNull(localTimeIso: String): LocalTime? =
        try {
            LocalTime.parse(localTimeIso)
        } catch (_: DateTimeException) {
            null
        }
}
