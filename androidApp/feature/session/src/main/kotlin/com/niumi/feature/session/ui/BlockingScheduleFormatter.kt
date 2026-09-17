package com.niumi.feature.session.ui

import com.niumi.core.interop.BlockingScheduleDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.core.interop.isImmediate

/**
 * Formate le début d'un blocage différé pour les écrans 5, 6 et 7 (SPEC_ANDROID §15, Lot 6 ;
 * SPEC_CORE_KMP §8.3). Ne porte aucune règle propre : l'instant de début suit exactement les règles
 * d'affichage du réveil — libellé relatif, date complète, fuseau, trou d'heure d'été — et ce
 * formateur se contente donc de traduire le `BlockingScheduleDto` en [WakeScheduleDto] équivalent
 * avant de déléguer à [WakeScheduleFormatter]. Deux mises en forme des instants dans ce module
 * finiraient par diverger ; il n'y en a qu'une.
 *
 * Rend `null` quand il n'y a rien à afficher : blocage immédiat (`isImmediate`, l'aide interop du
 * contrat 1.3), ou schedule aux champs partiels.
 */
object BlockingScheduleFormatter {
    fun format(
        schedule: BlockingScheduleDto,
        zoneIdAtActivation: String,
        nowEpochMillis: Long,
        displayZoneId: String = zoneIdAtActivation,
        use24Hour: Boolean = true,
    ): WakeScheduleDisplay? {
        if (schedule.isImmediate) return null
        return wakeScheduleOf(schedule, zoneIdAtActivation)?.let { equivalent ->
            WakeScheduleFormatter.format(
                schedule = equivalent,
                nowEpochMillis = nowEpochMillis,
                displayZoneId = displayZoneId,
                use24Hour = use24Hour,
            )
        }
    }

    /**
     * `null` pour un schedule aux champs partiels : un mélange que `INVALID_BLOCKING_SCHEDULE`
     * refuse en amont (SPEC_CORE_KMP §7.5) et dont l'interface n'a surtout rien à inventer.
     */
    private fun wakeScheduleOf(
        schedule: BlockingScheduleDto,
        zoneIdAtActivation: String,
    ): WakeScheduleDto? =
        schedule.localDateIso?.let { localDateIso ->
            schedule.localTimeIso?.let { localTimeIso ->
                schedule.startsAtEpochMillis?.let { startsAtEpochMillis ->
                    WakeScheduleDto(
                        localDateIso = localDateIso,
                        localTimeIso = localTimeIso,
                        zoneIdAtActivation = zoneIdAtActivation,
                        triggerAtEpochMillis = startsAtEpochMillis,
                    )
                }
            }
        }
}
