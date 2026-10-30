package com.niumi.designsystem.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Les deux jeux de couleurs de Niumi. Seul fichier de l'app où une couleur s'écrit en dur
 * (`Color(0x…)`) : les valeurs sont celles de `design-plan/valeurs-de-design.json`, et
 * `NiumiPaletteTest` compare chacune au JSON. Les commentaires citent la ligne de
 * `VALEURS-DE-DESIGN.md` §2 d'où vient la valeur, ou la décision qui la fixe quand le design n'en
 * donne pas pour ce thème.
 */
object NiumiPalettes {
    val Light =
        NiumiPalette(
            // « Fond d'écran » : #FAFAF8, celui de toutes les maquettes claires (accueil en cartes
            // compris depuis la dernière version des maquettes du 2026-10-30, après des essais de
            // #ECEDEE, #BFC3C8 et d'un anthracite).
            background = Color(0xFFFAFAF8),
            // « Surface »
            surface = Color(0xFFFFFFFF),
            // « Encre »
            ink = Color(0xFF0E0E10),
            // « Texte secondaire »
            textSecondary = Color(0xFF5C5C60),
            // « Bordure »
            border = Color(0xFF85858A),
            // « Séparateur »
            separator = Color(0xFFDCDCD8),
            // « Piste »
            track = Color(0xFFE8E8E4),
            // « Désactivé »
            disabled = Color(0xFFC6C6C2),
            // « Tuile grise »
            tileGrey = Color(0xFFEFEFEB),
            // « Case grisée »
            cellGrey = Color(0xFFF1F1EE),
            // « Ambre » ; règle de l'ambre validée le 2026-10-19 (D14) : jamais de texte sur fond clair.
            amber = Color(0xFFE9A23B),
            // « Teinte ambre »
            amberTint = Color(0xFFFCF1DF),
            // « Texte sur encre » : #FFFFFF sur le bouton principal. Le #FAFAF8 de la carte noire
            // n'en est pas distingué (« l'écart entre les deux ne se voit pas à l'œil »).
            onInk = Color(0xFFFFFFFF),
            // « Texte secondaire sur encre »
            onInkSecondary = Color(0xFFA9A9AE),
            // « Voile » : noir à 45 %.
            veil = Color.Black.copy(alpha = 0.45f),
            // Aucun liseré de feuille en clair (le liseré n'existe que dans le tableau sombre).
            sheetOutline = Color.Transparent,
            // Aucun bord de menu en clair : le menu ne se détache que par son ombre (VALEURS §4).
            menuBorder = Color.Transparent,
            // VALEURS §4, « Annuler » de la feuille de scan : #E8E8E4 (= piste).
            cancelButton = Color(0xFFE8E8E4),
            // Bouton inactif : fond #E8E8E4 (D9, tranché le 2026-10-19).
            disabledFill = Color(0xFFE8E8E4),
            // Bouton inactif : texte #5C5C60 (D9, tranché le 2026-10-19).
            disabledText = Color(0xFF5C5C60),
            // Le mot signalé n'est dessiné qu'en sombre ; en clair, la teinte ambre.
            mismatchTint = Color(0xFFFCF1DF),
            // « Désactivé » : bord des jours non cochés de l'accueil « Réveil désactivé » (maquette 13).
            disabledBorder = Color(0xFFC6C6C2),
            // Cartes de l'accueil (maquettes 15 à 29 et 61 à 65 du 2026-10-30) : ombre noire en deux
            // couches (`NiumiDimens.cardShadow`) ; les cartes de section n'ont pas de bord.
            sectionCardOutline = Color.Transparent,
            sectionCardShadow = Color.Black,
        )

    val Dark =
        NiumiPalette(
            // « Fond d'écran »
            background = Color(0xFF0E0E10),
            // « Surface »
            surface = Color(0xFF1B1B1F),
            // « Texte principal »
            ink = Color(0xFFFAFAF8),
            // « Texte secondaire »
            textSecondary = Color(0xFFA9A9AE),
            // « Bordure, texte désactivé »
            border = Color(0xFF74747B),
            // « Liseré de feuille, séparateur »
            separator = Color(0xFF2E2E33),
            // « Piste »
            track = Color(0xFF2A2A2F),
            // « Fond désactivé » : jours grisés pendant la session.
            disabled = Color(0xFF4A4A50),
            // « Piste » : rond de l'icône de scan.
            tileGrey = Color(0xFF2A2A2F),
            // Aucune case grisée dessinée en sombre : la piste.
            cellGrey = Color(0xFF2A2A2F),
            // « Ambre »
            amber = Color(0xFFE9A23B),
            // Aucune teinte ambre dessinée en sombre : la teinte du mot signalé.
            amberTint = Color(0xFF3D2C0E),
            // « Texte sur bouton principal »
            onInk = Color(0xFF0E0E10),
            // Texte secondaire posé sur le blanc cassé : le texte secondaire du thème clair.
            onInkSecondary = Color(0xFF5C5C60),
            // « Voile » : noir à 60 %.
            veil = Color.Black.copy(alpha = 0.6f),
            // « Liseré de feuille, séparateur »
            sheetOutline = Color(0xFF2E2E33),
            // « Bord de menu »
            menuBorder = Color(0xFF3A3A40),
            // « Bouton gris »
            cancelButton = Color(0xFF34343A),
            // VALEURS §4, bouton inactif sombre : fond #2A2A2F.
            disabledFill = Color(0xFF2A2A2F),
            // VALEURS §4, bouton inactif sombre : texte #74747B.
            disabledText = Color(0xFF74747B),
            // « Teinte du mot signalé »
            mismatchTint = Color(0xFF3D2C0E),
            // « Bord de menu » : aussi le bord des jours non cochés verrouillés en session (maquette 15).
            disabledBorder = Color(0xFF3A3A40),
            // « Bord de carte de section » (maquettes 17, 24 à 26 et 29 du 2026-10-30) : le liseré
            // des feuilles ; aucune ombre en sombre, ni sous les cartes de section ni sous la carte d'état.
            sectionCardOutline = Color(0xFF2E2E33),
            sectionCardShadow = Color.Transparent,
        )

    /**
     * Dégradé du soleil de l'écran 11, de haut en bas (position, couleur) : le seul dégradé des
     * écrans clairs (VALEURS §2).
     */
    val SunGradient: List<Pair<Float, Color>> =
        listOf(
            0f to Color(0xFFE9A23B),
            0.26f to Color(0xFFEFB65F),
            0.58f to Color(0xFFF8DDB0),
        )
}

/**
 * Palette du thème courant, fournie par `NiumiTheme(dark)`. La valeur par défaut (clair) ne sert
 * qu'aux tests instrumentés qui composent un écran sans `NiumiTheme` ; les deux activités de
 * l'app passent toujours par `NiumiTheme`.
 */
val LocalNiumiPalette = staticCompositionLocalOf { NiumiPalettes.Light }
