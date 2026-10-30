package com.niumi.feature.session.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.niumi.designsystem.R
import com.niumi.designsystem.component.DialState
import com.niumi.designsystem.component.DialTexts
import com.niumi.designsystem.component.NiumiAlertDialog
import com.niumi.designsystem.component.NiumiAppIcon
import com.niumi.designsystem.component.NiumiCard
import com.niumi.designsystem.component.NiumiComponentTexts
import com.niumi.designsystem.component.NiumiDayChip
import com.niumi.designsystem.component.NiumiDial
import com.niumi.designsystem.component.NiumiFieldState
import com.niumi.designsystem.component.NiumiMoreAppsSlot
import com.niumi.designsystem.component.NiumiScreenScaffold
import com.niumi.designsystem.component.NiumiSectionCard
import com.niumi.designsystem.component.NiumiSegmentedTabs
import com.niumi.designsystem.component.NiumiSwitch
import com.niumi.designsystem.component.NiumiTimePickerDialog
import com.niumi.designsystem.component.NiumiToast
import com.niumi.designsystem.component.TimePickerTexts
import com.niumi.designsystem.component.appSlotCapacity
import com.niumi.designsystem.component.appSlotLayout
import com.niumi.designsystem.component.niumiTabBarClearance
import com.niumi.designsystem.component.niumiTopClearance
import com.niumi.designsystem.drawing.DialGeometry
import com.niumi.designsystem.drawing.DialHandle
import com.niumi.designsystem.format.NiumiTimeFormat
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiShapes
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType
import com.niumi.feature.session.tabs.MainTab
import com.niumi.feature.session.tabs.MainTabBar
import com.niumi.feature.session.ui.AlarmSoundSummary
import java.time.LocalTime

/**
 * Écran 6 · Accueil (`design-plan/SPECIFICATION.md` §9, maquettes 15 à 20 ; étape 45) : cadran,
 * carte d'état, onglets « Ponctuel / Quotidien », pastilles des jours, lignes « Applications
 * bloquées » et « Sonnerie », puis la barre d'onglets du bas, « Réveil » choisi (décision de Mehdi
 * du 2026-10-25, qui retire l'icône des réglages du haut). Depuis les maquettes du 2026-10-30, le
 * bloc des jours et les deux lignes sont chacun dans une carte de section ; le cadran, d'abord dans
 * une carte, est posé sur le fond depuis l'essai de Mehdi du même jour. Tout défile sur un fond fixe,
 * sous la bande du haut et sous la barre, qui laissent deviner le contenu par leur bord translucide.
 * La bulle du bas (M10) se pose au-dessus de la barre et disparaît au toucher suivant, où qu'il soit.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    // Rien n'est affirmé avant la première lecture du planning : pas d'accueil par défaut qui
    // basculerait aussitôt (faux état, SPEC_ANDROID §15).
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize().background(LocalNiumiPalette.current.background))
        return
    }
    Box(
        modifier =
            modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    actions.onAnyTouch()
                }
            },
    ) {
        val backdrop = remember { HomeBackdrop() }
        NiumiScreenScaffold(
            backgroundBrush = homeBackdropBrush(backdrop),
            contentUnderTop = true,
            topBandColor = LocalNiumiPalette.current.surface,
            navigationBar = {
                MainTabBar(selected = MainTab.WAKE, onWake = {}, onSettings = actions.onOpenSettings)
            },
        ) {
            HomeContent(state, actions, backdrop)
        }
        state.message?.let { message ->
            NiumiToast(
                text = message,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        // Au-dessus de la barre d'onglets, à l'écart d'une barre système : les 34 de la
                        // maquette 27 sont la marge du bas de l'iPhone, que la barre occupe désormais.
                        .padding(bottom = NiumiDimens.tabBarHeight + NiumiDimens.systemBarGap)
                        .padding(horizontal = NiumiDimens.toastHorizontalMargin),
            )
        }
        state.picker?.let { handle -> HomeTimePicker(state, handle, actions) }
        state.immediateBlockingBedtimeMinutes?.let { bedtime -> ImmediateBlockingDialog(bedtime, actions) }
    }
}

/**
 * Avant un changement qui lancerait le blocage tout de suite (`SPECIFICATION.md` §2 et §9, décision
 * de Mehdi du 2026-10-25) : fenêtre centrée comme le sélecteur d'heure. « Annuler », le voile et le
 * retour ne changent rien.
 */
@Composable
private fun ImmediateBlockingDialog(
    bedtimeMinutes: Int,
    actions: HomeActions,
) {
    NiumiAlertDialog(
        onDismiss = actions.onImmediateBlockingDismissed,
        title = HomeTexts.IMMEDIATE_BLOCKING_TITLE,
        icon = R.drawable.ic_lock,
        primary = HomeTexts.BLOCK_NOW,
        onPrimary = actions.onImmediateBlockingConfirmed,
        secondaryLink = HomeTexts.CANCEL,
        onSecondary = actions.onImmediateBlockingDismissed,
    ) {
        Text(
            text = HomeTexts.immediateBlockingBody(clockOf(bedtimeMinutes)),
            style = NiumiType.body16,
            color = LocalNiumiPalette.current.textSecondary,
        )
    }
}

/**
 * Tout le contenu défile, cadran compris ; seul le fond reste en place (demande de Mehdi du
 * 2026-10-30). Le cadran est posé sur le fond, sans carte, et son anneau porte l'ombre des cartes
 * (essai du même jour ; dans une carte dans les maquettes 15 à 27) ; sa zone déborde de 12 sur les
 * marges. Le contenu qui remonte passe sous la bande du haut, translucide en bas comme le haut de la
 * barre d'onglets.
 */
@Composable
private fun HomeContent(
    state: HomeUiState,
    actions: HomeActions,
    backdrop: HomeBackdrop,
) {
    val fieldState =
        when (state.mode) {
            HomeMode.InSession -> NiumiFieldState.Locked
            HomeMode.Disabled -> NiumiFieldState.Greyed
            HomeMode.NothingArmed, HomeMode.Scheduled -> NiumiFieldState.Normal
        }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .onGloballyPositioned { backdrop.scrollTop = it.positionInRoot().y },
    ) {
        // Le contenu monte sous la bande du haut ; il commence sous elle.
        Spacer(modifier = Modifier.height(niumiTopClearance() + NiumiDimens.homeDialTop))
        NiumiDial(
            state = dialStateOf(state),
            texts = DialTexts(bedtime = HomeTexts.BEDTIME, wake = HomeTexts.WAKE, chooseTime = HomeTexts.CHOOSE_TIME),
            onTimeChanged = actions.onTimeChanged,
            onTap = actions.onDialTapped,
            modifier = Modifier.overflowSideMargins(NiumiDimens.dialOverflow),
        )
        Spacer(modifier = Modifier.height(NiumiDimens.homeCardTop))
        StatusCard(
            state,
            actions,
            modifier = Modifier.onGloballyPositioned { backdrop.statusTop = it.positionInParent().y },
        )
        Spacer(modifier = Modifier.height(NiumiDimens.homeSectionGap))
        HomeDaysCard(state, fieldState, actions)
        Spacer(modifier = Modifier.height(NiumiDimens.homeSectionGap))
        NiumiSectionCard(
            contentPadding = NiumiDimens.sectionCardRowsPadding,
            modifier = Modifier.onGloballyPositioned { backdrop.rowsTop = it.positionInParent().y },
        ) {
            AppsRow(state, fieldState, onOpen = if (state.isLocked) actions.onLockedTouch else actions.onOpenApps)
            RingtoneRow(state.ringtone, greyed = state.mode == HomeMode.Disabled, onOpen = actions.onOpenRingtone)
        }
        // Le contenu passe sous la barre ; sa fin doit pouvoir remonter au-dessus d'elle, avec un
        // espace pour que la dernière carte ne la touche pas.
        Spacer(modifier = Modifier.height(niumiTabBarClearance() + NiumiDimens.homeScrollEndGap))
    }
}

private fun dialStateOf(state: HomeUiState): DialState =
    when (state.mode) {
        HomeMode.InSession -> {
            DialState.InSession(
                state.bedtimeMinutes,
                state.wakeMinutes,
                state.nowMinutes,
                state.centerLabel,
                state.centerValue,
            )
        }

        HomeMode.Disabled -> {
            DialState.Disabled(
                state.bedtimeMinutes,
                state.wakeMinutes,
                state.nowMinutes,
                state.centerLabel,
                state.centerValue,
            )
        }

        HomeMode.NothingArmed, HomeMode.Scheduled -> {
            DialState.Scheduled(
                state.bedtimeMinutes,
                state.wakeMinutes,
                state.nowMinutes,
                state.centerLabel,
                state.centerValue,
            )
        }
    }

/**
 * Carte d'état : « Réveil activé » noire, « Réveil désactivé » au trait, « Blocage en cours » avec le
 * cadenas ambre et le lien « Annuler le réveil », sans interrupteur.
 */
@Composable
private fun StatusCard(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNiumiPalette.current
    val filled = state.mode == HomeMode.Scheduled || state.mode == HomeMode.NothingArmed
    val inSession = state.mode == HomeMode.InSession
    NiumiCard(filled = filled, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NiumiDimens.permissionTileTextGap),
            ) {
                Text(
                    text = state.cardTitle,
                    style = if (inSession) NiumiType.labelStrong16 else NiumiType.label16,
                    color = if (filled) palette.onInk else palette.ink,
                )
                state.cardLines.forEach { line ->
                    Text(
                        text = line,
                        style = NiumiType.secondary14,
                        color = if (filled) palette.onInkSecondary else palette.textSecondary,
                    )
                }
            }
            if (inSession) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock_bold),
                    contentDescription = NiumiComponentTexts.LOCKED,
                    tint = palette.amber,
                    modifier = Modifier.align(Alignment.Top).size(NiumiDimens.homeLockIcon),
                )
            } else {
                Spacer(modifier = Modifier.size(NiumiDimens.rowGap))
                NiumiSwitch(
                    checked = state.switchChecked,
                    onCheckedChange = actions.onSwitchChanged,
                    contentDescription = state.cardTitle,
                )
            }
        }
        if (inSession) {
            Text(
                text = HomeTexts.CANCEL_WAKE,
                style = NiumiType.chip15.copy(textDecoration = TextDecoration.Underline),
                color = palette.ink,
                modifier =
                    Modifier
                        .heightIn(min = NiumiDimens.minTouch)
                        .clickable(role = Role.Button, onClick = actions.onCancelWake)
                        .padding(vertical = NiumiDimens.homeLinkPadding),
            )
        }
    }
}

/**
 * Ligne « Applications bloquées » : titre et icônes à gauche, nombre et chevron ou cadenas. Première
 * ligne de sa carte : pas de trait au-dessus (maquettes du 2026-10-30).
 */
@Composable
private fun AppsRow(
    state: HomeUiState,
    fieldState: NiumiFieldState,
    onOpen: () -> Unit,
) {
    val palette = LocalNiumiPalette.current
    val greyed = fieldState != NiumiFieldState.Normal
    val count = state.apps.size
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(NiumiDimens.homeAppsRowHeight)
                .clickable(role = Role.Button, onClick = onOpen)
                .clearAndSetSemantics {
                    contentDescription = "${HomeTexts.BLOCKED_APPS}, ${HomeTexts.iconsLabel(count)}"
                    role = Role.Button
                    if (fieldState == NiumiFieldState.Locked) stateDescription = NiumiComponentTexts.LOCKED
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NiumiDimens.homeAppsIconsGap),
        ) {
            Text(
                text = HomeTexts.BLOCKED_APPS,
                style = NiumiType.body16,
                color = if (greyed) palette.border else palette.ink,
            )
            AppIcons(state, greyed)
        }
        Spacer(modifier = Modifier.size(NiumiDimens.rowGap))
        Text(text = count.toString(), style = NiumiType.body16, color = palette.textSecondary)
        Spacer(modifier = Modifier.size(NiumiDimens.homeRowTrailingGap))
        Icon(
            painter =
                painterResource(
                    if (fieldState ==
                        NiumiFieldState.Locked
                    ) {
                        R.drawable.ic_lock
                    } else {
                        R.drawable.ic_chevron_right
                    },
                ),
            contentDescription = null,
            tint = if (fieldState == NiumiFieldState.Locked) palette.border else palette.textSecondary,
            modifier = Modifier.size(NiumiDimens.smallIcon),
        )
    }
}

@Composable
private fun AppIcons(
    state: HomeUiState,
    greyed: Boolean,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val capacity = appSlotCapacity(maxWidth.value, NiumiDimens.appIcon.value, NiumiDimens.homeAppIconGap.value)
        val layout = appSlotLayout(state.apps.size, capacity)
        Row(horizontalArrangement = Arrangement.spacedBy(NiumiDimens.homeAppIconGap)) {
            state.apps.take(layout.shown).forEach { app ->
                NiumiAppIcon(
                    key = app.packageName,
                    icon = app.icon,
                    size = NiumiDimens.appIcon,
                    shape = NiumiShapes.appIcon,
                    greyed = greyed,
                )
            }
            if (layout.hidden > 0) {
                NiumiMoreAppsSlot(
                    text = HomeTexts.moreApps(layout.hidden),
                    size = NiumiDimens.appIcon,
                    shape = NiumiShapes.appIcon,
                    textStyle = NiumiType.caption13,
                )
            }
        }
    }
}

/** Ligne « Sonnerie » : le son et, dessous, « Progressive · 10 min » (D5 : rien si éteinte). */
@Composable
private fun RingtoneRow(
    summary: AlarmSoundSummary?,
    greyed: Boolean,
    onOpen: () -> Unit,
) {
    val palette = LocalNiumiPalette.current
    val ink = if (greyed) palette.border else palette.ink
    HorizontalDivider(thickness = NiumiDimens.hairline, color = palette.separator)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(NiumiDimens.homeRingtoneRowHeight)
                .clickable(role = Role.Button, onClick = onOpen)
                .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = HomeTexts.RINGTONE, style = NiumiType.body16, color = ink, modifier = Modifier.weight(1f))
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(NiumiDimens.permissionTileTextGap),
        ) {
            summary?.let { Text(text = it.name, style = NiumiType.body16, color = ink) }
            summary?.detail?.let { Text(text = it, style = NiumiType.caption13, color = palette.textSecondary) }
        }
        Spacer(modifier = Modifier.size(NiumiDimens.trailingGap))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size(NiumiDimens.smallIcon),
        )
    }
    // Pas de trait sous la dernière ligne (décision de Mehdi du 2026-10-25, la maquette en dessine un).
}

private val previewActions = HomeActions({}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, { _, _ -> }, {}, {}, {}, {}, {})

private val previewScheduled =
    HomeUiState(
        isLoading = false,
        mode = HomeMode.Scheduled,
        bedtimeMinutes = 22 * 60 + 30,
        wakeMinutes = 7 * 60,
        nowMinutes = 21 * 60 + 34,
        enabledIsoDays = setOf(1, 2, 3, 4, 5),
        dayMode = DayMode.CUSTOM,
        centerLabel = HomeTexts.WAKE_IN,
        centerValue = "9 h 26",
        cardTitle = HomeTexts.WAKE_ENABLED,
        cardLines = listOf(HomeTexts.blockingIn(56), HomeTexts.sleep(510)),
        switchChecked = true,
        ringtone = AlarmSoundSummary("Piano", "Progressive · 10 min"),
    )

@Preview(name = "Réveil prévu", widthDp = 390, heightDp = 844)
@Composable
private fun HomeScheduledPreview() {
    NiumiTheme(dark = false) { HomeScreen(previewScheduled, previewActions) }
}

@Preview(name = "Session en cours", widthDp = 390, heightDp = 844)
@Composable
private fun HomeInSessionPreview() {
    NiumiTheme(dark = true) {
        HomeScreen(
            previewScheduled.copy(
                mode = HomeMode.InSession,
                nowMinutes = 23 * 60 + 10,
                centerValue = "7 h 50",
                cardTitle = HomeTexts.BLOCKING_IN_PROGRESS,
                cardLines = listOf(HomeTexts.BLOCKED_UNTIL_SCAN),
            ),
            previewActions,
        )
    }
}
