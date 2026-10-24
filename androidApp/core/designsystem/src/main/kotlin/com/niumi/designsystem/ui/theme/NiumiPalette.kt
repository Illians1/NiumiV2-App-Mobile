package com.niumi.designsystem.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Rôles de couleur du design Niumi (`design-plan/VALEURS-DE-DESIGN.md` §2), un jeu par thème
 * ([NiumiPalettes.Light], [NiumiPalettes.Dark]). Les écrans lisent `LocalNiumiPalette.current`,
 * jamais `MaterialTheme.colorScheme` : la piste, la tuile grise, la teinte ambre ou le bouton gris
 * d'annulation n'ont pas d'équivalent Material 3.
 *
 * Le thème suit l'état de la session (`isSessionDark()`), jamais le réglage du téléphone.
 */
@Immutable
@Suppress("LongParameterList") // Un paramètre par rôle du design : les regrouper n'aurait pas de sens.
data class NiumiPalette(
    /** Fond d'écran. */
    val background: Color,
    /** Feuilles du bas, cartes, fenêtre d'alerte, menus. */
    val surface: Color,
    /** Texte principal, bouton principal, carte « Réveil activé », arc du cadran. */
    val ink: Color,
    /** Sous-titres, phrases d'aide, liens discrets. */
    val textSecondary: Color,
    /** Cartes au trait, cases, jours non cochés, pastilles non choisies. */
    val border: Color,
    /** Lignes entre les rangées des listes. */
    val separator: Color,
    /** Piste du cadran. */
    val track: Color,
    /** Éléments grisés (accueil « Réveil désactivé », jours grisés pendant la session). */
    val disabled: Color,
    /** Tuiles d'autorisation, rond de l'icône de scan. */
    val tileGrey: Color,
    /** Emplacements d'icônes d'applications grisés. */
    val cellGrey: Color,
    /** Interrupteur activé, poignée du réveil, point des Niumi Spots, soleil. Jamais de texte en clair (D14). */
    val amber: Color,
    /** Fond de la phrase « Bravo ! » (écran 11). */
    val amberTint: Color,
    /** Texte posé sur l'encre (bouton principal). */
    val onInk: Color,
    /** Texte secondaire posé sur l'encre (deuxième ligne de la carte « Réveil activé »). */
    val onInkSecondary: Color,
    /** Voile derrière une feuille ou une alerte. */
    val veil: Color,
    /** Contour d'un pixel des feuilles du bas. */
    val sheetOutline: Color,
    /** Bord du menu « trois points ». */
    val menuBorder: Color,
    /** Fond du bouton « Annuler » de la feuille de scan. */
    val cancelButton: Color,
    /** Fond du bouton inactif (D9). */
    val disabledFill: Color,
    /** Texte du bouton inactif et libellés grisés (D9). */
    val disabledText: Color,
    /** Fond du mot fautif dans la recopie (écran 9). */
    val mismatchTint: Color,
    /** Bord d'un jour non coché grisé (accueil « Réveil désactivé ») ou verrouillé (session). */
    val disabledBorder: Color,
)
