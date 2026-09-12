package com.niumi.feature.session.blocking

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.niumi.database.logging.TechnicalEventDetails
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.blocking.BlockAction
import com.niumi.system.blocking.BlockedPackagesProjection
import com.niumi.system.blocking.BlockingDecision
import com.niumi.system.blocking.BlockingProjectionRefresher
import com.niumi.system.common.OperationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Blocage comportemental (SPEC_ANDROID §12.2). Lit uniquement `event.packageName` : ne parcourt
 * jamais l'arbre d'accessibilité (`getRootInActiveWindow`), ne lit ni texte ni saisies, ne
 * transmet rien à un serveur (aucun accès réseau dans ce module). La configuration XML associée
 * (`niumi_accessibility_service.xml`) filtre déjà les types d'événement reçus.
 *
 * [projection] est reconstruite depuis la persistance depuis l'étape 15 : [refresher] la réaligne
 * à chaque (re)connexion du service et à chaque décision de session publiée (SPEC_ANDROID §12.2,
 * « le service doit recharger l'état actif depuis Room ou le snapshot après recréation »). Un
 * processus tué ne fait donc plus disparaître le blocage d'une session encore active, ce qui
 * était la limite de l'étape 5 (`ETAPE-05.md`).
 *
 * Le service ne porte que le câblage : un scope qu'il annule lui-même dans [onUnbind]. La logique
 * d'abonnement vit dans [com.niumi.system.blocking.BlockingProjectionRefresher], prouvable en JVM
 * — un `AccessibilityService` ne s'instancie pas en test unitaire.
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
    lateinit var refresher: BlockingProjectionRefresher

    @Inject
    lateinit var technicalEventLog: TechnicalEventLog

    /**
     * `Dispatchers.Main.immediate` : le cache de la projection est lu par
     * `onAccessibilityEvent` sur le thread principal, l'y écrire aussi évite toute question de
     * visibilité entre threads au-delà du `@Volatile` de la projection.
     */
    private var refreshScope: CoroutineScope? = null
    private var overlayController: BlockOverlayController? = null
    private var lastBlockedPackage: String? = null
    private var lastBlockAtElapsedMillis: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Peut être rappelé à chaque reconnexion : ne recrée ni l'overlay ni l'abonnement si
        // l'un et l'autre tiennent déjà.
        if (overlayController == null) {
            overlayController = WindowManagerBlockOverlayController(this)
        }
        if (refreshScope == null) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            refreshScope = scope
            scope.launch { refresher.observeDecisions() }
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
                    technicalEventLog.log(
                        TechnicalEventType.BLOCK_APPLIED,
                        detailsJson = TechnicalEventDetails.packageName(action.packageName),
                    )
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
        refreshScope?.cancel()
        refreshScope = null
        return super.onUnbind(intent)
    }

    private companion object {
        const val BLOCK_DEBOUNCE_MS = 1_000L
    }
}
