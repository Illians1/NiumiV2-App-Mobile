package com.niumi.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.niumi.designsystem.R
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Fenêtre d'alerte (maquette 57, VALEURS-DE-DESIGN.md §2, §4) : surface blanche centrée sur le
 * voile, 28 de marge de chaque côté, coins de 24, ombre portée. Mise en page de la maquette de
 * Mehdi du 2026-10-22 pour les fenêtres de l'écran 2 : croix ronde au coin haut droit si
 * [closeLabel] est donné, [icon] dans un rond gris, titre 22/600 centré sur autant de lignes qu'il
 * en faut, puis [content] aligné à gauche, le bouton plein de 52 ([primary]) et le lien discret
 * ([secondaryLink], « Plus tard » dans la maquette 57).
 *
 * Toucher le voile, la croix ou revenir en arrière appelle [onDismiss] ; toucher la fenêtre
 * elle-même ne ferme rien. Le voile est celui du thème (`veil`), pas l'assombrissement du système.
 */
@Suppress("LongParameterList") // Convention Compose : les paramètres d'un composant sont son API.
@Composable
fun NiumiAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    closeLabel: String? = null,
    primary: String? = null,
    onPrimary: () -> Unit = {},
    secondaryLink: String? = null,
    onSecondary: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalNiumiPalette.current
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnClickOutside = false,
            ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(palette.veil)
                    .pointerInput(onDismiss) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.Center,
        ) {
            AlertSurface(
                title = title,
                icon = icon,
                closeLabel = closeLabel,
                onClose = onDismiss,
                primary = primary,
                onPrimary = onPrimary,
                secondaryLink = secondaryLink,
                onSecondary = onSecondary,
                modifier = modifier,
                content = content,
            )
        }
    }
}

/** La surface blanche de la fenêtre : croix au coin, icône, titre, corps défilant, bouton et lien. */
@Suppress("LongParameterList")
@Composable
private fun AlertSurface(
    title: String,
    @DrawableRes icon: Int?,
    closeLabel: String?,
    onClose: () -> Unit,
    primary: String?,
    onPrimary: () -> Unit,
    secondaryLink: String?,
    onSecondary: () -> Unit,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            modifier
                .padding(horizontal = NiumiDimens.alertHorizontalMargin)
                .fillMaxWidth()
                .shadow(NiumiDimens.alertElevation, NiumiShapes.alert)
                .clip(NiumiShapes.alert)
                .background(palette.surface)
                // Un toucher sur la fenêtre est absorbé : seul le voile ferme.
                .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(NiumiDimens.alertPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                AlertIcon(icon)
                Spacer(modifier = Modifier.height(NiumiDimens.alertIconTitleGap))
            }
            // Sans icône, le titre s'écarte de la croix des deux côtés pour rester centré.
            val titleInset = if (icon == null && closeLabel != null) NiumiDimens.touchTarget else 0.dp
            Text(
                text = title,
                style = NiumiType.alertTitle22,
                color = palette.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = titleInset).semantics { heading() },
            )
            Spacer(modifier = Modifier.height(NiumiDimens.alertTitleBodyGap))
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                content = content,
            )
            if (primary != null) {
                Spacer(modifier = Modifier.height(NiumiDimens.alertBodyButtonGap))
                NiumiPrimaryButton(text = primary, onClick = onPrimary, size = NiumiButtonSize.Sheet)
            }
            if (secondaryLink != null) NiumiDiscreetLink(text = secondaryLink, onClick = onSecondary)
        }
        if (closeLabel != null) {
            NiumiRoundCloseButton(
                label = closeLabel,
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(NiumiDimens.alertCloseInset),
            )
        }
    }
}

/** Icône de la fenêtre, décorative : le titre dit déjà de quoi il s'agit. */
@Composable
private fun AlertIcon(
    @DrawableRes icon: Int,
) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier = Modifier.size(NiumiDimens.alertIconCircle).clip(CircleShape).background(palette.tileGrey),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = palette.ink,
            modifier = Modifier.size(NiumiDimens.permissionTileIcon),
        )
    }
}

@Preview(name = "Avec croix et bouton", widthDp = 390, heightDp = 844)
@Composable
private fun NiumiAlertDialogPreview() {
    NiumiTheme(dark = false) {
        NiumiAlertDialog(
            onDismiss = {},
            title = "Verrouiller Niumi",
            icon = R.drawable.ic_permission_recents_lock,
            closeLabel = "Fermer",
            primary = "J’ai verrouillé Niumi",
        ) {
            Text(
                text = "Ouvre les applications récentes, appuie longuement sur Niumi et touche le cadenas.",
                style = NiumiType.body16,
                color = LocalNiumiPalette.current.textSecondary,
            )
        }
    }
}
