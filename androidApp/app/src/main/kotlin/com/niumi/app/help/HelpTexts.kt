package com.niumi.app.help

/** Une section de l'écran d'aide : un titre, et les limites qu'il regroupe. */
data class HelpSection(
    val title: String,
    val items: List<String>,
)

/**
 * Contenu de l'écran 13 « Aide et limites » (SPEC_ANDROID §15, étape 21). `object` pur, testable
 * en JVM, comme `OnboardingTexts` et `AccessibilityConsentTexts`.
 *
 * **Ce texte est celui de `docs/android/LIMITES.md`**, mot pour mot, sections et puces comprises.
 * `HelpTextsTest` compare les deux à chaque build : le document est la version lisible hors de
 * l'application — `PRIVACY_POLICY.md` y renvoie le lecteur de la fiche Play — et l'écran en est
 * la restitution, jamais une reformulation.
 *
 * Les six limites de [com.niumi.feature.setup.onboarding.OnboardingTexts] s'y retrouvent à
 * l'identique : l'aide est un sur-ensemble de l'onboarding, pas un second discours sur les mêmes
 * faits.
 */
object HelpTexts {
    const val TITLE = "Aide et limites"

    const val INTRO =
        "Niumi te réveille et bloque les applications que tu choisis jusqu'au scan de ton " +
            "boîtier. Voilà ce qu'il ne peut pas garantir, et pourquoi."

    val sections: List<HelpSection> =
        listOf(
            HelpSection(
                title = "Ce que Niumi ne peut pas garantir",
                items =
                    listOf(
                        "Un arrêt forcé depuis les réglages supprime le réveil programmé : Niumi ne sonnera pas.",
                        "Le système peut arrêter le service qui fait sonner l'alarme. Niumi ne peut pas l'en " +
                            "empêcher.",
                        "Le mode Ne pas déranger en silence total coupe le son de l'alarme et empêche l'écran " +
                            "de réveil de s'afficher. Niumi refuse d'activer une session dans cet état, et ne " +
                            "modifie jamais ce réglage à ta place.",
                        "Le service d'accessibilité peut être désactivé à tout moment dans les réglages " +
                            "Android, ce qui arrête le blocage.",
                        "Le blocage renvoie les applications choisies à l'accueil ; il ne les rend pas " +
                            "impossibles à ouvrir. Désactiver le service, arrêter Niumi ou le désinstaller suffit à " +
                            "le contourner.",
                        "Le scan sur écran verrouillé n'est pas garanti : ton téléphone peut exiger un " +
                            "déverrouillage avant de lire le boîtier.",
                        "Il n'existe aucun secours logiciel pendant une session : ni code, ni délai, ni bouton " +
                            "« Arrêter quand même ». Sans ton boîtier, il te reste l'arrêt forcé ou l'extinction du " +
                            "téléphone.",
                        "Si une permission est retirée ou le volume d'alarme coupé après l'activation, Niumi le " +
                            "signale par un incident. Il ne peut pas le corriger seul.",
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
                            "revérifie avant chaque session.",
                        "Juste après un redémarrage, le diagnostic peut annoncer le NFC désactivé alors qu'il " +
                            "ne l'est pas : la pile NFC du système n'a pas fini de démarrer. Rouvrir Niumi corrige " +
                            "l'affichage.",
                    ),
            ),
            HelpSection(
                title = "Notifications et avertissements",
                items =
                    listOf(
                        "La notification qui te demande de scanner ton boîtier peut être balayée. Depuis " +
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
