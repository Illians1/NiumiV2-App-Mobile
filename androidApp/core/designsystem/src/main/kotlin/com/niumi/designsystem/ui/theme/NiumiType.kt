package com.niumi.designsystem.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Échelle de texte relevée dans les 58 maquettes (VALEURS-DE-DESIGN.md §3,
 * `valeurs-de-design.json` → `tailles`). Le nom de chaque style porte sa taille ; le commentaire,
 * la ligne du tableau §3. Interligne = taille × facteur de la maquette ; interlettrage en sp, comme
 * le px CSS des sources. Les styles sans interligne ou sans interlettrage dans le design les
 * laissent non spécifiés. `NiumiTypeTest` compare chaque valeur au JSON.
 */
object NiumiType {
    /** 112/500 — heure de l'écran de réveil. Interligne 1, interlettrage −4. */
    val clock112 = style(112, FontWeight.Medium, lineHeight = 112.sp, letterSpacing = (-4).sp)

    /** 42/500 — chiffres au centre du cadran (« 9 h 26 »). Interlettrage −1. */
    val dial42 = style(42, FontWeight.Medium, letterSpacing = (-1).sp)

    /** 40/600 — titre de l'écran 11, valeur du milieu de la roue. Interligne 1,1, interlettrage −1. */
    val title40 = style(40, FontWeight.SemiBold, lineHeight = 44.sp, letterSpacing = (-1).sp)

    /** 30/600 — consigne de l'écran de réveil. Interligne 1,15, interlettrage −0,5. */
    val instruction30 = style(30, FontWeight.SemiBold, lineHeight = 34.5.sp, letterSpacing = (-0.5).sp)

    /** 30/500 — « Désactivé » au centre du cadran. */
    val dialDisabled30 = style(30, FontWeight.Medium)

    /** 28/600 — titres d'écran, écran 2 compris (D19). Interligne 1,15, interlettrage −0,5. */
    val screenTitle28 = style(28, FontWeight.SemiBold, lineHeight = 32.2.sp, letterSpacing = (-0.5).sp)

    /** 26/400 — « Prêt à scanner », feuille de scan. */
    val scanSheetTitle26 = style(26, FontWeight.Normal)

    /** 24/600 — titres des feuilles et des échecs, titre de l'écran de blocage. Interligne 1,2, interlettrage −0,5. */
    val sheetTitle24 = style(24, FontWeight.SemiBold, lineHeight = 28.8.sp, letterSpacing = (-0.5).sp)

    /** 22/600 — titre de la feuille « Sonnerie », de l'alerte d'autorisations. Interligne 1,2, interlettrage −0,3. */
    val alertTitle22 = style(22, FontWeight.SemiBold, lineHeight = 26.4.sp, letterSpacing = (-0.3).sp)

    /** 22/500 — consigne de l'écran 3. Interligne 1,3. */
    val instruction22 = style(22, FontWeight.Medium, lineHeight = 28.6.sp)

    /** 20/600 — titres de carte et de liste. */
    val cardTitle20 = style(20, FontWeight.SemiBold)

    /** 20/500 — sous-titre de l'écran 11, phrase du guide des autorisations. */
    val subtitle20 = style(20, FontWeight.Medium)

    /** 17/500 — texte des boutons principaux. */
    val button17 = style(17, FontWeight.Medium)

    /** 17/600 — heures autour du cadran, consigne de la recopie, intertitres de l'aide. */
    val dialHour17 = style(17, FontWeight.SemiBold)

    /** 17/400 — texte sous les titres de l'écran 1, phrase de la feuille de scan. */
    val lead17 = style(17, FontWeight.Normal)

    /** 16/600 — noms des autorisations, « Blocage en cours », son choisi. */
    val labelStrong16 = style(16, FontWeight.SemiBold)

    /** 16/500 — « Réveil activé », jours cochés, pilule « Modifier », bouton de la recopie. */
    val label16 = style(16, FontWeight.Medium)

    /** 16/400 — texte courant, noms des pièces. Interligne 1,35. */
    val body16 = style(16, FontWeight.Normal, lineHeight = 21.6.sp)

    /** 15/400 — pastilles de durée, lien « Annuler la session ». */
    val chip15 = style(15, FontWeight.Normal)

    /** 15/500 — pastille de durée choisie, bouton « Autoriser ». */
    val chipSelected15 = style(15, FontWeight.Medium)

    /** 14/400 — phrases des tuiles, deuxième ligne des cartes, liens discrets. Interligne 1,35. */
    val secondary14 = style(14, FontWeight.Normal, lineHeight = 18.9.sp)

    /** 13/400 — légendes, intitulés de section, mentions de bas d'écran. */
    val caption13 = style(13, FontWeight.Normal)

    /** 12/400 — noms sous les icônes d'applications. */
    val appLabel12 = style(12, FontWeight.Normal)

    /** 12/600 — nom sous l'icône d'une application cochée. */
    val appLabelChecked12 = style(12, FontWeight.SemiBold)

    /**
     * Typographie Material 3 des composants Material encore présents (champ de recherche, feuille
     * modale, anciens écrans jusqu'à leur refonte) : chaque rôle reprend un style Niumi, aucune
     * taille Material par défaut ne subsiste. `displayLarge` est `dial42` et non `clock112` : le
     * `TimePicker` Material (écran 5, jusqu'à l'étape 45) l'écrit dans des champs de 96 × 80 dp
     * coupés à leur forme ; l'écran de réveil lit `clock112` directement.
     */
    fun material(): Typography =
        Typography(
            displayLarge = dial42,
            displayMedium = dial42,
            displaySmall = title40,
            headlineLarge = instruction30,
            headlineMedium = screenTitle28,
            headlineSmall = sheetTitle24,
            titleLarge = cardTitle20,
            titleMedium = labelStrong16,
            titleSmall = label16,
            bodyLarge = lead17,
            bodyMedium = body16,
            bodySmall = secondary14,
            labelLarge = button17,
            labelMedium = chip15,
            labelSmall = appLabel12,
        )

    private fun style(
        size: Int,
        weight: FontWeight,
        lineHeight: TextUnit = TextUnit.Unspecified,
        letterSpacing: TextUnit = TextUnit.Unspecified,
    ) = TextStyle(
        fontFamily = NiumiFont.PlexSans,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
    )
}
