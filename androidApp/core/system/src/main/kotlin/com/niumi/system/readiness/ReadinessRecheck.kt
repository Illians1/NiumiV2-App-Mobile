package com.niumi.system.readiness

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Provider

/**
 * Re-contrôle différé de la surveillance de §13.1 à la fin de la fenêtre de liaison ([UnlockSettling],
 * étape 25). Sans lui, un service d'accessibilité **réellement** non relié — mort du processus sur
 * HyperOS — dont le verdict a été suspendu ne serait jugé qu'au déclencheur suivant, peut-être au
 * réveil.
 */
fun interface ReadinessRecheck {
    /** Sans effet hors fenêtre de liaison, et quand un re-contrôle est déjà en attente. */
    fun recheckAfterSettling()
}

/**
 * Meilleur effort, dans ce processus : s'il meurt avant la fin de la fenêtre, le verdict revient au
 * déclencheur suivant, comme tout ce qui arrive pendant que Niumi est endormi (§13.1, « Limite à ne
 * pas masquer »). [trigger] est un `Provider` : le déclencheur dépend lui-même du moniteur, qui
 * dépend de ce re-contrôle.
 */
class CoroutineReadinessRecheck(
    private val settling: UnlockSettling,
    private val trigger: Provider<ForegroundReadinessTrigger>,
    dispatcher: CoroutineDispatcher,
) : ReadinessRecheck {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    @Volatile
    private var pending: Job? = null

    @Synchronized
    override fun recheckAfterSettling() {
        val remaining = settling.remainingMillis()
        if (remaining <= 0 || pending?.isActive == true) return
        pending =
            scope.launch {
                delay(remaining)
                trigger.get().evaluateAsync()
            }
    }
}
