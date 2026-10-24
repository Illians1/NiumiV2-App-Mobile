package com.niumi.feature.setup.pairing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.settingsIntentFor

/**
 * Écran 3 — « Mes boîtiers » (SPEC_ANDROID §11.1, §15 Lot 8). Composable pur et sans état, testé
 * en isolation : aucun clic n'y est simulé, seuls les textes et l'état des actions sont vérifiés.
 *
 * Liste sans carte, l'espace sépare les lignes ; nom en `titleMedium`, préfixe en `bodySmall`.
 * Depuis l'étape 33, « Continuer » et la confirmation d'un dialogue sont à l'encre (bouton
 * principal du design-plan) ; les actions de ligne, « Ajouter un boîtier » et « Annuler » prennent
 * la couleur du texte ([neutralTextButton], [neutralOutlinedButton]). L'ambre ne porte jamais de
 * texte sur fond clair (`design-plan/VALEURS-DE-DESIGN.md` §2, D14). L'écran est remplacé à
 * l'étape 47. Le token n'apparaît nulle part, ni son empreinte : seul un préfixe de `boxId` est
 * affiché (§16).
 */
@Composable
fun PairingScreen(
    state: PairingUiState,
    actions: PairingActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = PairingTexts.TITLE, style = MaterialTheme.typography.headlineSmall)

            BoxList(state = state, actions = actions)

            AddSection(state = state, actions = actions)

            state.message?.let { message -> Text(text = message, style = MaterialTheme.typography.bodyMedium) }

            if (state.canContinue) {
                Button(
                    onClick = actions.onContinue,
                    modifier = Modifier.semantics { contentDescription = PairingTexts.CONTINUE_BUTTON_LABEL },
                ) {
                    Text(PairingTexts.CONTINUE_BUTTON_LABEL)
                }
            }
        }
    }

    NameDialogs(state = state, actions = actions)
}

/**
 * La liste annonce sa taille et chaque ligne sa position (« élément 2 sur 3 ») : Compose n'a pas
 * de rôle « élément de liste », la sémantique de collection en tient lieu pour TalkBack (§15).
 */
@Composable
private fun BoxList(
    state: PairingUiState,
    actions: PairingActions,
) {
    if (state.boxes.isEmpty()) return
    Column(
        modifier = Modifier.semantics { collectionInfo = CollectionInfo(rowCount = state.boxes.size, columnCount = 1) },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.boxes.forEachIndexed { index, row ->
            BoxRow(
                row = row,
                index = index,
                // Un dialogue ouvert attend sa réponse ; une session ne fige plus rien (étape 43).
                areActionsEnabled = !state.isDialogOpen,
                canRemove = state.canRemove,
                actions = actions,
            )
        }
    }
}

@Composable
private fun BoxRow(
    row: PairedBoxRow,
    index: Int,
    areActionsEnabled: Boolean,
    canRemove: Boolean,
    actions: PairingActions,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    collectionItemInfo =
                        CollectionItemInfo(rowIndex = index, rowSpan = 1, columnIndex = 0, columnSpan = 1)
                },
    ) {
        Text(text = row.name, style = MaterialTheme.typography.titleMedium)
        Text(text = row.prefix, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = { actions.onStartRename(row.boxId) },
                enabled = areActionsEnabled,
                colors = neutralTextButton(),
                modifier = Modifier.semantics { contentDescription = PairingTexts.renameDescription(row.name) },
            ) {
                Text(PairingTexts.RENAME_LABEL)
            }
            // Décision Q22 (étape 43) : il reste toujours au moins un boîtier, sans texte nouveau
            // sur cet écran transitoire (décision de Mehdi du 2026-10-24).
            if (canRemove) {
                TextButton(
                    onClick = { actions.onRemove(row.boxId) },
                    enabled = areActionsEnabled,
                    colors = neutralTextButton(),
                    modifier = Modifier.semantics { contentDescription = PairingTexts.removeDescription(row.name) },
                ) {
                    Text(PairingTexts.REMOVE_LABEL)
                }
            }
        }
    }
}

/**
 * Bas de liste : l'état du lecteur prime (NFC absent ou coupé), puis le plafond, puis la lecture
 * en cours, enfin le bouton d'ajout. Un seul de ces blocs à la fois, pour qu'aucun ne contredise
 * l'autre (§15, ne jamais afficher un faux état).
 */
@Composable
private fun AddSection(
    state: PairingUiState,
    actions: PairingActions,
) {
    when {
        state.nfcAvailability == NfcAvailability.ABSENT -> {
            Text(text = PairingTexts.NFC_ABSENT, style = MaterialTheme.typography.bodyLarge)
        }

        state.nfcAvailability == NfcAvailability.DISABLED -> {
            Text(text = PairingTexts.NFC_DISABLED, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(
                onClick = actions.onOpenNfcSettings,
                modifier = Modifier.semantics { contentDescription = PairingTexts.OPEN_NFC_SETTINGS_BUTTON_LABEL },
            ) {
                Text(PairingTexts.OPEN_NFC_SETTINGS_BUTTON_LABEL)
            }
        }

        state.isAtLimit -> {
            Text(text = PairingTexts.LIMIT_REACHED, style = MaterialTheme.typography.bodyLarge)
        }

        state.isScanning -> {
            Text(text = PairingTexts.INSTRUCTION, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(
                onClick = actions.onCancelScan,
                colors = neutralOutlinedButton(),
                modifier = Modifier.semantics { contentDescription = PairingTexts.CANCEL_LABEL },
            ) {
                Text(PairingTexts.CANCEL_LABEL)
            }
        }

        else -> {
            OutlinedButton(
                onClick = actions.onStartAdding,
                colors = neutralOutlinedButton(),
                modifier = Modifier.semantics { contentDescription = PairingTexts.ADD_BUTTON_LABEL },
            ) {
                Text(PairingTexts.ADD_BUTTON_LABEL)
            }
        }
    }
}

/** Les deux dialogues de nom : mêmes règles, seuls le titre et la confirmation changent. */
private enum class NameDialogKind(
    val title: String,
    val confirmLabel: String,
) {
    ADD(PairingTexts.ADD_DIALOG_TITLE, PairingTexts.CONFIRM_ADD_LABEL),
    RENAME(PairingTexts.RENAME_DIALOG_TITLE, PairingTexts.CONFIRM_RENAME_LABEL),
}

@Composable
private fun NameDialogs(
    state: PairingUiState,
    actions: PairingActions,
) {
    state.pendingName?.let { text ->
        NameDialog(NameDialogKind.ADD, text, state.canConfirmName, actions)
    }
    state.renaming?.let { renaming ->
        NameDialog(NameDialogKind.RENAME, renaming.text, state.canConfirmRename, actions)
    }
}

/**
 * Dialogue de nom, à l'ajout comme au renommage. La confirmation reste inactive tant que le dépôt
 * refuserait le nom (`PairedBoxName.sanitize`) : l'écran empêche l'appel sans redéfinir la règle.
 */
@Composable
private fun NameDialog(
    kind: NameDialogKind,
    text: String,
    canConfirm: Boolean,
    actions: PairingActions,
) {
    val onConfirm = if (kind == NameDialogKind.ADD) actions.onConfirmAdd else actions.onConfirmRename
    val onDismiss = if (kind == NameDialogKind.ADD) actions.onCancelAdd else actions.onCancelRename
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(kind.title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = actions.onNameChanged,
                label = { Text(PairingTexts.NAME_FIELD_LABEL) },
                supportingText = { Text(PairingTexts.nameCounter(text)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = canConfirm) { Text(kind.confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = neutralTextButton()) { Text(PairingTexts.CANCEL_LABEL) }
        },
    )
}

@Composable
private fun neutralTextButton(): ButtonColors =
    ButtonDefaults.textButtonColors(contentColor = LocalNiumiPalette.current.ink)

@Composable
private fun neutralOutlinedButton(): ButtonColors =
    ButtonDefaults.outlinedButtonColors(contentColor = LocalNiumiPalette.current.ink)

/**
 * Point d'entrée réel. Le Reader Mode est branché ici et non dans le ViewModel :
 * `NfcAdapter.enableReaderMode()` exige une `Activity` au premier plan (§11.1). Il démarre sur
 * `ON_RESUME` et s'arrête sur `ON_PAUSE`, comme `AlarmActivity` — la lecture ne reste jamais
 * active quand l'écran n'est pas devant l'utilisateur. La clé `isReaderModeExpected` relance
 * l'effet quand « Ajouter un boîtier » ouvre la lecture ou qu'un dialogue la ferme : l'observateur
 * réinscrit reçoit `ON_RESUME` si l'écran est déjà au premier plan.
 *
 * Ouvert aussi pendant une session, depuis l'écran de session active (étape 43, lien transitoire) :
 * les boîtiers se modifient à tout moment (SPEC_CORE_KMP §2, décisions 11 et 16).
 */
@Composable
fun PairingRoute(
    onContinue: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    val state = viewModel.state

    DisposableEffect(lifecycleOwner, activity, state.isReaderModeExpected) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshAvailability()
                    activity?.let(viewModel::startReaderMode)
                }
                if (event == Lifecycle.Event.ON_PAUSE) {
                    activity?.let(viewModel::stopReaderMode)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.let(viewModel::stopReaderMode)
        }
    }

    PairingScreen(
        state = state,
        actions =
            PairingActions(
                onOpenNfcSettings = {
                    settingsIntentFor(ReadinessAction.OpenNfcSettings, context.packageName)
                        ?.let(context::startActivity)
                },
                onContinue = onContinue,
                onStartAdding = viewModel::startAdding,
                onCancelScan = viewModel::cancelScan,
                onNameChanged = viewModel.editor::onNameChanged,
                onConfirmAdd = viewModel.editor::confirmAdd,
                onCancelAdd = viewModel.editor::cancelAdd,
                onStartRename = viewModel.editor::startRename,
                onConfirmRename = viewModel.editor::confirmRename,
                onCancelRename = viewModel.editor::cancelRename,
                onRemove = viewModel.editor::remove,
            ),
    )
}

/** `LocalContext` est un `ContextThemeWrapper` dans une `ComponentActivity` : déplier jusqu'à elle. */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Preview(showBackground = true)
@Composable
private fun PairingScreenPreview() {
    NiumiTheme(dark = false) {
        PairingScreen(
            state =
                PairingUiState(
                    boxes =
                        listOf(
                            PairedBoxRow("550e8400-e29b-41d4-a716-446655440000", "550e8400…", "Salle de bain"),
                            PairedBoxRow("7c9e6a41-0000-4000-8000-000000000000", "7c9e6a41…", "Cuisine"),
                        ),
                ),
            actions = PairingActions(onOpenNfcSettings = {}, onContinue = {}),
        )
    }
}
