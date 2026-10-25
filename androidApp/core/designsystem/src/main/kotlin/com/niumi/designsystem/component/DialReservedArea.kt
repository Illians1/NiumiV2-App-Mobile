package com.niumi.designsystem.component

import androidx.compose.ui.unit.Dp

/**
 * Zone que les étiquettes du cadran doivent laisser libre en haut à droite de l'écran : un carré de
 * [size] à [inset] du bord droit de l'écran, dont le haut est [above] au-dessus du haut du cadran
 * (l'icône des réglages de l'accueil, maquettes 15 à 20 ; le cadran est descendu sous elle depuis
 * le 2026-10-25). Le cadran suppose les marges d'écran de 24 sur lesquelles il déborde de 12.
 */
data class DialReservedArea(
    val size: Dp,
    val inset: Dp,
    val above: Dp,
)
