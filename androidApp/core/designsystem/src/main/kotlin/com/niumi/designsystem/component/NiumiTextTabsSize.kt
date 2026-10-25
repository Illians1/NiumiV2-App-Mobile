package com.niumi.designsystem.component

import androidx.compose.ui.text.TextStyle
import com.niumi.designsystem.ui.theme.NiumiType

/** Taille des onglets : 14 sur l'accueil (« Ponctuel / Quotidien »), 15 dans le sélecteur d'heure. */
enum class NiumiTextTabsSize(
    val regular: TextStyle,
    val selected: TextStyle,
) {
    Home(NiumiType.tab14, NiumiType.tabSelected14),
    Picker(NiumiType.chip15, NiumiType.chipSelected15),
}
