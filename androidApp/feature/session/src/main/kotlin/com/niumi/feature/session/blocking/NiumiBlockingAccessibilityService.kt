package com.niumi.feature.session.blocking

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.blocking.BlockAction
import com.niumi.system.blocking.BlockedPackagesProjection
import com.niumi.system.blocking.BlockingDecision
import com.niumi.system.common.OperationResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Blocage comportemental (SPEC_ANDROID §12.2). Lit uniquement `event.packageName` : ne parcourt
 * jamais l'arbre d'accessibilité (`getRootInActiveWindow`), ne lit ni texte ni saisies, ne
 * transmet rien à un serveur (aucun accès réseau dans ce module). La configuration XML associée
 * (`niumi_accessibility_service.xml`) filtre déjà les types d'événement reçus.
 *
 * [projection] reste en mémoire (étape 15 : lecture Room ou snapshot) : un process tué perd
 * la liste de blocage active jusqu'à la prochaine décision d'activation. Limite documentée dans
 * `ETAPE-05.md`, pas un contournement — aucune trace durable n'existe encore à relire.
 *
 * L'overlay est construit ici, avec le service comme `Context`, et non injecté : seul le
 * service porte le token de fenêtre autorisant `TYPE_ACCESSIBILITY_OVERLAY`
 * (voir [WindowManagerBlockOverlayController]). Même motif que `AlarmNfcScanCoordinator`,
 * instancié à la main par `AlarmActivity` faute d'un contexte injectable pertinent.
 */
@AndroidEntryPoint
class NiumiBlockingAccessibilityService : AccessibilityService() {
    @Inject
    lateinit var projection: BlockedPackagesProjection

    @Inject
    lateinit var technicalEventLog: TechnicalEventLog

    private var overlayController: BlockOverlayController? = null
    private var lastBlockedPackage: String? = null
    private var lastBlockAtElapsedMillis: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Peut être rappelé à chaque reconnexion : ne recrée l'overlay que si nécessaire.
        if (overlayController == null) {
            overlayController = WindowManagerBlockOverlayController(this)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        when (
            val action =
                BlockingDecision.decide(
                    state = projection.current(),
                    foregroundPackage = packageName,
                    selfPackageName = applicationContext.packageName,
                )
        ) {
            BlockAction.None -> {
                // Volontairement sans `hide()`. SPEC_ANDROID §12.2 prévoit un retrait « dès que
                // le package bloqué n'est plus au premier plan », mais Niumi renvoie lui-même à
                // l'accueil : GLOBAL_ACTION_HOME provoque immédiatement un événement pour le
                // launcher, non bloqué, qui effacerait l'overlay avant qu'il soit visible. Le
                // retrait est donc porté par le minuteur de 3 s, la seconde règle de §12.2.
                // Contradiction de spec signalée et tranchée dans ETAPE-05.md.
            }

            is BlockAction.GoHome -> {
                if (shouldBlockNow(action.packageName)) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    showOverlay(action.displayName)
                    technicalEventLog.log(TechnicalEventType.BLOCK_APPLIED, packageName = action.packageName)
                }
            }
        }
    }

    /**
     * Anti-rebond : une application au premier plan émet des rafales d'événements, et sans cette
     * garde chaque événement déclencherait un `GLOBAL_ACTION_HOME` et une entrée `BLOCK_APPLIED`.
     * Le journal technique est borné à 200 entrées (SPEC_ANDROID §17) : il serait saturé en
     * quelques secondes. Une nouvelle tentative sur la même application n'est reconnue qu'après
     * [BLOCK_DEBOUNCE_MS]. Appelé depuis le thread principal du service uniquement.
     */
    private fun shouldBlockNow(packageName: String): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        val isRepeat =
            packageName == lastBlockedPackage && now - lastBlockAtElapsedMillis < BLOCK_DEBOUNCE_MS
        if (isRepeat) return false
        lastBlockedPackage = packageName
        lastBlockAtElapsedMillis = now
        return true
    }

    /**
     * Le retour à l'accueil a déjà eu lieu quand l'overlay échoue : le blocage reste effectif,
     * seule l'explication manque. On journalise plutôt que de laisser remonter (SPEC_ANDROID
     * §17 : `OEM_RESTRICTION_SUSPECTED` couvre un refus de la couche système).
     */
    private fun showOverlay(displayName: String) {
        val result = overlayController?.show(displayName) ?: return
        if (result is OperationResult.Failure) {
            technicalEventLog.log(TechnicalEventType.OEM_RESTRICTION_SUSPECTED)
        }
    }

    override fun onInterrupt() {
        overlayController?.hide()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        overlayController?.hide()
        overlayController = null
        return super.onUnbind(intent)
    }

    private companion object {
        const val BLOCK_DEBOUNCE_MS = 1_000L
    }
}
