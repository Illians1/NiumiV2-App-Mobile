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
import com.niumi.system.readiness.ReadinessInput
import com.niumi.system.readiness.ReadinessOutcome
import com.niumi.system.readiness.toActivationPolicyInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Diagnostic avant activation (SPEC_ANDROID §13). Le ViewModel ne décide rien : il rejoue
 * `DeviceReadinessChecker`, convertit le rapport avec `toActivationPolicyInput()` et laisse
 * `NiumiCoreFacade.evaluateActivation` trancher. Aucune règle d'activation n'est réimplémentée
 * côté Android (règle d'or de CLAUDE.md, SPEC_CORE_KMP §7.4).
 *
 * `refresh()` est rappelé sur chaque `ON_RESUME` et chaque retour du focus de la fenêtre
 * (fermeture du volet rapide) : « recalculer l'état après chaque retour des réglages » (§13).
 * C'est aussi ce qui fait passer au vert l'exemption d'énergie, détectée depuis l'étape 25 :
 * aucune confirmation à donner au retour des réglages. Au retour d'un réglage, les deux
 * déclencheurs se suivent : la relance en cours est annulée, la dernière fait foi.
 */
@HiltViewModel
class ReadinessViewModel
    @Inject
    constructor(
        private val readinessChecker: DeviceReadinessChecker,
        private val facade: NiumiCoreFacade,
    ) : ViewModel() {
        var state by mutableStateOf(ReadinessUiState())
            private set

        init {
            refresh()
        }

        private var refreshJob: Job? = null

        fun refresh() {
            refreshJob?.cancel()
            refreshJob = viewModelScope.launch { state = evaluate() }
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
            ReadinessItem(
                id = check.id,
                message = ReadinessMessages.forCheck(check.id),
                label = ReadinessMessages.labelFor(check.id),
                severity = check.severity,
                outcome = check.outcome,
                action = check.action,
                actionLabel = ReadinessMessages.actionLabelFor(check.id),
                isActionAvailable = check.action !in UNAVAILABLE_ACTIONS,
            ).withRevisitLabel()

        /**
         * Une étape de parcours satisfaite garde son action mais change de libellé : « Changer de
         * boîtier » plutôt que « Associer mon boîtier », qui contredirait la ligne verte juste
         * au-dessus (§15, ne jamais afficher un faux état).
         */
        private fun ReadinessItem.withRevisitLabel(): ReadinessItem =
            if (isRevisitable) copy(actionLabel = ReadinessMessages.revisitLabelFor(id)) else this

        private companion object {
            /**
             * Recours dont l'écran n'existe pas : `Unsupported` n'en a sur aucune version, le
             * matériel manque. `StartPairing` et `OpenAppPicker` en sont sortis à l'étape 13,
             * `FixTime` à l'étape 14, leurs écrans étant livrés. Ce jeu se vide au fil des étapes ;
             * il ne doit jamais servir à masquer un contrôle.
             */
            val UNAVAILABLE_ACTIONS = setOf(ReadinessAction.Unsupported)
        }
    }
