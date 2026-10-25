package com.niumi.feature.setup.apps

import com.niumi.system.plan.PlanChangeReason
import com.niumi.system.plan.WakePlanScheduler
import com.niumi.system.plan.WakePlanStore
import com.niumi.system.setup.SetupPreferences
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Ce que l'écran 4 touche hors de la sélection (étape 45) : le planning, pour réarmer après un
 * changement, et la fin de la mise en route. Regroupés pour rester sous `LongParameterList` de detekt.
 */
class AppSelectionPlanning
    @Inject
    constructor(
        val planStore: WakePlanStore,
        val scheduler: WakePlanScheduler,
        val setupPreferences: SetupPreferences,
    ) {
        /**
         * Planning allumé : la session en attente porte l'ancienne sélection, elle est désarmée et une
         * nouvelle est armée (`PlanChangeReason.APP_SELECTION`, décision 11 amendée). Un refus du
         * scheduler (appareil verrouillé) n'empêche pas l'enregistrement de la sélection.
         */
        suspend fun rearmAfterSelection() {
            val plan = planStore.read()
            if (!plan.enabled) return
            try {
                scheduler.apply(plan, PlanChangeReason.APP_SELECTION)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (
                @Suppress("TooGenericExceptionCaught") _: Exception,
            ) {
                // Appareil verrouillé : la sélection est enregistrée, le réarmement attendra le prochain changement.
            }
        }
    }
