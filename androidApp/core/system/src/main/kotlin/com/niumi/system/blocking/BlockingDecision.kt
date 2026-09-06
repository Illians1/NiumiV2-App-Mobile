package com.niumi.system.blocking

/** Action à exécuter à la suite d'un changement de fenêtre (SPEC_ANDROID §12.2). */
sealed interface BlockAction {
    data object None : BlockAction

    data class GoHome(
        val packageName: String,
        val displayName: String,
    ) : BlockAction
}

/**
 * Algorithme pur de SPEC_ANDROID §12.2 : « à chaque changement de fenêtre, lire le package au
 * premier plan […] si le package est bloqué : exécuter `GLOBAL_ACTION_HOME`, afficher
 * l'overlay ». Ne connaît ni `AccessibilityEvent`, ni `Context`, ni `WindowManager` — le
 * service ne fait que traduire cette décision en appels Android.
 */
object BlockingDecision {
    fun decide(
        state: BlockedPackagesState,
        foregroundPackage: String,
        selfPackageName: String,
    ): BlockAction {
        val packages =
            when (state) {
                BlockedPackagesState.Inactive -> emptySet()
                is BlockedPackagesState.Active -> state.packages
                is BlockedPackagesState.Releasing -> state.effectivePackages
            }
        val blocked = packages.firstOrNull { it.packageName == foregroundPackage }
        return if (foregroundPackage == selfPackageName || blocked == null) {
            BlockAction.None
        } else {
            BlockAction.GoHome(blocked.packageName, blocked.displayNameSnapshot)
        }
    }
}
