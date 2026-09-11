package com.niumi.feature.setup.readiness

import com.niumi.system.readiness.ReadinessCheckId

/**
 * Textes du diagnostic avant activation (SPEC_ANDROID §13). Ils vivent dans `:feature:setup` et
 * non dans `:core:system`, qui reste sans interface.
 *
 * Cinq messages sont repris **mot pour mot** de §13 (`EXACT_ALARM`, `NOTIFICATIONS`,
 * `NFC_ENABLED`, `ALARM_VOLUME`, `DND_OTHER_MODE`) ; les neuf autres ont été rédigés à l'étape
 * 12b et ajoutés à §13 dans le même changement, la spec restant la source de vérité. Chacun nomme
 * le réglage en cause **et** sa conséquence, en tutoiement (§15).
 *
 * Les `when` sont exhaustifs sans branche `else` : un quinzième contrôle casserait la compilation
 * ici plutôt que d'atterrir silencieusement sur un texte générique.
 */
object ReadinessMessages {
    fun forCheck(id: ReadinessCheckId): String =
        when (id) {
            ReadinessCheckId.NFC_PRESENT -> {
                "Cet appareil n'a pas de puce NFC. Niumi ne peut pas fonctionner sans boîtier à scanner."
            }

            ReadinessCheckId.NFC_ENABLED -> {
                "Le NFC est désactivé. Active-le avant de démarrer la session."
            }

            ReadinessCheckId.PAIRED_BOX -> {
                "Aucun boîtier n'est associé. Associe ton boîtier Niumi : c'est lui qui terminera " +
                    "ta session."
            }

            ReadinessCheckId.APP_SELECTION -> {
                "Aucune application n'est choisie. Sélectionne celles que Niumi bloquera pendant " +
                    "ta session."
            }

            ReadinessCheckId.EXACT_ALARM -> {
                "Niumi ne peut pas programmer ce réveil, car l'accès aux alarmes exactes n'est pas " +
                    "disponible sur cet appareil."
            }

            ReadinessCheckId.FULL_SCREEN_INTENT -> {
                "Autorise les alarmes plein écran, sinon l'écran de réveil ne s'ouvrira pas tout " +
                    "seul au moment de sonner."
            }

            ReadinessCheckId.NOTIFICATIONS -> {
                "Active les notifications pour que l'écran du réveil puisse s'afficher."
            }

            ReadinessCheckId.ALARM_CHANNEL -> {
                "Le canal de notification du réveil est désactivé. Réactive-le, sinon la sonnerie " +
                    "ne pourra pas démarrer."
            }

            ReadinessCheckId.ALARM_VOLUME -> {
                "Le volume des alarmes est à zéro. Augmente-le avant de continuer."
            }

            ReadinessCheckId.DND_TOTAL_SILENCE -> {
                "Le silence total coupe le son des alarmes et empêche l'écran de réveil de " +
                    "s'afficher. Désactive-le avant de démarrer la session."
            }

            ReadinessCheckId.DND_OTHER_MODE -> {
                "Le mode Ne pas déranger peut empêcher la sonnerie d'être audible. Vérifie qu'il " +
                    "autorise les alarmes."
            }

            ReadinessCheckId.ACCESSIBILITY_SERVICE -> {
                "Le service d'accessibilité de Niumi est inactif. Sans lui, les applications " +
                    "choisies ne seront pas bloquées."
            }

            ReadinessCheckId.FUTURE_TRIGGER -> {
                "L'heure de réveil choisie est déjà passée. Choisis une heure future."
            }

            ReadinessCheckId.BATTERY_OPTIMIZATION -> {
                "Les restrictions de batterie peuvent geler Niumi et désactiver le blocage sans " +
                    "prévenir. Lève-les, puis confirme ici que c'est fait."
            }
        }

    /**
     * Nom du contrôle, affiché quand il est satisfait. [forCheck] décrit une **panne** et sa
     * remédiation : l'afficher à côté d'un contrôle qui passe énoncerait l'inverse de la vérité,
     * ce que §15 interdit (« ne jamais afficher un faux état de fiabilité »). Défaut trouvé lors
     * de la validation sur appareil du 2026-09-11.
     */
    fun labelFor(id: ReadinessCheckId): String =
        when (id) {
            ReadinessCheckId.NFC_PRESENT -> "Puce NFC présente"
            ReadinessCheckId.NFC_ENABLED -> "NFC activé"
            ReadinessCheckId.PAIRED_BOX -> "Boîtier associé"
            ReadinessCheckId.APP_SELECTION -> "Applications choisies"
            ReadinessCheckId.EXACT_ALARM -> "Accès aux alarmes exactes"
            ReadinessCheckId.FULL_SCREEN_INTENT -> "Alarmes plein écran autorisées"
            ReadinessCheckId.NOTIFICATIONS -> "Notifications autorisées"
            ReadinessCheckId.ALARM_CHANNEL -> "Canal du réveil actif"
            ReadinessCheckId.ALARM_VOLUME -> "Volume des alarmes audible"
            ReadinessCheckId.DND_TOTAL_SILENCE -> "Silence total désactivé"
            ReadinessCheckId.DND_OTHER_MODE -> "Ne pas déranger inactif"
            ReadinessCheckId.ACCESSIBILITY_SERVICE -> "Service d'accessibilité actif"
            ReadinessCheckId.FUTURE_TRIGGER -> "Heure de réveil future"
            ReadinessCheckId.BATTERY_OPTIMIZATION -> "Restrictions de batterie levées"
        }

    /** Libellé du bouton d'action, lu tel quel par TalkBack (§15). */
    fun actionLabelFor(id: ReadinessCheckId): String =
        when (id) {
            ReadinessCheckId.NFC_PRESENT -> "Appareil non compatible"
            ReadinessCheckId.NFC_ENABLED -> "Ouvrir les réglages NFC"
            ReadinessCheckId.PAIRED_BOX -> "Associer mon boîtier"
            ReadinessCheckId.APP_SELECTION -> "Choisir mes applications"
            ReadinessCheckId.EXACT_ALARM -> "Comprendre ce blocage"
            ReadinessCheckId.FULL_SCREEN_INTENT -> "Ouvrir les réglages plein écran"
            ReadinessCheckId.NOTIFICATIONS -> "Autoriser les notifications"
            ReadinessCheckId.ALARM_CHANNEL -> "Ouvrir les réglages du canal"
            ReadinessCheckId.ALARM_VOLUME -> "Ouvrir les réglages du son"
            ReadinessCheckId.DND_TOTAL_SILENCE -> "Ouvrir les réglages Ne pas déranger"
            ReadinessCheckId.DND_OTHER_MODE -> "Ouvrir les réglages Ne pas déranger"
            ReadinessCheckId.ACCESSIBILITY_SERVICE -> "Ouvrir les réglages d'accessibilité"
            ReadinessCheckId.FUTURE_TRIGGER -> "Corriger l'heure"
            ReadinessCheckId.BATTERY_OPTIMIZATION -> "Lever les restrictions de batterie"
        }

    const val TITLE = "Vérifions ton appareil"

    const val ALL_CLEAR = "Ton appareil est prêt. Tu peux préparer ta session."

    const val BATTERY_CONFIRM_LABEL = "J'ai levé les restrictions"

    /**
     * Libellés des deux étapes de parcours **déjà satisfaites** (§11.1, §12.1) : l'utilisateur
     * peut refaire son choix tant qu'aucune session n'est en cours. « Associer mon boîtier » sur
     * une ligne verte laisserait croire qu'aucun boîtier n'est associé.
     */
    const val CHANGE_PAIRED_BOX_LABEL = "Changer de boîtier"

    const val CHANGE_APP_SELECTION_LABEL = "Modifier ma sélection"

    fun revisitLabelFor(id: ReadinessCheckId): String =
        when (id) {
            ReadinessCheckId.PAIRED_BOX -> CHANGE_PAIRED_BOX_LABEL
            ReadinessCheckId.APP_SELECTION -> CHANGE_APP_SELECTION_LABEL
            else -> actionLabelFor(id)
        }

    /**
     * `ShowExactAlarmDiagnostic` : Niumi déclare `USE_EXACT_ALARM` et jamais
     * `SCHEDULE_EXACT_ALARM` (§13). Il n'existe donc **aucun** réglage utilisateur à ouvrir, et ce
     * texte remplace l'action au lieu de rediriger vers « Alarmes et rappels ».
     */
    const val EXACT_ALARM_DIAGNOSTIC =
        "Niumi déclare cet accès à l'installation, il n'y a pas de réglage à changer. Un refus " +
            "vient d'une incompatibilité de l'appareil ou d'une restriction de sa surcouche."

    /** `Unsupported` : le matériel manque, aucun recours n'existe. */
    const val UNSUPPORTED = "Aucun réglage ne peut corriger ce point sur cet appareil."
}
