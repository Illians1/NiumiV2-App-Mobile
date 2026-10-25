package com.niumi.feature.session.home

import androidx.compose.runtime.Composable
import com.niumi.designsystem.component.NiumiTimePickerDialog
import com.niumi.designsystem.component.TimePickerTexts
import com.niumi.designsystem.drawing.DialGeometry
import com.niumi.designsystem.drawing.DialHandle
import com.niumi.designsystem.format.NiumiTimeFormat
import java.time.LocalTime

/** Sélecteur d'heure de l'accueil (maquettes 19 et 20), ouvert sur l'onglet de [handle]. */
@Composable
internal fun HomeTimePicker(
    state: HomeUiState,
    handle: DialHandle,
    actions: HomeActions,
) {
    NiumiTimePickerDialog(
        initialHandle = handle,
        bedtimeMinutes = state.bedtimeMinutes,
        wakeMinutes = state.wakeMinutes,
        texts =
            TimePickerTexts(
                bedtimeTab = HomeTexts.BEDTIME,
                wakeTab = HomeTexts.WAKE,
                bedtimeTitle = HomeTexts.BEDTIME_TITLE,
                wakeTitle = HomeTexts.WAKE_TITLE,
                hours = HomeTexts.HOURS,
                minutes = HomeTexts.MINUTES,
                confirm = HomeTexts.CONFIRM,
                close = HomeTexts.CLOSE,
            ),
        sentence = { tab, bedtime, wake ->
            val period = DialGeometry.periodMinutes(bedtime, wake)
            if (tab == DialHandle.BEDTIME) {
                HomeTexts.sleepBeforeWake(period, clockOf(wake))
            } else {
                HomeTexts.sleepAfterBedtime(period, clockOf(bedtime))
            }
        },
        onConfirm = actions.onPickerConfirmed,
        onDismiss = actions.onPickerDismissed,
    )
}

/** « 22:30 », « 7:00 » : l'heure du cadran, partagée par le sélecteur et la fenêtre de confirmation. */
internal fun clockOf(minutes: Int): String =
    NiumiTimeFormat.clock(
        LocalTime.of(
            minutes / MINUTES_PER_HOUR,
            minutes % MINUTES_PER_HOUR,
        ),
    )

private const val MINUTES_PER_HOUR = 60
