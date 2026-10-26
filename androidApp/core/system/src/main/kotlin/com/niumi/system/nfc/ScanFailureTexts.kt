package com.niumi.system.nfc

/** Titre, phrase d'aide et bouton d'une feuille d'échec du scan. */
data class ScanFailureCopy(
    val title: String,
    val help: String,
    val actionLabel: String,
)

/**
 * Textes des échecs du scan (`design-plan/SPECIFICATION.md` §14, tableau des échecs ;
 * `TEXTES-DES-MAQUETTES.md`), apostrophe typographique (D13). Les mêmes à l'écran 10 et sur la
 * feuille d'annulation (§11) : ils vivent ici depuis l'étape 46, comme `ReadinessMessages`, pour
 * que `:feature:ringing` et `:feature:session` ne les recopient pas. Vérifié par
 * `ScanFailureTextsTest`.
 */
object ScanFailureTexts {
    /** Nom de la feuille d'échec pour TalkBack (`aria-label` des maquettes). */
    const val FAILURE_PANE = "Échec du scan"

    private const val NOT_A_NIUMI_SPOT = "Ce n’est pas un Niumi Spot."
    private const val NOT_REGISTERED = "Ce Niumi Spot n’est pas enregistré."
    private const val SCAN_A_REGISTERED_POINT = "Scanne un Niumi Spot que tu as enregistré."
    private const val UNREADABLE = "Ton Niumi Spot n’a pas pu être lu."
    private const val UNREADABLE_HELP = "Rapproche ton téléphone et réessaie."
    private const val NFC_DISABLED = "Le NFC est coupé."
    private const val NFC_DISABLED_HELP = "Active-le pour scanner ton Niumi Spot."
    private const val RETRY = "Réessayer"
    private const val OPEN_SETTINGS = "Ouvrir les réglages"

    fun of(reason: ScanFailureReason): ScanFailureCopy =
        when (reason) {
            ScanFailureReason.NOT_A_NIUMI_SPOT -> ScanFailureCopy(NOT_A_NIUMI_SPOT, SCAN_A_REGISTERED_POINT, RETRY)
            ScanFailureReason.NOT_REGISTERED -> ScanFailureCopy(NOT_REGISTERED, SCAN_A_REGISTERED_POINT, RETRY)
            ScanFailureReason.UNREADABLE -> ScanFailureCopy(UNREADABLE, UNREADABLE_HELP, RETRY)
            ScanFailureReason.NFC_DISABLED -> ScanFailureCopy(NFC_DISABLED, NFC_DISABLED_HELP, OPEN_SETTINGS)
        }
}
