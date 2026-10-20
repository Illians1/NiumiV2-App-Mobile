package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme

/**
 * Cadre des `@Preview` des composants : le thème demandé, son fond d'écran et les marges latérales
 * des maquettes. Chaque composant a un aperçu clair et un aperçu sombre (étape 34).
 */
@Composable
internal fun NiumiPreviewFrame(
    dark: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    NiumiTheme(dark = dark) {
        Column(
            modifier =
                Modifier
                    .background(LocalNiumiPalette.current.background)
                    .padding(NiumiDimens.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(NiumiDimens.trailingGap),
            content = content,
        )
    }
}
