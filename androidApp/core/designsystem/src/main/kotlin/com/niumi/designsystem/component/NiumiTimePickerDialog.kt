package com.niumi.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.drawing.DialHandle
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiType

/**
 * Sélecteur d'heure de l'accueil (maquettes 19 et 20 ; décisions de Mehdi du 2026-10-25) : fenêtre
 * centrée, de la largeur de la fenêtre d'alerte (28 des bords, demande de Mehdi du 2026-10-30 ; 20
 * avant), onglets « Coucher » et « Réveil », une roue des heures et une des minutes à la minute
 * près, une phrase qui suit le choix ([sentence], « 8 h 30 de déconnexion avant le réveil de 7:00 »,
 * sur deux lignes équilibrées) et « Valider ». Ouvert sur l'onglet [initialHandle].
 *
 * Toutes les heures sont acceptées : une minute d'écart au moins, deux heures identiques font 24 h.
 * Passer d'un onglet à l'autre garde le choix en cours ; [onConfirm] reçoit les deux heures, en
 * minutes depuis minuit. La croix, le voile et le retour ferment sans rien changer.
 */
@Suppress("LongParameterList") // Convention Compose : les paramètres d'un composant sont son API.
@Composable
fun NiumiTimePickerDialog(
    initialHandle: DialHandle,
    bedtimeMinutes: Int,
    wakeMinutes: Int,
    texts: TimePickerTexts,
    sentence: (handle: DialHandle, bedtime: Int, wake: Int) -> String,
    onConfirm: (bedtime: Int, wake: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var handle by rememberSaveable { mutableStateOf(initialHandle) }
    var bedtime by rememberSaveable { mutableIntStateOf(bedtimeMinutes) }
    var wake by rememberSaveable { mutableIntStateOf(wakeMinutes) }
    val palette = LocalNiumiPalette.current
    val edited = if (handle == DialHandle.BEDTIME) bedtime else wake
    val update: (Int) -> Unit = { minutes -> if (handle == DialHandle.BEDTIME) bedtime = minutes else wake = minutes }

    NiumiDialogHost(onDismiss = onDismiss) {
        Box(
            modifier =
                Modifier
                    .padding(horizontal = NiumiDimens.alertHorizontalMargin)
                    .fillMaxWidth()
                    .shadow(NiumiDimens.alertElevation, NiumiShapes.alert)
                    .clip(NiumiShapes.alert)
                    .background(palette.surface)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .semantics {
                        paneTitle =
                            if (handle ==
                                DialHandle.BEDTIME
                            ) {
                                texts.bedtimeTitle
                            } else {
                                texts.wakeTitle
                            }
                    },
        ) {
            PickerBody(
                handle = handle,
                edited = edited,
                texts = texts,
                sentence = sentence(handle, bedtime, wake),
                onTab = { handle = it },
                onEdit = update,
                onConfirm = { onConfirm(bedtime, wake) },
            )
            NiumiRoundCloseButton(
                label = texts.close,
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(NiumiDimens.alertCloseInset),
            )
        }
    }
}

/** Onglets, roues, phrase et « Valider ». */
@Suppress("LongParameterList")
@Composable
private fun PickerBody(
    handle: DialHandle,
    edited: Int,
    texts: TimePickerTexts,
    sentence: String,
    onTab: (DialHandle) -> Unit,
    onEdit: (Int) -> Unit,
    onConfirm: () -> Unit,
) {
    val palette = LocalNiumiPalette.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(NiumiDimens.timePickerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NiumiTextTabs(
            labels = listOf(texts.bedtimeTab, texts.wakeTab),
            selectedIndex = if (handle == DialHandle.BEDTIME) 0 else 1,
            onSelect = { index -> onTab(if (index == 0) DialHandle.BEDTIME else DialHandle.WAKE) },
            size = NiumiTextTabsSize.Picker,
            modifier = Modifier.padding(horizontal = NiumiDimens.touchTarget),
        )
        Spacer(modifier = Modifier.height(NiumiDimens.timePickerWheelsTop))
        key(handle) { TimeWheels(edited, texts, onEdit) }
        Spacer(modifier = Modifier.height(NiumiDimens.timePickerSentenceTop))
        // Deux lignes (décision de Mehdi du 2026-10-30) : la phrase porte son retour à la ligne entre
        // la durée et le reste ; `minLines` fixe la hauteur si une phrase n'en a pas, et la coupure
        // équilibrée ne joue que si une police agrandie fait déborder une ligne.
        Text(
            text = sentence,
            style = NiumiType.secondary14.copy(lineBreak = BALANCED_PARAGRAPH),
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            minLines = 2,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(modifier = Modifier.height(NiumiDimens.timePickerButtonTop))
        NiumiPrimaryButton(
            text = texts.confirm,
            onClick = onConfirm,
            size = NiumiButtonSize.Sheet,
        )
    }
}

/**
 * Les deux roues séparées par « : », entre les deux traits de la valeur choisie. Les traits ne
 * couvrent que les roues, de la première colonne à la seconde (demande de Mehdi du 2026-10-30 ; toute
 * la largeur de la fenêtre avant).
 */
@Composable
private fun TimeWheels(
    minutes: Int,
    texts: TimePickerTexts,
    onChange: (Int) -> Unit,
) {
    val palette = LocalNiumiPalette.current
    Box(
        modifier =
            Modifier.drawBehind {
                val line = NiumiDimens.wheelLine.toPx()
                val half = NiumiDimens.wheelSelectedHeight.toPx() / 2
                val middle = size.height / 2
                listOf(middle - half, middle + half).forEach { y ->
                    drawLine(palette.ink, Offset(0f, y), Offset(size.width, y), strokeWidth = line)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NiumiDimens.wheelColumnGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NiumiWheelPicker(
                values = HOURS,
                selectedIndex = minutes / MINUTES_PER_HOUR,
                onSelectedIndexChange = { hour -> onChange(hour * MINUTES_PER_HOUR + minutes % MINUTES_PER_HOUR) },
                contentDescription = texts.hours,
            )
            Text(
                text = ":",
                style = NiumiType.wheelSelected34,
                color = palette.ink,
                modifier = Modifier.offset(y = NiumiDimens.wheelColonLift).clearAndSetSemantics {},
            )
            NiumiWheelPicker(
                values = MINUTES,
                selectedIndex = minutes % MINUTES_PER_HOUR,
                onSelectedIndexChange = { minute -> onChange(minutes / MINUTES_PER_HOUR * MINUTES_PER_HOUR + minute) },
                contentDescription = texts.minutes,
            )
        }
    }
}

private const val MINUTES_PER_HOUR = 60

/** Coupure en lignes de longueur proche, pour la phrase du sélecteur. */
private val BALANCED_PARAGRAPH = LineBreak.Paragraph.copy(strategy = LineBreak.Strategy.Balanced)
private const val HOURS_PER_DAY = 24

/** Heures sans zéro devant (« 7 »), minutes sur deux chiffres (« 00 »), comme les maquettes 19 et 20. */
private val HOURS = (0 until HOURS_PER_DAY).map { it.toString() }
private val MINUTES = (0 until MINUTES_PER_HOUR).map { it.toString().padStart(2, '0') }

@Preview(name = "Coucher", widthDp = 390, heightDp = 844)
@Composable
private fun NiumiTimePickerDialogPreview() {
    NiumiPreviewFrame(dark = false) {
        NiumiTimePickerDialog(
            initialHandle = DialHandle.BEDTIME,
            bedtimeMinutes = 22 * 60 + 30,
            wakeMinutes = 7 * 60,
            texts =
                TimePickerTexts(
                    "Coucher",
                    "Réveil",
                    "Heure de coucher",
                    "Heure de réveil",
                    "Heures",
                    "Minutes",
                    "Valider",
                    "Fermer",
                ),
            sentence = { _, _, _ -> "8 h 30 de déconnexion\navant le réveil de 7:00" },
            onConfirm = { _, _ -> },
            onDismiss = {},
        )
    }
}
