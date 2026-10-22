package com.niumi.designsystem.component

/** Ce que montre la feuille de scan : « Prêt à scanner », ou un échec avec ses textes. */
sealed interface NfcScanSheetContent {
    data object Ready : NfcScanSheetContent

    /** Textes d'un échec, fournis par l'écran : ils changent d'un écran à l'autre (E7, M6). */
    data class Failure(
        val title: String,
        val help: String,
        val actionLabel: String,
    ) : NfcScanSheetContent
}
