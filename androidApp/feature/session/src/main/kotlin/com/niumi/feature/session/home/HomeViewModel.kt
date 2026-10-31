package com.niumi.feature.session.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.isBlockingPending
import com.niumi.designsystem.drawing.DialGeometry
import com.niumi.designsystem.drawing.DialHandle
import com.niumi.designsystem.format.NiumiTimeFormat
import com.niumi.feature.session.ui.AlarmSoundSummary
import com.niumi.feature.session.ui.AlarmSoundTexts
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.apps.InstalledApp
import com.niumi.system.apps.InstalledAppsSource
import com.niumi.system.audio.AlarmSoundPreferences
import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.common.Clock
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.plan.PlanArmingState
import com.niumi.system.plan.PlanArmingStatus
import com.niumi.system.plan.PlanChangeReason
import com.niumi.system.plan.WakePlan
import com.niumi.system.plan.WakePlanScheduler
import com.niumi.system.plan.WakePlanStore
import com.niumi.system.plan.nextOccurrence
import com.niumi.system.readiness.ForegroundReadinessTrigger
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionPersistenceGateway
import com.niumi.system.session.SessionSnapshotPublisher
import com.niumi.system.session.isBlockingInForce
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.ceil

/** Le planning et son application (étape 44). */
class HomePlanSources
    @Inject
    constructor(
        val store: WakePlanStore,
        val scheduler: WakePlanScheduler,
        val armingStatus: PlanArmingStatus,
        val facade: NiumiCoreFacade,
        val readiness: ForegroundReadinessTrigger,
    )

/** Ce que montrent les lignes « Applications bloquées » et « Sonnerie ». */
class HomeRowSources
    @Inject
    constructor(
        val appSelectionStore: AppSelectionStore,
        val installedApps: InstalledAppsSource,
        val alarmSoundPreferences: AlarmSoundPreferences,
        val gateway: SessionPersistenceGateway,
    )

/** L'heure, le fuseau et le battement de la minute. */
class HomeTime
    @Inject
    constructor(
        val clock: Clock,
        val timeZoneProvider: TimeZoneProvider,
        val ticker: MinuteTicker,
    )

/** Dépôts et horloges de l'accueil, regroupés pour rester sous `LongParameterList` de detekt. */
class HomeSources
    @Inject
    constructor(
        val plan: HomePlanSources,
        val rows: HomeRowSources,
        val snapshotPublisher: SessionSnapshotPublisher,
        val time: HomeTime,
    )

/**
 * Accueil (`design-plan/SPECIFICATION.md` §9, étape 45). Le planning est la source du cadran, de la
 * carte, des onglets et des pastilles ; la session, celle du verrouillage et du thème (décision 4 du
 * Lot 13). L'écran n'écrit jamais un état de session : chaque modification passe par
 * [WakePlanScheduler.apply], qui désarme et réarme sous le verrou du coordinateur.
 *
 * Le planning demandé est montré tout de suite, le temps que le scheduler l'applique ; il disparaît
 * s'il est refusé (session commencée entre-temps). Un changement qui lancerait le blocage tout de
 * suite (coucher déjà atteint) attend d'abord la confirmation de l'utilisateur ; d'ici là, l'écran
 * garde le planning lu (décision de Mehdi du 2026-10-25, qui remplace « sans confirmation », §2).
 * Le cadran, les comptes à rebours et le point de l'heure suivent la minute ([MinuteTicker], Q13).
 */
@Suppress("TooManyFunctions") // Une fonction par geste de l'écran.
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val sources: HomeSources,
    ) : ViewModel() {
        var state by mutableStateOf(HomeUiState())
            private set

        private var plan: WakePlan? = null
        private var requested: WakePlan? = null
        private var snapshot: SessionSnapshotDto? = null
        private var armingFailed = false
        private var apps: List<InstalledApp> = emptyList()
        private var ringtone: AlarmSoundSummary? = null
        private var message: String? = null
        private var picker: DialHandle? = null
        private var pendingChange: Pair<WakePlan, PlanChangeReason>? = null
        private var messageJob: Job? = null
        private var refreshJob: Job? = null
        private val applying = Mutex()

        init {
            viewModelScope.launch {
                combine(
                    sources.plan.store.plan,
                    sources.snapshotPublisher.snapshot,
                    sources.plan.armingStatus.state,
                    sources.time.ticker.ticks(),
                ) { plan, snapshot, arming, _ -> Triple(plan, snapshot, arming) }
                    .collectLatest { (plan, snapshot, arming) ->
                        this@HomeViewModel.plan = plan
                        this@HomeViewModel.snapshot = snapshot
                        armingFailed = arming is PlanArmingState.Failed
                        recompute()
                    }
            }
            refresh()
        }

        /**
         * Rejoué à chaque `ON_RESUME`, au retour de l'écran 4 ou de la feuille « Sonnerie », et à la fin
         * de chaque enregistrement de cette feuille. La relecture précédente est annulée : lancée plus
         * tôt, elle peut avoir lu une valeur périmée et finir après la nouvelle (constat du 2026-10-26).
         */
        fun refresh() {
            refreshJob?.cancel()
            refreshJob =
                viewModelScope.launch {
                    apps = loadApps()
                    ringtone = loadRingtone()
                    recompute()
                }
        }

        fun onSwitchChanged(enabled: Boolean) = change(PlanChangeReason.SWITCH) { it.copy(enabled = enabled) }

        /** Décocher le dernier jour rend le réveil ponctuel (décision du 2026-10-25). */
        fun onDayToggled(isoDay: Int) =
            change(PlanChangeReason.DAYS) {
                val days = it.enabledIsoDays
                it.copy(enabledIsoDays = if (isoDay in days) days - isoDay else days + isoDay)
            }

        fun onDayModeSelected(mode: DayMode) =
            change(PlanChangeReason.DAYS) {
                when (mode) {
                    DayMode.ONE_OFF -> it.copy(enabledIsoDays = emptySet())
                    DayMode.DAILY -> it.copy(enabledIsoDays = ALL_DAYS)
                    DayMode.CUSTOM -> it
                }
            }

        /** Poignée relâchée : une seule application du planning, jamais pendant le glisser. */
        fun onTimeChanged(
            handle: DialHandle,
            minutes: Int,
        ) = when (handle) {
            DialHandle.BEDTIME -> change(PlanChangeReason.BEDTIME) { it.copy(bedtimeIso = minutes.toIso()) }
            DialHandle.WAKE -> change(PlanChangeReason.WAKE_TIME) { it.copy(wakeTimeIso = minutes.toIso()) }
        }

        fun onDialTapped(handle: DialHandle) {
            if (state.isLocked) return onLockedTouch()
            picker = handle
            recompute()
        }

        /** « Valider » du sélecteur : les deux heures en une seule application. */
        fun onPickerConfirmed(
            bedtime: Int,
            wake: Int,
        ) {
            picker = null
            recompute()
            val current = displayedPlan() ?: return
            val reason =
                if (wake.toIso() !=
                    current.wakeTimeIso
                ) {
                    PlanChangeReason.WAKE_TIME
                } else {
                    PlanChangeReason.BEDTIME
                }
            change(reason) { it.copy(bedtimeIso = bedtime.toIso(), wakeTimeIso = wake.toIso()) }
        }

        fun onPickerDismissed() {
            picker = null
            recompute()
        }

        /** « Bloquer maintenant » : le changement mis en attente est appliqué, une seule fois. */
        fun onImmediateBlockingConfirmed() {
            val (target, reason) = pendingChange ?: return
            pendingChange = null
            apply(target, reason)
        }

        /** « Annuler », le voile ou le retour : rien n'est appliqué, l'écran garde le planning lu. */
        fun onImmediateBlockingDismissed() {
            if (pendingChange == null) return
            pendingChange = null
            recompute()
        }

        /** M10 : la bulle du bas, quatre secondes ou jusqu'au toucher suivant. */
        fun onLockedTouch() = showMessage(HomeTexts.LOCKED_UNTIL_SCAN)

        /** Après l'annulation par scan (§11, étape 46) : même bulle, même durée que M10. */
        fun showCancelledMessage() = showMessage(HomeTexts.WAKE_CANCELLED)

        private fun showMessage(text: String) {
            message = text
            recompute()
            messageJob?.cancel()
            messageJob =
                viewModelScope.launch {
                    delay(MESSAGE_MILLIS)
                    dismissMessage()
                }
        }

        fun dismissMessage() {
            if (message == null) return
            messageJob?.cancel()
            message = null
            recompute()
        }

        private fun change(
            reason: PlanChangeReason,
            edit: (WakePlan) -> WakePlan,
        ) {
            if (state.isLocked) return onLockedTouch()
            val current = displayedPlan()
            val target = current?.let(edit)?.takeIf { it != current } ?: return
            if (blocksImmediately(target)) {
                pendingChange = target to reason
                recompute()
            } else {
                apply(target, reason)
            }
        }

        /** Interrupteur allumé et coucher atteint : armer ce planning bloquerait tout de suite (§8.4, règle 6). */
        private fun blocksImmediately(target: WakePlan): Boolean =
            target.enabled &&
                sources.plan.facade
                    .nextOccurrence(
                        target,
                        sources.time.timeZoneProvider.currentZoneId(),
                        sources.time.clock.nowEpochMillis(),
                    ).occurrence
                    ?.blockingStartsImmediately == true

        private fun apply(
            target: WakePlan,
            reason: PlanChangeReason,
        ) {
            requested = target
            recompute()
            viewModelScope.launch {
                applying.withLock {
                    try {
                        sources.plan.scheduler.apply(target, reason)
                        // Surveillance §13.1 tout de suite : un réveil armé sans le service
                        // d'accessibilité doit être signalé sans attendre le prochain retour au
                        // premier plan (essai du 2026-10-25).
                        sources.plan.readiness.evaluateAsync()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (
                        @Suppress("TooGenericExceptionCaught") _: Exception,
                    ) {
                        // Appareil verrouillé, planning invalide ou stockage en erreur : rien n'est
                        // écrit, l'écran revient au planning lu ; le scheduler journalise ses propres
                        // échecs d'armement (Q14).
                    }
                }
                if (requested == target) requested = null
                recompute()
            }
        }

        private fun displayedPlan(): WakePlan? = requested ?: plan

        private suspend fun loadApps(): List<InstalledApp> {
            val chosen = sources.rows.appSelectionStore.selection()
            val installed =
                sources.rows.installedApps
                    .launchableApps()
                    .associateBy { it.packageName }
            return chosen.map {
                installed[it.packageName]
                    ?: InstalledApp(it.packageName, it.displayNameSnapshot, icon = null)
            }
        }

        /** Même source que la feuille « Sonnerie » : la session armée (ce qui sonnera), sinon la préférence. */
        private suspend fun loadRingtone(): AlarmSoundSummary {
            val armed =
                (sources.rows.gateway.load() as? LoadResult.Present)
                    ?.takeIf { it.snapshot.state == SessionStateDto.ARMED }
                    ?.let { AlarmSoundSettings(it.extras.ringtoneKey, it.extras.volumeRampSeconds) }
            return AlarmSoundTexts.summary(armed ?: sources.rows.alarmSoundPreferences.read())
        }

        private fun recompute() {
            val plan = displayedPlan() ?: return
            val now = sources.time.clock.nowEpochMillis()
            val zone = sources.time.timeZoneProvider.currentZoneId()
            val snapshot = snapshot
            val bedtime = plan.bedtimeIso.toMinutes()
            val wake = plan.wakeTimeIso.toMinutes()
            val pending = snapshot?.takeIf { it.state == SessionStateDto.ARMED && it.isBlockingPending }
            val mode =
                when {
                    snapshot.isBlockingInForce() -> HomeMode.InSession
                    !plan.enabled -> HomeMode.Disabled
                    armingFailed && pending == null -> HomeMode.NothingArmed
                    else -> HomeMode.Scheduled
                }
            val base =
                HomeUiState(
                    isLoading = false,
                    mode = mode,
                    bedtimeMinutes = bedtime,
                    wakeMinutes = wake,
                    nowMinutes = minutesOfDay(now, zone),
                    enabledIsoDays = plan.enabledIsoDays,
                    dayMode = dayModeOf(plan.enabledIsoDays),
                    switchChecked = plan.enabled,
                    apps = apps,
                    ringtone = ringtone,
                    message = message,
                    picker = picker,
                    immediateBlockingBedtimeMinutes = pendingChange?.first?.bedtimeIso?.toMinutes(),
                )
            state = textsFor(base, snapshot, pending, plan, zone, now)
        }

        /** Centre du cadran et carte selon l'état (maquettes 15 à 18 ; Q14). */
        @Suppress("LongParameterList")
        private fun textsFor(
            base: HomeUiState,
            snapshot: SessionSnapshotDto?,
            pending: SessionSnapshotDto?,
            plan: WakePlan,
            zone: String,
            now: Long,
        ): HomeUiState {
            val sleep = HomeTexts.sleep(DialGeometry.periodMinutes(base.bedtimeMinutes, base.wakeMinutes))
            return when (base.mode) {
                HomeMode.InSession -> {
                    base.copy(
                        centerLabel = HomeTexts.WAKE_IN,
                        centerValue = countdown(now, snapshot?.wakeSchedule?.triggerAtEpochMillis),
                        cardTitle = HomeTexts.BLOCKING_IN_PROGRESS,
                        cardLines = listOf(HomeTexts.BLOCKED_UNTIL_SCAN),
                    )
                }

                HomeMode.Disabled -> {
                    base.copy(
                        centerLabel = HomeTexts.WAKE,
                        centerValue = HomeTexts.DISABLED,
                        cardTitle = HomeTexts.WAKE_DISABLED,
                        cardLines = listOf(HomeTexts.NOTHING_PLANNED),
                    )
                }

                HomeMode.NothingArmed -> {
                    base.copy(
                        centerLabel = HomeTexts.WAKE,
                        cardTitle = HomeTexts.WAKE_ENABLED,
                        cardLines = listOf(HomeTexts.NOTHING_PLANNED),
                    )
                }

                // Le centre compte jusqu'au coucher tant qu'il n'est pas atteint, puis jusqu'au réveil,
                // même si le blocage n'est pas encore en vigueur (décision de Mehdi du 2026-10-31).
                HomeMode.Scheduled -> {
                    val (trigger, blockingStart) = scheduledInstants(pending, plan, zone, now)
                    val beforeBedtime = blockingStart != null && blockingStart > now
                    base.copy(
                        centerLabel = if (beforeBedtime) HomeTexts.BEDTIME_IN else HomeTexts.WAKE_IN,
                        centerValue = countdown(now, if (beforeBedtime) blockingStart else trigger),
                        cardTitle = HomeTexts.WAKE_ENABLED,
                        cardLines =
                            listOfNotNull(
                                blockingStart?.takeIf { it > now }?.let {
                                    HomeTexts.blockingIn(
                                        minutesUntil(now, it),
                                    )
                                },
                                sleep,
                            ),
                    )
                }
            }
        }

        /** Instants de la session en attente ; à défaut, ceux de la prochaine occurrence calculée. */
        private fun scheduledInstants(
            pending: SessionSnapshotDto?,
            plan: WakePlan,
            zone: String,
            now: Long,
        ): Pair<Long?, Long?> {
            val occurrence =
                if (pending ==
                    null
                ) {
                    sources.plan.facade
                        .nextOccurrence(plan, zone, now)
                        .occurrence
                } else {
                    null
                }
            val trigger = pending?.wakeSchedule?.triggerAtEpochMillis ?: occurrence?.wakeSchedule?.triggerAtEpochMillis
            val blockingStart =
                pending?.blockingSchedule?.startsAtEpochMillis ?: occurrence?.blockingSchedule?.startsAtEpochMillis
            return trigger to blockingStart
        }

        private companion object {
            const val MESSAGE_MILLIS = 4_000L
            val ALL_DAYS = (1..DAYS_PER_WEEK).toSet()
        }
    }

private const val MINUTE_MILLIS = 60_000.0
private const val MINUTES_PER_HOUR = 60
private const val DAYS_PER_WEEK = 7

private fun dayModeOf(days: Set<Int>): DayMode =
    when {
        days.isEmpty() -> DayMode.ONE_OFF
        days.size == DAYS_PER_WEEK -> DayMode.DAILY
        else -> DayMode.CUSTOM
    }

/** Minutes entamées jusqu'à [target] : « 56 min » à 6:04 pour 7:00, jamais zéro avant l'heure. */
private fun minutesUntil(
    now: Long,
    target: Long,
): Int = ceil((target - now).coerceAtLeast(0) / MINUTE_MILLIS).toInt()

private fun countdown(
    now: Long,
    target: Long?,
): String = target?.let { HomeTexts.countdown(minutesUntil(now, it)) }.orEmpty()

private fun minutesOfDay(
    epochMillis: Long,
    zone: String,
): Int {
    val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of(zone)).toLocalTime()
    return time.hour * MINUTES_PER_HOUR + time.minute
}

private fun String.toMinutes(): Int = LocalTime.parse(this).let { it.hour * MINUTES_PER_HOUR + it.minute }

/** « 07:05 », chiffres latins quelle que soit la locale (`WakePlan` attend `HH:mm`). */
private fun Int.toIso(): String =
    "${(this / MINUTES_PER_HOUR).toString().padStart(2, '0')}:${(this % MINUTES_PER_HOUR).toString().padStart(2, '0')}"
