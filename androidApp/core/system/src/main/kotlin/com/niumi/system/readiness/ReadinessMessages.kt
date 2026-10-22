package com.niumi.system.readiness

/**
 * Textes des contrôles de SPEC_ANDROID §13. Ils vivent dans `:core:system` depuis l'étape 36 de la
 * refonte : l'écran 2 (`:feature:setup`) les affiche dans les feuilles de la tuile « Blocage », et
 * le récapitulatif (`:feature:session`) nomme avec eux le contrôle qui refuse l'activation. Deux
 * modules `feature` ne peuvent pas partager un fichier autrement (§6) ; `:core:system` porte déjà
 * des textes affichés (`SessionWarningNotificationSpecs`). Apostrophe typographique (D13).
 *
 * Cinq messages sont repris **mot pour mot** de §13 (`EXACT_ALARM`, `NOTIFICATIONS`,
 * `NFC_ENABLED`, `ALARM_VOLUME`, `DND_OTHER_MODE`) ; les autres ont été rédigés à l'étape
 * 12b et ajoutés à §13 dans le même changement, la spec restant la source de vérité. Chacun nomme
 * le réglage en cause **et** sa conséquence, en tutoiement (§15). Lot 9 : le contrôle du plein
 * écran est retiré, et le message `NOTIFICATIONS` revu — la notification de sonnerie est devenue
 * le chemin vers l'écran de scan.
 *
 * Les `when` sont exhaustifs sans branche `else` : un quinzième contrôle casserait la compilation
 * ici plutôt que d'atterrir silencieusement sur un texte générique.
 */
object ReadinessMessages {
    fun forCheck(id: ReadinessCheckId): String =
        when (id) {
            ReadinessCheckId.NFC_PRESENT -> {
                "Cet appareil n’a pas de puce NFC. Niumi ne peut pas fonctionner sans boîtier à scanner."
            }

            ReadinessCheckId.NFC_ENABLED -> {
                "Le NFC est désactivé. Active-le avant de démarrer la session."
            }

            ReadinessCheckId.PAIRED_BOX -> {
                "Aucun boîtier n’est associé. Associe au moins un boîtier Niumi : c’est l’un d’eux " +
                    "qui terminera ta session."
            }

            ReadinessCheckId.APP_SELECTION -> {
                "Aucune application n’est choisie. Sélectionne celles que Niumi bloquera pendant " +
                    "ta session."
            }

            ReadinessCheckId.EXACT_ALARM -> {
                "Niumi ne peut pas programmer ce réveil, car l’accès aux alarmes exactes n’est pas " +
                    "disponible sur cet appareil."
            }

            ReadinessCheckId.NOTIFICATIONS -> {
                "Active les notifications de Niumi : c’est par elles que tu ouvres l’écran de scan " +
                    "quand ton réveil sonne."
            }

            ReadinessCheckId.ALARM_CHANNEL -> {
                "Le canal de notification du réveil est désactivé. Réactive-le, sinon la sonnerie " +
                    "ne pourra pas démarrer."
            }

            ReadinessCheckId.ALARM_VOLUME -> {
                "Le volume des alarmes est à zéro. Augmente-le avant de continuer."
            }

            ReadinessCheckId.DND_TOTAL_SILENCE -> {
                "Le silence total coupe le son et la vibration des alarmes et cache la notification " +
                    "du réveil sur l’écran de verrouillage. Désactive-le avant de démarrer la session."
            }

            ReadinessCheckId.DND_OTHER_MODE -> {
                "Le mode Ne pas déranger peut empêcher la sonnerie d’être audible. Vérifie qu’il " +
                    "autorise les alarmes."
            }

            ReadinessCheckId.ACCESSIBILITY_SERVICE -> {
                "Le service d’accessibilité de Niumi est inactif. Sans lui, les applications " +
                    "choisies ne seront pas bloquées."
            }

            ReadinessCheckId.FUTURE_TRIGGER -> {
                "L’heure de réveil choisie est déjà passée. Choisis une heure future."
            }

            ReadinessCheckId.BATTERY_OPTIMIZATION -> {
                "Les restrictions de batterie peuvent geler Niumi et désactiver le blocage sans " +
                    "prévenir. Autorise Niumi à fonctionner sans restriction."
            }

            ReadinessCheckId.RECENTS_LOCK -> {
                "Sur ce téléphone, « Tout effacer » dans les applications récentes arrête le blocage, " +
                    "sauf si Niumi y est verrouillé. Ouvre les applications récentes, appuie longuement " +
                    "sur Niumi et touche le cadenas. C’est à faire une seule fois."
            }
        }

    /** Libellé du bouton d'action, lu tel quel par TalkBack (§15). */
    fun actionLabelFor(id: ReadinessCheckId): String =
        when (id) {
            ReadinessCheckId.NFC_PRESENT -> "Appareil non compatible"
            ReadinessCheckId.NFC_ENABLED -> "Ouvrir les réglages NFC"
            ReadinessCheckId.PAIRED_BOX -> "Associer un boîtier"
            ReadinessCheckId.APP_SELECTION -> "Choisir mes applications"
            ReadinessCheckId.EXACT_ALARM -> "Comprendre ce blocage"
            ReadinessCheckId.NOTIFICATIONS -> "Autoriser les notifications"
            ReadinessCheckId.ALARM_CHANNEL -> "Ouvrir les réglages du canal"
            ReadinessCheckId.ALARM_VOLUME -> "Ouvrir les réglages du son"
            ReadinessCheckId.DND_TOTAL_SILENCE -> "Ouvrir les réglages Ne pas déranger"
            ReadinessCheckId.DND_OTHER_MODE -> "Ouvrir les réglages Ne pas déranger"
            ReadinessCheckId.ACCESSIBILITY_SERVICE -> "Ouvrir les réglages d’accessibilité"
            ReadinessCheckId.FUTURE_TRIGGER -> "Corriger l’heure"
            ReadinessCheckId.BATTERY_OPTIMIZATION -> "Lever les restrictions de batterie"
            ReadinessCheckId.RECENTS_LOCK -> "J’ai verrouillé Niumi"
        }

    /**
     * `ShowExactAlarmDiagnostic` : Niumi déclare `USE_EXACT_ALARM` et jamais
     * `SCHEDULE_EXACT_ALARM` (§13). Il n'existe donc **aucun** réglage utilisateur à ouvrir, et ce
     * texte remplace l'action au lieu de rediriger vers « Alarmes et rappels ».
     */
    const val EXACT_ALARM_DIAGNOSTIC =
        "Niumi déclare cet accès à l’installation, il n’y a pas de réglage à changer. Un refus " +
            "vient d’une incompatibilité de l’appareil ou d’une restriction de sa surcouche."
}
