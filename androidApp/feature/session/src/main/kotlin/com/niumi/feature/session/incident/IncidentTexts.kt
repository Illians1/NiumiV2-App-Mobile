package com.niumi.feature.session.incident

import com.niumi.core.interop.IncidentSeverityDto

/**
 * Libellés d'un incident, partagés par l'écran 7 et l'écran 12 (SPEC_ANDROID §15). Une seule table :
 * les deux écrans parlent du même fait et ne peuvent pas le nommer différemment.
 *
 * Extraits d'`ActiveSessionTexts` à l'étape 16, quand l'écran 12 s'est mis à afficher des incidents.
 * Mesuré sur appareil : il y montrait le code technique brut (`BLOCKING_PERMISSION_REVOKED`), que
 * SPEC_CORE_KMP §7.3 ne saurait accepter d'un « diagnostic visible par l'utilisateur ».
 */
object IncidentTexts {
    fun severityLabel(severity: IncidentSeverityDto): String =
        when (severity) {
            IncidentSeverityDto.WARNING -> "Information"
            IncidentSeverityDto.DEGRADED -> "Fiabilité réduite"
            IncidentSeverityDto.CRITICAL -> "Critique"
        }

    /**
     * Les codes d'incident sont des identifiants techniques (SPEC_CORE_KMP §7.1) : sont traduits
     * les six que la surveillance de §13.1 peut produire pendant une session, plus ceux du
     * réconciliateur. Un code inconnu reste affiché tel quel plutôt que masqué — §15 interdit de
     * présenter une session comme saine quand elle ne l'est pas.
     */
    fun label(code: String): String =
        when (code) {
            "BLOCKING_PERMISSION_REVOKED" -> {
                "Le service d'accessibilité a été désactivé : le blocage ne s'applique plus."
            }

            "ALARM_PERMISSION_REVOKED" -> {
                "L'autorisation d'alarme exacte a été retirée : le réveil peut être retardé."
            }

            "ANDROID_ALARM_MUTED_BY_DND" -> {
                "Le mode « Ne pas déranger » coupe le son de l'alarme."
            }

            "ANDROID_ALARM_VOLUME_ZERO" -> {
                "Le volume des alarmes est à zéro."
            }

            "ANDROID_NOTIFICATIONS_REVOKED" -> {
                "Les notifications de Niumi sont désactivées."
            }

            "ANDROID_FULL_SCREEN_REVOKED" -> {
                "Niumi ne peut plus ouvrir l'écran de réveil par-dessus l'écran verrouillé."
            }

            "NFC_DISABLED" -> {
                "Le NFC est désactivé : il faudra le réactiver pour scanner ton boîtier."
            }

            "TIME_CHANGED" -> {
                "L'heure du téléphone a changé."
            }

            "TIMEZONE_CHANGED" -> {
                "Le fuseau horaire du téléphone a changé."
            }

            "MISSED_TRIGGER_WINDOW" -> {
                "Le réveil n'a pas pu sonner à l'heure prévue."
            }

            "PROCESS_RECREATED" -> {
                "Niumi a été relancé pendant la session."
            }

            "RELEASE_PARTIAL_FAILURE" -> {
                "Le déblocage n'a pas pu être terminé entièrement."
            }

            "MISSED_BLOCKING_START_WINDOW" -> {
                "Le blocage a commencé en retard : Niumi n'était pas en vie à l'heure prévue."
            }

            "SNAPSHOT_CORRUPTED" -> {
                "Les données de la session n'étaient pas lisibles."
            }

            else -> {
                code
            }
        }
}
