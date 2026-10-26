package com.niumi.feature.session.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.niumi.designsystem.component.NfcScanSheet
import com.niumi.designsystem.component.NfcScanSheetContent
import com.niumi.designsystem.component.NiumiBottomSheet
import com.niumi.designsystem.component.NiumiPrimaryButton
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType
import com.niumi.system.nfc.ScanFailureReason
import com.niumi.system.nfc.ScanFailureTexts
import com.niumi.system.nfc.ScanSheetState
import com.niumi.system.nfc.nfcAdapterSettledChanges
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.settingsIntentFor
import kotlinx.coroutines.launch

/**
 * Feuille « Annuler le réveil ? » ouverte par le lien de l'accueil en session (étape 46,
 * `SPECIFICATION.md` §11, maquettes 25 et 26). [onCancelled] après un réveil annulé ;
 * [onCompleted] si l'alarme a sonné entre-temps et que le scan a terminé la session (écran 11) ;
 * [onDismiss] quand la feuille se ferme sans rien changer.
 *
 * Lecteur NFC câblé sur le lifecycle, comme l'écran 10 (SPEC_ANDROID §11.2) : actif seulement feuille
 * « Prêt à scanner » ouverte et au premier plan ; NFC relu à `ON_RESUME` et à chaque état stable
 * de l'adaptateur ; l'`Activity` n'est jamais retenue.
 */
@Composable
fun CancelSessionRoute(
    onDismiss: () -> Unit,
    onCancelled: () -> Unit,
    onCompleted: (String) -> Unit,
    viewModel: CancelSessionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    val state = viewModel.state

    LaunchedEffect(state.end) {
        when (val end = state.end) {
            CancelSessionEnd.Cancelled -> onCancelled()
            is CancelSessionEnd.Completed -> onCompleted(end.sessionId)
            CancelSessionEnd.Closed -> onDismiss()
            null -> Unit
        }
    }

    LaunchedEffect(lifecycleOwner, activity) {
        if (activity == null) return@LaunchedEffect
        // Bloc sur `Main.immediate` (`repeatOnLifecycle`) : le `launch` inscrit le receveur sans attendre.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // §11.2 (étape 31) : un état stable relit le NFC ; `readerModeRequested` relance la lecture.
            launch { context.nfcAdapterSettledChanges().collect { viewModel.onAdapterSettled() } }
            // Relue une fois le receveur inscrit, comme l'écran 10 (relecture de l'étape 38) : un
            // `STATE_ON` survenu entre `ON_RESUME` et l'inscription laisserait sinon « Le NFC est
            // coupé. » affiché.
            viewModel.onAdapterSettled()
            snapshotFlow { viewModel.state.readerModeRequested }.collect { requested ->
                if (requested) viewModel.startReaderMode(activity) else viewModel.stopReaderMode(activity)
            }
        }
    }

    DisposableEffect(lifecycleOwner, activity) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAvailability()
                if (event == Lifecycle.Event.ON_PAUSE) activity?.let(viewModel::stopReaderMode)
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.let(viewModel::stopReaderMode)
        }
    }

    if (state.end == null) {
        CancelSessionSheet(
            state = state,
            actions =
                CancelSessionActions(
                    onDismiss = onDismiss,
                    onScan = viewModel::openScanSheet,
                    onCancelScan = viewModel::cancelScan,
                    onRetry = viewModel::retryScan,
                    onOpenNfcSettings = {
                        settingsIntentFor(ReadinessAction.OpenNfcSettings, context.packageName)
                            ?.let(context::startActivity)
                    },
                ),
        )
    }
}

/**
 * La question (titre, texte, « Scanner le Niumi Spot ») ou, par-dessus et à sa place, la feuille de
 * scan (maquette 26), puis, après un scan accepté, son cercle de chargement. « Annuler » de la
 * feuille de scan ramène à la question. Le lien « Je n'ai pas
 * accès à mon Niumi Spot » arrive avec la sortie de secours (étape 49).
 */
@Composable
fun CancelSessionSheet(
    state: CancelSessionUiState,
    actions: CancelSessionActions,
    modifier: Modifier = Modifier,
) {
    // Pendant la libération, le cercle seul (choix de Mehdi du 2026-10-26) ; fermer la feuille ne
    // revient pas à la question : l'annulation est déjà en cours, l'accueil la montrera.
    val scanContent =
        if (state.releasing) {
            NfcScanSheetContent.Releasing(
                CancelSessionTexts.RELEASE_LOADER,
            )
        } else {
            scanSheetContent(state.sheet)
        }
    if (scanContent == null) {
        QuestionSheet(onDismiss = actions.onDismiss, onScan = actions.onScan, modifier = modifier)
    } else {
        NfcScanSheet(
            content = scanContent,
            onDismiss = if (state.releasing) actions.onDismiss else actions.onCancelScan,
            onFailureAction =
                if (state.sheet == ScanSheetState.Failed(ScanFailureReason.NFC_DISABLED)) {
                    actions.onOpenNfcSettings
                } else {
                    actions.onRetry
                },
            failurePaneTitle = ScanFailureTexts.FAILURE_PANE,
            modifier = modifier,
            phrase = CancelSessionTexts.SCAN_PHRASE,
        )
    }
}

@Composable
private fun QuestionSheet(
    onDismiss: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    // Titre en 22 et centré avec le texte, comme « Prêt à scanner » (décision de Mehdi du 2026-10-26 ;
    // la maquette 25 l'écrit en 24, aligné à gauche).
    NiumiBottomSheet(
        onDismiss = onDismiss,
        title = CancelSessionTexts.TITLE,
        modifier = modifier,
        titleCentered = true,
    ) {
        Spacer(Modifier.height(NiumiDimens.questionSheetBodyTop))
        Text(
            text = CancelSessionTexts.BODY,
            style = NiumiType.body16,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(NiumiDimens.questionSheetButtonTop))
        NiumiPrimaryButton(text = CancelSessionTexts.SCAN_BUTTON, onClick = onScan, modifier = Modifier.fillMaxWidth())
    }
}

private fun scanSheetContent(sheet: ScanSheetState): NfcScanSheetContent? =
    when (sheet) {
        ScanSheetState.Hidden -> {
            null
        }

        ScanSheetState.Ready -> {
            NfcScanSheetContent.Ready
        }

        is ScanSheetState.Failed -> {
            ScanFailureTexts.of(sheet.reason).let { NfcScanSheetContent.Failure(it.title, it.help, it.actionLabel) }
        }
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

@Preview(name = "Sombre", widthDp = 390, heightDp = 844)
@Composable
private fun CancelSessionSheetPreview() {
    NiumiTheme(dark = true) {
        CancelSessionSheet(state = CancelSessionUiState(), actions = CancelSessionActions({}, {}, {}, {}, {}))
    }
}
