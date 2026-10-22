package com.niumi.feature.setup.permissions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.system.permissions.NiumiPermission
import com.niumi.system.permissions.PermissionStatus
import com.niumi.system.permissions.PermissionStatusSource
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.setup.SetupPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Écran 2 (`SPECIFICATION.md` §5, SPEC_ANDROID §13). Les tuiles sont relues à chaque `ON_RESUME`
 * et à chaque retour du focus de la fenêtre (§13, écart 10) ; le NFC n'a pas de tuile (D10). Un refus
 * ne fait rien de particulier, la tuile reste simplement non cochée. Au retour d'un réglage les
 * déclencheurs se suivent ; la relance en cours est annulée et la dernière fait foi.
 *
 * Deux cas se relisent en plus d'eux-mêmes, faute d'autre déclencheur : un service d'accessibilité
 * pas encore jugeable, et la tuile « Batterie » juste après ses réglages, dont HyperOS reporte le
 * choix après le retour dans Niumi ([BATTERY_SETTLE_RECHECK_MILLIS]).
 *
 * « Autoriser » traite le premier manque de la tuile, un seul à la fois (§13). Aucun réglage n'est
 * modifié à la place de l'utilisateur (§12.3).
 */
@HiltViewModel
class PermissionsViewModel
    @Inject
    constructor(
        private val source: PermissionStatusSource,
        private val setupPreferences: SetupPreferences,
    ) : ViewModel() {
        var state by mutableStateOf(PermissionsUiState())
            private set

        private var statuses: List<PermissionStatus> = emptyList()

        private var refreshJob: Job? = null

        /**
         * Relectures restantes de la tuile « Batterie » : armées quand « Autoriser » ouvre ses
         * réglages, consommées au retour, remises à zéro dès que la tuile est accordée.
         */
        private var batteryRereadsLeft = 0

        init {
            refresh()
            viewModelScope.launch {
                val requested = setupPreferences.isNotificationPermissionRequested()
                state = state.copy(notificationRequestedBefore = requested || state.notificationRequestedBefore)
            }
        }

        fun refresh() {
            refreshJob?.cancel()
            refreshJob =
                viewModelScope.launch {
                    do {
                        val read = source.status()
                        statuses = read
                        state =
                            state.copy(
                                tiles = read.map { PermissionTileState(it.permission, it.granted) },
                                isLoading = false,
                                dialog = state.dialog?.takeIf { it in dialogsOf(read) },
                            )
                        // Un contrôle pas encore jugeable (service d'accessibilité dans sa fenêtre
                        // de liaison) se relit de lui-même : aucun autre déclencheur ne viendrait.
                        val awaiting = read.any { it.isAwaitingJudgement }
                        // La tuile « Batterie » aussi, un temps, après ses réglages : HyperOS reporte
                        // le choix sur la liste blanche après le retour dans Niumi.
                        val battery = read.firstOrNull { it.permission == NiumiPermission.BATTERY }
                        val batteryGranted = battery?.granted ?: true
                        if (batteryGranted) batteryRereadsLeft = 0
                        val settling = batteryRereadsLeft > 0
                        if (settling) batteryRereadsLeft--
                        when {
                            settling -> delay(BATTERY_SETTLE_RECHECK_MILLIS)
                            awaiting -> delay(AWAITING_JUDGEMENT_RECHECK_MILLIS)
                        }
                    } while (awaiting || settling)
                }
        }

        fun authorize(permission: NiumiPermission): PermissionStep {
            val pending = statuses.firstOrNull { it.permission == permission }?.pending
            val action = pending?.action
            val dialog = action?.let(::dialogOf)
            if (dialog != null) state = state.copy(dialog = dialog)
            // Le choix fait dans ces réglages arrive après le retour : relire un temps (`refresh`).
            if (action == ReadinessAction.OpenBatterySettings) batteryRereadsLeft = BATTERY_SETTLE_REREADS
            return when {
                dialog != null -> PermissionStep.None

                // Avant Android 13, ce contrôle porte l'action du canal du réveil ; seuls les
                // réglages de notification de Niumi rétablissent des notifications toutes coupées.
                pending?.id == ReadinessCheckId.NOTIFICATIONS -> PermissionStep.RequestNotifications

                action == ReadinessAction.RequestNotificationPermission -> PermissionStep.RequestNotifications

                action is ReadinessAction.OpenChannelSettings -> PermissionStep.OpenSettings(action)

                action == ReadinessAction.OpenBatterySettings -> PermissionStep.OpenSettings(action)

                else -> PermissionStep.None
            }
        }

        /** Appelé juste avant d'ouvrir la fenêtre système des notifications. */
        fun markNotificationPermissionRequested() {
            state = state.copy(notificationRequestedBefore = true)
            viewModelScope.launch { setupPreferences.markNotificationPermissionRequested() }
        }

        /**
         * Croix ou toucher à côté : la fenêtre se ferme et les contrôles sont relus, l'utilisateur
         * ayant pu régler quelque chose entre-temps (réglages ouverts depuis la fenêtre).
         */
        fun dismissDialog() {
            state = state.copy(dialog = null)
            refresh()
        }

        /** « J'ai verrouillé Niumi » : le verrou ne se pose pas dans Niumi, le bouton ne fait que relire. */
        fun confirmRecentsLock() = dismissDialog()

        companion object {
            /**
             * Délai entre deux relectures tant qu'une tuile attend son verdict. La fenêtre de
             * liaison dure 30 s (`UnlockSettling`) ; le service se relie en général en moins de
             * 4 s (mesures du 2026-09-27) : une relecture toutes les 3 s rend la main vite sans
             * solliciter les sources plus de dix fois.
             */
            const val AWAITING_JUDGEMENT_RECHECK_MILLIS: Long = 3_000L

            /**
             * Relecture de la tuile « Batterie » après ses réglages. Mesuré le 2026-10-22 sur
             * HyperOS : la page « Économiseur de batterie » de Xiaomi reporte le choix sur la liste
             * blanche moins d'une seconde après sa fermeture, et aucune annonce publique ne le signale
             * (`ACTION_POWER_SAVE_WHITELIST_CHANGED` est `@hide`). Au retour immédiat, `ON_RESUME` et
             * le focus lisent encore `false`. Six relectures espacées de 500 ms couvrent trois fois la
             * mesure, puis l'écran s'arrête : un refus laisse la tuile à faire.
             */
            const val BATTERY_SETTLE_RECHECK_MILLIS: Long = 500L
            const val BATTERY_SETTLE_REREADS: Int = 6

            /** Divulgation §12.3 toujours avant les réglages d'accessibilité (Google Play). */
            private fun dialogOf(action: ReadinessAction): PermissionDialog? =
                when (action) {
                    ReadinessAction.OpenAccessibilitySettings -> PermissionDialog.CONSENT
                    ReadinessAction.LockInRecents -> PermissionDialog.RECENTS_LOCK
                    ReadinessAction.ShowExactAlarmDiagnostic -> PermissionDialog.EXACT_ALARM
                    else -> null
                }

            /** Une fenêtre ne reste ouverte que tant que son manque existe encore. */
            private fun dialogsOf(statuses: List<PermissionStatus>): Set<PermissionDialog> =
                statuses.mapNotNull { status -> status.pending?.action?.let(::dialogOf) }.toSet()
        }
    }
