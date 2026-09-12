package com.niumi.feature.session.ui

/**
 * Projection d'affichage d'un `WakeScheduleDto`, partagée par les écrans 5, 6 et 7 (SPEC_ANDROID
 * §15 : afficher date, heure et fuseau ; SPEC_CORE_KMP §8.1 : afficher la date complète avant
 * confirmation). Produite par [WakeScheduleFormatter], jamais construite à la main : chaque écran
 * garde son propre état par ailleurs, cette classe n'est pas un état transversal.
 *
 * [shiftedFromLocalTime] n'est renseigné que lors d'un trou d'heure d'été : l'heure saisie
 * n'existait pas ce jour-là, et [timeLabel] porte l'instant réellement programmé (§15, ne jamais
 * afficher un faux état), pas la saisie. Décision validée avec l'utilisateur le 2026-09-12 :
 * l'écran explique alors l'écart plutôt que de le taire.
 */
data class WakeScheduleDisplay(
    val relativeDayLabel: String?,
    val fullDateLabel: String,
    val timeLabel: String,
    val zoneLabel: String,
    val shiftedFromLocalTime: String?,
) {
    /** « Demain, jeudi 4 septembre à 07:00 (Europe/Paris) ». */
    val sentence: String
        get() =
            buildString {
                if (relativeDayLabel != null) {
                    append(relativeDayLabel)
                    append(", ")
                }
                append(fullDateLabel)
                append(" à ")
                append(timeLabel)
                append(" (")
                append(zoneLabel)
                append(")")
            }
}
