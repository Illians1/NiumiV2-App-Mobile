package com.niumi.core.schedule

import com.niumi.core.domain.WakeSchedule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

// Demi-amplitude de la recherche par bissection d'un instant de transition (voir
// `firstValidInstantAtOrAfter`) : les décalages UTC réels vont de -12:00 à +14:00, soit un écart
// maximal de 26h ; 30h laisse une marge confortable sans risquer de rater la fenêtre.
private val TRANSITION_SEARCH_HALF_WINDOW: Duration = 30.hours
private val BISECTION_PRECISION: Duration = 1.nanoseconds

/**
 * Calcule l'instant de réveil à partir d'une heure locale choisie (SPEC_CORE_KMP §8.1). Aucune
 * exception ne traverse la frontière (§14) : une zone ou une heure invalide renvoie un statut
 * typé plutôt que de lancer.
 */
public object WakeScheduleCalculator {
    // Deux issues de refus (zone, heure) avant le calcul, en clauses de garde : même motif que
    // `BoxPayloadParser.parse`, la forme la plus lisible pour une chaîne de contrôles séquentiels
    // (voir `ETAPE-02.md`).
    @Suppress("ReturnCount")
    public fun compute(input: WakeScheduleInput): WakeScheduleResult {
        val zone = resolveZone(input.zoneId) ?: return WakeScheduleResult(WakeScheduleStatus.UNKNOWN_ZONE, null)
        val localTime =
            resolveLocalTime(input.localTimeIso) ?: return WakeScheduleResult(WakeScheduleStatus.INVALID_TIME, null)

        val now = Instant.fromEpochMilliseconds(input.nowEpochMillis)
        val today = now.toLocalDateTime(zone).date
        val sameDayInstant = resolveInstant(LocalDateTime(today, localTime), zone)

        // Si l'instant obtenu n'est pas strictement futur, reprendre au jour civil suivant
        // (SPEC_CORE_KMP §8.1, point 2 ; couvre aussi l'heure choisie exactement égale à l'heure
        // courante, qui doit basculer au lendemain).
        val (chosenDate, chosenInstant) =
            if (sameDayInstant > now) {
                today to sameDayInstant
            } else {
                val tomorrow = today.plus(1, DateTimeUnit.DAY)
                tomorrow to resolveInstant(LocalDateTime(tomorrow, localTime), zone)
            }

        val schedule =
            WakeSchedule(
                localDateIso = chosenDate.toString(),
                localTimeIso = localTime.toString(),
                zoneIdAtActivation = input.zoneId,
                triggerAtEpochMillis = chosenInstant.toEpochMilliseconds(),
            )
        return WakeScheduleResult(WakeScheduleStatus.VALID, schedule)
    }

    @Suppress("SwallowedException")
    private fun resolveZone(zoneId: String): TimeZone? =
        try {
            TimeZone.of(zoneId)
        } catch (invalidZone: IllegalArgumentException) {
            null
        }

    @Suppress("SwallowedException")
    private fun resolveLocalTime(localTimeIso: String): LocalTime? =
        try {
            LocalTime.parse(localTimeIso)
        } catch (invalidTime: IllegalArgumentException) {
            null
        }

    /**
     * Résout un [LocalDateTime] en instant (SPEC_CORE_KMP §8.1, points 3 et 4). Le chevauchement
     * d'automne est déjà résolu à la première occurrence par `toInstant(zone)` — vérifié par
     * round-trip : reconvertir l'instant obtenu redonne le même [LocalDateTime]. Le trou de
     * printemps ne l'est pas : `toInstant(zone)` décale alors l'heure locale de la taille du saut
     * au lieu de retenir le premier instant valide après le saut, ce que la spec impose. Voir
     * `ETAPE-08.md`, constat sur le plan d'origine.
     */
    private fun resolveInstant(
        local: LocalDateTime,
        zone: TimeZone,
    ): Instant {
        val naive = local.toInstant(zone)
        return if (naive.toLocalDateTime(zone) == local) {
            naive
        } else {
            firstValidInstantAtOrAfter(local, zone)
        }
    }

    /**
     * Recherche par bissection le plus petit instant dont la lecture locale dans [zone] est
     * supérieure ou égale à [local]. Appelée uniquement quand [local] tombe dans un trou d'heure
     * d'été : la lecture locale y est monotone croissante (elle « saute » par-dessus le trou sans
     * jamais redescendre), ce qui rend la bissection valide. Ne s'appuie que sur `toLocalDateTime`,
     * jamais sur la politique de trou de `toInstant(zone)` : le résultat est donc identique quelle
     * que soit la plateforme.
     */
    private fun firstValidInstantAtOrAfter(
        local: LocalDateTime,
        zone: TimeZone,
    ): Instant {
        var lower = local.toInstant(UtcOffset.ZERO) - TRANSITION_SEARCH_HALF_WINDOW
        var upper = local.toInstant(UtcOffset.ZERO) + TRANSITION_SEARCH_HALF_WINDOW
        while (upper - lower > BISECTION_PRECISION) {
            val mid = lower + (upper - lower) / 2
            if (mid.toLocalDateTime(zone) < local) {
                lower = mid
            } else {
                upper = mid
            }
        }
        return upper
    }
}
