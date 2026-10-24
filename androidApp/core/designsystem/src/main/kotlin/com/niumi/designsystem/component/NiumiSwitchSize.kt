package com.niumi.designsystem.component

import androidx.compose.ui.unit.Dp
import com.niumi.designsystem.ui.theme.NiumiDimens

/** Les deux tailles d'interrupteur de VALEURS-DE-DESIGN.md §4. */
enum class NiumiSwitchSize(
    val width: Dp,
    val height: Dp,
    val thumb: Dp,
) {
    /** Interrupteur général de l'accueil : 52 × 32, pastille de 26 (maquettes 15 et 16). */
    Main(NiumiDimens.switchWidth, NiumiDimens.switchHeight, NiumiDimens.switchThumb),

    /** Interrupteur de ligne : 44 × 26, pastille de 20 (feuille Sonnerie, maquette 18 ; 2026-10-23). */
    Row(NiumiDimens.switchRowWidth, NiumiDimens.switchRowHeight, NiumiDimens.switchRowThumb),
}
