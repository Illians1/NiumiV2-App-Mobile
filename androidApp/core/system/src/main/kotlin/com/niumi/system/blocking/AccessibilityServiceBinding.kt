package com.niumi.system.blocking

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Le service d'accessibilité de Niumi est-il **relié dans ce processus** (SPEC_ANDROID §13.1,
 * décision du 2026-10-15) ? C'est le service lui-même qui le dit, à `onServiceConnected` et à
 * `onUnbind`/`onDestroy` — pas un réglage système.
 *
 * `Settings.Secure.ACCESSIBILITY_ENABLED` servait d'indice jusque-là ; c'est un interrupteur
 * **global**, à 1 dès qu'un service d'accessibilité quelconque tourne. Mesuré le 2026-10-15 sur
 * Xiaomi 25080RABDG / Android 16 : après une mort du processus, HyperOS ne reliait plus Niumi
 * (« Crashed services »), mais une autre application d'accessibilité tenait l'interrupteur à 1 —
 * le diagnostic lisait « actif », et l'incident `BLOCKING_PERMISSION_REVOKED` n'était jamais
 * consigné. Les mesures de l'étape 25 n'avaient réussi que parce que Niumi était alors seul.
 *
 * L'état meurt avec le processus : un processus neuf, y compris celui qu'Android relance après
 * une mort, lit « non relié » tant que le système n'a pas effectivement reconnecté le service. Le
 * service tourne dans le processus de l'application (aucun `android:process` dans le manifeste) ;
 * lui en donner un rendrait ce signal faux.
 */
interface AccessibilityServiceBinding {
    val isBound: Boolean
}

/**
 * Même motif que [PersistedBlockedPackagesProjection] : la classe concrète est injectée par
 * l'écrivain (le service, dans `:feature:session`), l'interface par les lecteurs.
 */
@Singleton
class InMemoryAccessibilityServiceBinding
    @Inject
    constructor() : AccessibilityServiceBinding {
        @Volatile
        private var bound: Boolean = false

        override val isBound: Boolean get() = bound

        fun markConnected() {
            bound = true
        }

        fun markDisconnected() {
            bound = false
        }
    }
