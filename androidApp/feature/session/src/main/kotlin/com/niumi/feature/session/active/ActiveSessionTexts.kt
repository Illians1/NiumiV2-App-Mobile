package com.niumi.feature.session.active

import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.SessionStateDto

/** Textes de l'écran 7 (SPEC_ANDROID §15, écran 7), complets depuis l'étape 15. Tutoiement (§15). */
object ActiveSessionTexts {
    const val TITLE = "Ta session est active"

    const val NO_SESSION = "Aucune session active."

    const val CURRENT_ZONE_TITLE = "Dans ton fuseau actuel"

    const val COMMITMENT_REMINDER = "Seul le scan du boîtier terminera la session."

    const val BLOCKED_APPS_TITLE = "Applications bloquées"

    const val NO_BLOCKED_APP = "Aucune application bloquée pour cette session."

    const val HEALTH_TITLE = "État de la session"

    const val HEALTHY = "Tout fonctionne normalement."

    /**
     * §15 : « ne jamais afficher un faux état de fiabilité ». Une session `DEGRADED` ne redevient
     * jamais `HEALTHY` d'elle-même (SPEC_CORE_KMP §7.3), le texte ne promet donc aucun retour à la
     * normale.
     */
    const val DEGRADED = "Un incident a affecté cette session. Ton réveil peut être moins fiable."

    const val INCIDENTS_TITLE = "Incidents"

    const val CRITICAL_INCIDENTS_TITLE = "À vérifier maintenant"

    /** Écran 9 : le seul chemin de sortie d'une session active (SPEC_CORE_KMP §2, points 3 et 4). */
    const val MODIFY_OR_CANCEL_BUTTON = "Modifier ou annuler"

    /**
     * Un état métier nommé en clair. `RINGING` et les trois états d'attente de scan ont leur propre
     * écran (§10.4) ; ils ne sont listés ici que pour n'afficher jamais un état inconnu.
     */
    fun stateLabel(state: SessionStateDto): String =
        when (state) {
            SessionStateDto.PREPARING -> "Activation en cours"
            SessionStateDto.ARMED -> "Réveil programmé"
            SessionStateDto.RINGING -> "Ton réveil sonne"
            SessionStateDto.AWAITING_NFC -> "En attente du scan de ton boîtier"
            SessionStateDto.TRIGGERED_AWAITING_NFC -> "En attente du scan de ton boîtier"
            SessionStateDto.RELEASING -> "Déblocage en cours"
            SessionStateDto.COMPLETED -> "Session terminée"
            SessionStateDto.CANCELLED -> "Session annulée"
            SessionStateDto.FAILED -> "L'activation a échoué"
        }

    fun severityLabel(severity: IncidentSeverityDto): String =
        when (severity) {
            IncidentSeverityDto.WARNING -> "Information"
            IncidentSeverityDto.DEGRADED -> "Fiabilité réduite"
            IncidentSeverityDto.CRITICAL -> "Critique"
        }

    /**
     * Les codes d'incident sont des identifiants techniques (SPEC_CORE_KMP §7.1) : l'écran 7 en
     * traduit les six que la surveillance de §13.1 peut produire pendant une session, plus les
     * quatre du réconciliateur. Un code inconnu reste affiché tel quel plutôt que masqué — §15
     * interdit de présenter une session comme saine quand elle ne l'est pas.
     */
    fun incidentLabel(code: String): String =
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

            "SNAPSHOT_CORRUPTED" -> {
                "Les données de la session n'étaient pas lisibles."
            }

            else -> {
                code
            }
        }
}
