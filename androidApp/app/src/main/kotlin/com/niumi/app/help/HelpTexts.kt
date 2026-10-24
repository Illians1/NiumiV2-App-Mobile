package com.niumi.app.help

/** Une section de l'écran d'aide : un titre, et les limites qu'il regroupe. */
data class HelpSection(
    val title: String,
    val items: List<String>,
)

/**
 * Contenu de l'écran 13 « Aide et limites » (SPEC_ANDROID §15, étape 21). `object` pur, testable
 * en JVM, comme `PresentationTexts` et `AccessibilityConsentTexts`.
 *
 * **Ce texte est celui de `docs/android/LIMITES.md`**, mot pour mot, sections et puces comprises.
 * `HelpTextsTest` compare les deux à chaque build : le document est la version lisible hors de
 * l'application — `PRIVACY_POLICY.md` y renvoie le lecteur de la fiche Play — et l'écran en est
 * la restitution, jamais une reformulation.
 *
 * Depuis l'étape 35 de la refonte (décision de Mehdi du 2026-10-20), l'aide est le seul écran à
 * porter les limites : l'onboarding qui les énonçait avant le premier réveil a laissé la place à
 * l'écran 1 · Présentation, qui ne les reprend pas.
 */
object HelpTexts {
    const val TITLE = "Aide et limites"

    const val INTRO =
        "Niumi te réveille et bloque les applications que tu choisis jusqu'au scan de ton " +
            "Niumi Spot. Voilà ce qu'il ne peut pas garantir, et pourquoi."

    val sections: List<HelpSection> =
        listOf(
            HelpSection(
                title = "Ce que Niumi ne peut pas garantir",
                items =
                    listOf(
                        "Un arrêt forcé depuis les réglages supprime le réveil programmé : Niumi ne sonnera pas.",
                        "Le système peut arrêter le service qui fait sonner l'alarme. Niumi ne peut pas l'en " +
                            "empêcher.",
                        "Le mode Ne pas déranger en silence total coupe le son et la vibration de l'alarme, et " +
                            "la notification du réveil n'apparaît pas sur l'écran de verrouillage : pour scanner, " +
                            "déverrouille ton téléphone et ouvre Niumi toi-même. Niumi refuse d'activer ton " +
                            "réveil dans cet état, et ne modifie jamais ce réglage à ta place.",
                        "Le service d'accessibilité peut être désactivé à tout moment dans les réglages " +
                            "Android, ce qui arrête le blocage.",
                        "Le blocage renvoie les applications choisies à l'accueil ; il ne les rend pas " +
                            "impossibles à ouvrir. Désactiver le service, arrêter Niumi ou le désinstaller suffit à " +
                            "le contourner.",
                        "Le scan demande un téléphone déverrouillé : quand ça sonne, déverrouille-le, puis " +
                            "touche la notification Niumi ou ouvre l'application.",
                        "Il n'existe aucun secours logiciel une fois ton réveil activé : ni code, ni délai, ni " +
                            "bouton « Arrêter quand même ». Sans ton Niumi Spot, il te reste l'arrêt forcé ou " +
                            "l'extinction du téléphone.",
                        "Si une permission est retirée, le volume d'alarme coupé ou l'exemption d'énergie perdue " +
                            "après l'activation, Niumi le signale par un incident. Il ne peut pas le corriger seul.",
                        "Quand ton réveil est programmé, une notification Niumi reste affichée : tu peux " +
                            "l'écarter sans rien annuler. Sur Xiaomi, « Tout effacer » dans les applications " +
                            "récentes arrête le blocage tant que Niumi n'y est pas verrouillé : le diagnostic le " +
                            "demande une fois. Sans réveil programmé, balayer Niumi dans les applications récentes " +
                            "oblige à réactiver son service d'accessibilité avant le prochain réveil : le " +
                            "diagnostic te le dira.",
                        "Si Niumi est arrêté alors que ton réveil est programmé — plantage ou arrêt forcé — le " +
                            "blocage ne reprend pas tant que tu n'as pas réactivé le service d'accessibilité. " +
                            "Niumi te prévient dès qu'il tourne à nouveau, et un blocage différé commence alors " +
                            "en retard.",
                        "Un blocage différé commence à l'heure choisie tant que le téléphone est allumé, même " +
                            "verrouillé, en veille ou redémarré entre-temps. Si le téléphone est éteint à cette " +
                            "heure-là, le blocage commence dès qu'il redémarre, et Niumi signale le retard.",
                        "À l'heure de début, une application choisie déjà ouverte est renvoyée à l'accueil en " +
                            "moins d'une seconde, sans que tu aies à changer d'écran.",
                    ),
            ),
            HelpSection(
                title = "Niumi Spots",
                items =
                    listOf(
                        "N'importe quel Niumi Spot enregistré au moment du scan arrête le réveil et débloque tes " +
                            "applications. Tu peux en ajouter, en retirer ou en renommer à tout moment, même quand " +
                            "ton réveil est programmé ; il en reste toujours au moins un. Un Niumi Spot retiré " +
                            "n'arrête plus rien.",
                    ),
            ),
            HelpSection(
                title = "Après un redémarrage ou une mise à jour",
                items =
                    listOf(
                        "Un redémarrage ne perd pas ton réveil : Niumi le reprogramme avant même le premier " +
                            "déverrouillage.",
                        "Sur Xiaomi et les surcouches proches, un réglage « Démarrage automatique en " +
                            "arrière-plan » décide si Niumi peut démarrer seul. Mesuré : il ne bloque pas la " +
                            "reprogrammation du réveil après un redémarrage, mais il empêche Niumi de se remettre à " +
                            "jour tout seul après une mise à jour de l'application.",
                        "Une mise à jour de Niumi peut réinitialiser l'exemption d'énergie. Le diagnostic la " +
                            "revérifie à chaque activation de ton réveil, et Niumi te prévient s'il la voit " +
                            "disparaître ensuite.",
                        "Juste après un redémarrage, le diagnostic peut annoncer le NFC désactivé alors qu'il " +
                            "ne l'est pas : la pile NFC du système n'a pas fini de démarrer. Rouvrir Niumi corrige " +
                            "l'affichage.",
                        "Juste après un redémarrage, l'écran de réveil et l'écran de scan peuvent aussi annoncer " +
                            "le NFC désactivé : le message disparaît de lui-même dès que le NFC est prêt, en " +
                            "général une vingtaine de secondes après le déverrouillage, et peut revenir une ou deux " +
                            "secondes pendant que le téléphone relance son NFC. Attends qu'il ait disparu pour " +
                            "scanner ton Niumi Spot.",
                    ),
            ),
            HelpSection(
                title = "Sonnerie",
                items =
                    listOf(
                        "La pré-écoute et l'alarme suivent le volume des alarmes de ton téléphone, jamais " +
                            "celui des médias.",
                        "Si Niumi s'arrête pendant la sonnerie et que le système le relance, le son se coupe une " +
                            "ou deux secondes, puis reprend au volume où il en était, pas depuis le début.",
                    ),
            ),
            HelpSection(
                title = "Notifications et avertissements",
                items =
                    listOf(
                        "Quand ça sonne, Niumi n'ouvre aucun écran de lui-même et n'allume pas l'écran : la " +
                            "notification « Ton réveil Niumi sonne » est visible sur l'écran de verrouillage et " +
                            "dans le volet, et revient en quelques secondes si tu la balaies. Si tu utilises ton " +
                            "téléphone à ce moment-là, sa bannière disparaît au bout de quelques secondes ; le son " +
                            "continue.",
                        "Sur Xiaomi, si les notifications de Niumi sont masquées sur l'écran de verrouillage, la " +
                            "notification du réveil n'y apparaît pas : déverrouille ton téléphone, puis touche-la " +
                            "dans le volet ou ouvre Niumi. Le diagnostic ne peut pas voir ce réglage.",
                        "La notification qui te demande de scanner ton Niumi Spot peut être balayée. Depuis " +
                            "Android 14, Niumi ne peut pas l'en empêcher. Elle revient dès que tu rouvres " +
                            "l'application, et l'écran de blocage te rappelle le scan.",
                        "Si un réglage casse ton réveil après l'activation, Niumi t'avertit au plus tôt, jamais " +
                            "immédiatement : le système peut l'avoir arrêté entre-temps.",
                    ),
            ),
            HelpSection(
                title = "Journal et diagnostic",
                items =
                    listOf(
                        "Le journal technique garde les 200 derniers événements, sur ton téléphone uniquement. " +
                            "Rien n'est envoyé, jamais.",
                        "Les événements écrits avant ton premier déverrouillage après un redémarrage peuvent " +
                            "être perdus si Niumi s'arrête avant que tu déverrouilles. Les incidents, eux, ne sont " +
                            "jamais perdus.",
                    ),
            ),
        )
}
