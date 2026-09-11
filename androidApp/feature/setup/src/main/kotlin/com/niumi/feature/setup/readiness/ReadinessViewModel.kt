package com.niumi.feature.setup.readiness

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheck
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessInput
import com.niumi.system.readiness.ReadinessOutcome
import com.niumi.system.readiness.toActivationPolicyInput
import com.niumi.system.setup.SetupPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Diagnostic avant activation (SPEC_ANDROID §13). Le ViewModel ne décide rien : il rejoue
 * `DeviceReadinessChecker`, convertit le rapport avec `toActivationPolicyInput()` et laisse
 * `NiumiCoreFacade.evaluateActivation` trancher. Aucune règle d'activation n'est réimplémentée
 * côté Android (règle d'or de CLAUDE.md, SPEC_CORE_KMP §7.4).
 *
 * `refresh()` est rappelé sur chaque `ON_RESUME` : « recalculer l'état après chaque retour des
 * réglages » (§13).
 */
@HiltViewModel
class ReadinessViewModel
    @Inject
    constructor(
        private val readinessChecker: DeviceReadinessChecker,
        private val facade: NiumiCoreFacade,
        private val setupPreferences: SetupPreferences,
    ) : ViewModel() {
        /**
         * Deux temps pour le contrôle d'énergie : d'abord ouvrir les réglages, puis confirmer.
         * §13 exige la confirmation de l'utilisateur parce que la détection AOSP est partielle, et
         * §13 impose une seule action principale à la fois — d'où une bascule plutôt que deux
         * boutons côte à côte. L'état est volontairement en mémoire : la question doit être reposée
         * à chaque visite de l'écran.
         */
        private var batterySettingsOpened = false

        var state by mutableStateOf(ReadinessUiState())
            private set

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch { state = evaluate() }
        }

        fun onBatterySettingsOpened() {
            batterySettingsOpened = true
            state =
                state.copy(
                    items = state.items.map(::withBatteryLabel),
                    primary = state.primary?.let(::withBatteryLabel),
                )
        }

        fun confirmBatteryExemption() {
            viewModelScope.launch {
                setupPreferences.setBatteryExemptionConfirmed(confirmed = true)
                batterySettingsOpened = false
                state = evaluate()
            }
        }

        private suspend fun evaluate(): ReadinessUiState {
            val report = readinessChecker.check(ReadinessInput())
            val result = facade.evaluateActivation(report.toActivationPolicyInput())
            val items =
                report.checks
                    .filter { it.outcome != ReadinessOutcome.NOT_APPLICABLE }
                    .map(::toItem)
            return ReadinessUiState(
                items = items,
                primary = items.firstOrNull { it.outcome == ReadinessOutcome.FAILED },
                isAllowed = result.allowed,
                isLoading = false,
                nowEpochMillis = report.nowEpochMillis,
            )
        }

        private fun toItem(check: ReadinessCheck): ReadinessItem =
            withBatteryLabel(
                ReadinessItem(
                    id = check.id,
                    message = ReadinessMessages.forCheck(check.id),
                    label = ReadinessMessages.labelFor(check.id),
                    severity = check.severity,
                    outcome = check.outcome,
                    action = check.action,
                    actionLabel = ReadinessMessages.actionLabelFor(check.id),
                    isActionAvailable = check.action !in UNAVAILABLE_ACTIONS,
                ),
            )

        private fun withBatteryLabel(item: ReadinessItem): ReadinessItem =
            if (item.id == ReadinessCheckId.BATTERY_OPTIMIZATION && batterySettingsOpened) {
                item.copy(actionLabel = ReadinessMessages.BATTERY_CONFIRM_LABEL)
            } else {
                item
            }

        private companion object {
            /**
             * Recours dont l'écran n'existe pas encore : l'association et le sélecteur arrivent à
             * l'étape 13. `Unsupported` n'a de recours sur aucune version : le matériel manque.
             * Ce jeu se vide au fil des étapes ; il ne doit jamais servir à masquer un contrôle.
             */
            val UNAVAILABLE_ACTIONS =
                setOf(
                    ReadinessAction.StartPairing,
                    ReadinessAction.OpenAppPicker,
                    ReadinessAction.FixTime,
                    ReadinessAction.Unsupported,
                )
        }
    }
