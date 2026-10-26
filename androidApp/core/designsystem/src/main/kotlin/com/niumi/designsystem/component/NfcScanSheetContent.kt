package com.niumi.designsystem.component

/**
 * Ce que montre la feuille de scan : « Prêt à scanner », un échec avec ses textes, ou le cercle de
 * chargement de la libération (étape 46).
 */
sealed interface NfcScanSheetContent {
    data object Ready : NfcScanSheetContent

    /** Textes d'un échec, fournis par l'écran : ils changent d'un écran à l'autre (E7, M6). */
    data class Failure(
        val title: String,
        val help: String,
        val actionLabel: String,
    ) : NfcScanSheetContent

    /** Après un scan accepté, le temps de la libération : cercle seul, lu par TalkBack ([label]). */
    data class Releasing(
        val label: String,
    ) : NfcScanSheetContent
}
