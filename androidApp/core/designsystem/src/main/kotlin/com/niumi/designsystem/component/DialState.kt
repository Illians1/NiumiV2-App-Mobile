package com.niumi.designsystem.component

/**
 * Ce que montre le cadran de l'accueil (VALEURS-DE-DESIGN.md §5, maquettes 15 à 17). Heures en
 * minutes depuis minuit ; [centerLabel] et [centerValue] sont les deux lignes du centre
 * (« Coucher dans » / « 56 min », « Réveil dans » / « 7 h 50 », « Réveil » / « Désactivé »),
 * [centerValue] vide pour n'en montrer qu'une.
 */
sealed interface DialState {
    val bedtimeMinutes: Int
    val wakeMinutes: Int
    val nowMinutes: Int
    val centerLabel: String
    val centerValue: String

    /** « Réveil prévu » : arc à l'encre, poignée du coucher blanche, poignée du réveil ambre. */
    data class Scheduled(
        override val bedtimeMinutes: Int,
        override val wakeMinutes: Int,
        override val nowMinutes: Int,
        override val centerLabel: String,
        override val centerValue: String,
    ) : DialState

    /** « Réveil désactivé » : tout est grisé, les deux poignées blanches restent déplaçables. */
    data class Disabled(
        override val bedtimeMinutes: Int,
        override val wakeMinutes: Int,
        override val nowMinutes: Int,
        override val centerLabel: String,
        override val centerValue: String,
    ) : DialState

    /** Session en cours (sombre) : pas de poignées, l'arc se remplit d'ambre jusqu'à l'heure actuelle. */
    data class InSession(
        override val bedtimeMinutes: Int,
        override val wakeMinutes: Int,
        override val nowMinutes: Int,
        override val centerLabel: String,
        override val centerValue: String,
    ) : DialState
}

/** Textes du cadran, fournis par l'écran (« Coucher », « Réveil », « Choisir l'heure »). */
data class DialTexts(
    val bedtime: String,
    val wake: String,
    val chooseTime: String,
)
