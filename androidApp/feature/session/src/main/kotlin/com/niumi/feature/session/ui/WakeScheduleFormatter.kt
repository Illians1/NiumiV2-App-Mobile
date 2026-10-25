package com.niumi.feature.session.ui

import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Heure d'un instant pour la notification du service de garde du blocage (SPEC_ANDROID §12.5). Lit
 * l'instant programmé, jamais l'heure saisie : lors d'un trou d'heure d'été, les deux diffèrent et
 * afficher la saisie serait le faux état interdit par §15. Depuis l'étape 45, l'accueil écrit ses
 * heures avec `NiumiTimeFormat` ; ce formateur ne sert plus qu'à la notification.
 *
 * Les locales sont passées explicitement (jamais `Locale.getDefault()`) pour que l'affichage ne
 * dépende pas de la locale par défaut de l'appareil (chiffres arabo-indiens sur un appareil en
 * `ar-EG`, par exemple).
 */
object WakeScheduleFormatter {
    /** « 7:00 », sans zéro devant, comme les écrans (`design-plan/SPECIFICATION.md` §2). */
    private val TIME_FORMATTER_24H = DateTimeFormatter.ofPattern("H:mm", Locale.ROOT)
    private val TIME_FORMATTER_12H = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    /**
     * Heure seule d'un instant, dans le fuseau de l'activation. `null` si le fuseau persisté est
     * illisible : un service de premier plan ne doit jamais tomber pour un libellé.
     */
    fun formatTime(
        epochMillis: Long,
        zoneId: String,
        use24Hour: Boolean,
    ): String? =
        try {
            (if (use24Hour) TIME_FORMATTER_24H else TIME_FORMATTER_12H)
                .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of(zoneId)))
        } catch (_: DateTimeException) {
            null
        }
}
