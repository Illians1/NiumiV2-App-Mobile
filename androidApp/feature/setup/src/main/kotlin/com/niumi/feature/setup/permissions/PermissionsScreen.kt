package com.niumi.feature.setup.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.R
import com.niumi.designsystem.component.NiumiAlertDialog
import com.niumi.designsystem.component.NiumiPrimaryButton
import com.niumi.designsystem.component.NiumiScreenScaffold
import com.niumi.designsystem.component.NiumiTopBar
import com.niumi.designsystem.component.niumiBottomScrollFade
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType
import com.niumi.feature.setup.accessibility.AccessibilityConsentTexts
import com.niumi.system.permissions.NiumiPermission
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessMessages

/**
 * Écran 2 · Autorisations (`SPECIFICATION.md` §5, maquette 03), sans état : flèche de retour, titre
 * en 28 (D19) qui compte les tuiles, texte, tuiles, puis « Continuer », inactif tant qu'une tuile
 * n'est pas accordée, toujours à 16 au moins de la dernière tuile (décision du 2026-10-22, écart à
 * la maquette). Le contenu défile si l'écran est trop petit ; les tuiles cachées sous le bouton
 * s'estompent alors dans le fond (`niumiBottomScrollFade`, décision du 2026-10-22). Les tuiles « Batterie » et
 * « Verrouiller Niumi » (Android, décisions des 2026-10-21 et 22) n'ont pas de maquette : elles
 * reprennent la tuile du design system. Les fenêtres (consentement, verrou, alarme exacte) sont
 * la fenêtre d'alerte de la maquette 57 dans la mise en page de Mehdi du 2026-10-22 (croix ronde,
 * icône de la tuile), et les textes de SPEC_ANDROID §12.3 et §13.
 */
@Composable
fun PermissionsScreen(
    state: PermissionsUiState,
    actions: PermissionsActions,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    val scrollState = rememberScrollState()
    NiumiScreenScaffold(
        modifier = modifier,
        bottom = {
            // Hors de la colonne qui défile : l'écart reste visible quand les tuiles défilent.
            Spacer(modifier = Modifier.height(NiumiDimens.permissionsButtonTop))
            NiumiPrimaryButton(
                text = PermissionsTexts.CONTINUE,
                onClick = actions.onContinue,
                enabled = state.canContinue,
            )
        },
    ) {
        NiumiTopBar(onBack = actions.onBack, backLabel = PermissionsTexts.BACK, title = null)
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Des tuiles cachées sous « Continuer » s'estompent : il reste à faire défiler.
                    .niumiBottomScrollFade(scrollState)
                    .verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(NiumiDimens.permissionsTitleTop))
            // Le titre compte les tuiles affichées ; avant la première lecture, ce nombre n'est pas
            // connu et aucun titre n'est affirmé.
            if (!state.isLoading) {
                Text(
                    text = PermissionsTexts.title(tileCount = state.tiles.size),
                    style = NiumiType.screenTitle28,
                    color = palette.ink,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Spacer(modifier = Modifier.height(NiumiDimens.permissionsTitleBodyGap))
            Text(text = PermissionsTexts.BODY, style = NiumiType.chip15, color = palette.textSecondary)
            Spacer(modifier = Modifier.height(NiumiDimens.permissionsTilesTop))
            Column(verticalArrangement = Arrangement.spacedBy(NiumiDimens.permissionTileSpacing)) {
                state.tiles.forEach { tile ->
                    val content = TILES.getValue(tile.permission)
                    PermissionTile(
                        content = content,
                        granted = tile.granted,
                        onAuthorize = { actions.onAuthorize(tile.permission) },
                    )
                }
            }
        }
    }
    state.dialog?.let { dialog -> PermissionDialogContent(dialog, actions) }
}

/**
 * Une fenêtre par manque qui ne se règle pas d'un seul geste système (décision du 2026-10-21 :
 * fenêtre centrée, croix et toucher à côté pour fermer, sans lien). Le consentement de §12.3
 * précède toujours les réglages d'accessibilité : son bouton est le seul à les ouvrir.
 */
@Composable
private fun PermissionDialogContent(
    dialog: PermissionDialog,
    actions: PermissionsActions,
) {
    val palette = LocalNiumiPalette.current
    when (dialog) {
        PermissionDialog.CONSENT -> {
            NiumiAlertDialog(
                onDismiss = actions.onDismissDialog,
                title = AccessibilityConsentTexts.TITLE,
                icon = R.drawable.ic_permission_blocking,
                closeLabel = PermissionsTexts.CLOSE,
                primary = AccessibilityConsentTexts.OPEN_SETTINGS_BUTTON_LABEL,
                onPrimary = actions.onOpenAccessibilitySettings,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(NiumiDimens.alertTitleBodyGap)) {
                    AccessibilityConsentTexts.points.forEach { point ->
                        Text(text = point, style = NiumiType.body16, color = palette.ink)
                    }
                    // Le chemin, détaché de la divulgation : ce que l'utilisateur va faire ensuite.
                    Text(
                        text = AccessibilityConsentTexts.HOW_TO,
                        style = NiumiType.label16,
                        color = palette.ink,
                        modifier = Modifier.padding(top = NiumiDimens.alertTitleBodyGap),
                    )
                }
            }
        }

        PermissionDialog.RECENTS_LOCK -> {
            NiumiAlertDialog(
                onDismiss = actions.onDismissDialog,
                title = PermissionsTexts.RECENTS_LOCK_NAME,
                icon = R.drawable.ic_permission_recents_lock,
                closeLabel = PermissionsTexts.CLOSE,
                primary = ReadinessMessages.actionLabelFor(ReadinessCheckId.RECENTS_LOCK),
                onPrimary = actions.onConfirmRecentsLock,
            ) {
                Text(
                    text = ReadinessMessages.forCheck(ReadinessCheckId.RECENTS_LOCK),
                    style = NiumiType.body16,
                    color = palette.textSecondary,
                )
            }
        }

        // Rien à ouvrir : Niumi déclare `USE_EXACT_ALARM` (§13), la fenêtre explique seulement.
        PermissionDialog.EXACT_ALARM -> {
            NiumiAlertDialog(
                onDismiss = actions.onDismissDialog,
                title = PermissionsTexts.ALARM_NAME,
                icon = R.drawable.ic_permission_alarm,
                closeLabel = PermissionsTexts.CLOSE,
            ) {
                Text(
                    text = ReadinessMessages.EXACT_ALARM_DIAGNOSTIC,
                    style = NiumiType.body16,
                    color = palette.textSecondary,
                )
            }
        }
    }
}

private val TILES: Map<NiumiPermission, TileContent> =
    mapOf(
        NiumiPermission.ALARM to
            TileContent(PermissionsTexts.ALARM_NAME, PermissionsTexts.ALARM_PHRASE, R.drawable.ic_permission_alarm),
        NiumiPermission.APP_BLOCKING to
            TileContent(
                PermissionsTexts.BLOCKING_NAME,
                PermissionsTexts.BLOCKING_PHRASE,
                R.drawable.ic_permission_blocking,
            ),
        NiumiPermission.BATTERY to
            TileContent(
                PermissionsTexts.BATTERY_NAME,
                PermissionsTexts.BATTERY_PHRASE,
                R.drawable.ic_permission_battery,
            ),
        NiumiPermission.RECENTS_LOCK to
            TileContent(
                PermissionsTexts.RECENTS_LOCK_NAME,
                PermissionsTexts.RECENTS_LOCK_PHRASE,
                R.drawable.ic_permission_recents_lock,
            ),
        NiumiPermission.NOTIFICATIONS to
            TileContent(
                PermissionsTexts.NOTIFICATIONS_NAME,
                PermissionsTexts.NOTIFICATIONS_PHRASE,
                R.drawable.ic_permission_notifications,
            ),
    )

private val previewState =
    PermissionsUiState(
        tiles =
            listOf(
                PermissionTileState(NiumiPermission.ALARM, granted = true),
                PermissionTileState(NiumiPermission.APP_BLOCKING, granted = false),
                PermissionTileState(NiumiPermission.BATTERY, granted = false),
                PermissionTileState(NiumiPermission.RECENTS_LOCK, granted = true),
                PermissionTileState(NiumiPermission.NOTIFICATIONS, granted = false),
            ),
        isLoading = false,
    )

private val noActions = PermissionsActions(onBack = {}, onAuthorize = {}, onContinue = {})

@Preview(name = "Android, cinq tuiles", widthDp = 390, heightDp = 844)
@Composable
private fun PermissionsScreenPreview() {
    NiumiTheme(dark = false) { PermissionsScreen(state = previewState, actions = noActions) }
}

@Preview(name = "Petit écran", widthDp = 320, heightDp = 568)
@Composable
private fun PermissionsSmallScreenPreview() {
    NiumiTheme(dark = false) { PermissionsScreen(state = previewState, actions = noActions) }
}
