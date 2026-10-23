package com.niumi.feature.session.blocking

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.niumi.database.blocking.BlockedPackagesState
import com.niumi.database.logging.TechnicalEventDetails
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.apps.PackageQuery
import com.niumi.system.blocking.BlockAction
import com.niumi.system.blocking.BlockScreenDismissal
import com.niumi.system.blocking.BlockedPackagesProjection
import com.niumi.system.blocking.BlockingDecision
import com.niumi.system.blocking.BlockingProjectionRefresher
import com.niumi.system.blocking.InMemoryAccessibilityServiceBinding
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
 * L'écran 7 est construit ici, avec le service comme `Context`, et non injecté : seul le
 * service porte le token de fenêtre autorisant `TYPE_ACCESSIBILITY_OVERLAY`
 * (voir [WindowManagerOverlayWindowHost]). Même motif que `AlarmNfcScanCoordinator`,
 * instancié à la main par `AlarmActivity` faute d'un contexte injectable pertinent.
 *
 * Depuis l'étape 39 de la refonte, l'écran 7 reste affiché jusqu'à « Fermer » ou « Ouvrir Niumi »
 * (Q6) ; le service le retire aussi à la fin du blocage, à l'extinction de l'écran (sinon il
 * reviendrait par-dessus l'écran de verrouillage), dans [onInterrupt] et dans [onUnbind].
 *
 * Le service annonce lui-même sa liaison au diagnostic (SPEC_ANDROID §13.1, 2026-10-15) par
 * [binding] : relié à [onServiceConnected], délié à [onUnbind] et [onDestroy]. Un réglage système
 * ne peut pas le dire à sa place — l'interrupteur global d'accessibilité reste à 1 tant qu'une
 * autre application en tient un service, Niumi délié ou non.
 */
@AndroidEntryPoint
class NiumiBlockingAccessibilityService : AccessibilityService() {
    @Inject
    lateinit var projection: BlockedPackagesProjection

    @Inject
    lateinit var refresher: BlockingProjectionRefresher

    @Inject
    lateinit var technicalEventLog: TechnicalEventLog

    @Inject
    lateinit var binding: InMemoryAccessibilityServiceBinding

    @Inject
    lateinit var packageQuery: PackageQuery

    /**
     * `Dispatchers.Main.immediate` : le cache de la projection est lu par
     * `onAccessibilityEvent` sur le thread principal, l'y écrire aussi évite toute question de
     * visibilité entre threads au-delà du `@Volatile` de la projection.
     */
    private var refreshScope: CoroutineScope? = null
    private var overlayController: BlockOverlayController? = null
    private var lastBlockedPackage: String? = null
    private var lastBlockAtElapsedMillis: Long = 0L
    private var screenOffReceiverRegistered = false

    /**
     * Lanceurs lus quand l'écran 7 paraît, pour [BlockScreenDismissal] : `null` si la lecture a
     * échoué, auquel cas aucune application ne retire l'écran (le lanceur l'effacerait aussitôt).
     */
    private var homePackagesWhileShown: Set<String>? = null

    /** Extinction de l'écran : l'écran 7 ne doit pas attendre le rallumage par-dessus le verrouillage. */
    private val screenOffReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                overlayController?.hide()
            }
        }

    /**
     * Dernier package reçu, bloqué ou non — distinct de [lastBlockedPackage], qui ne retient que les
     * blocages effectifs pour l'anti-rebond. Sert à la relecture du début différé (SPEC_ANDROID
     * §12.4) : sans lui, une application bloquée déjà au premier plan à l'heure de début resterait
     * ouverte jusqu'au prochain changement de fenêtre.
     */
    private var lastForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Hors des gardes qui suivent : une reconnexion doit remarquer la liaison même si l'overlay
        // et l'abonnement tiennent déjà.
        binding.markConnected()
        // Peut être rappelé à chaque reconnexion : ne recrée ni l'overlay ni l'abonnement si
        // l'un et l'autre tiennent déjà.
        if (overlayController == null) {
            overlayController = WindowManagerBlockOverlayController(this, onOpenNiumi = ::openNiumi)
        }
        if (!screenOffReceiverRegistered) {
            // Diffusion système protégée : reçue malgré `RECEIVER_NOT_EXPORTED`.
            ContextCompat.registerReceiver(
                this,
                screenOffReceiver,
                IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            screenOffReceiverRegistered = true
        }
        if (refreshScope == null) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            refreshScope = scope
            scope.launch { refresher.observeDecisions(::onBlockingActivated, ::onBlockingDeactivated) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        lastForegroundPackage = packageName
        applyDecisionFor(packageName)
    }

    /**
     * Début d'un blocage différé (SPEC_ANDROID §12.4, Lot 6) : la projection vient de devenir active,
     * et l'application au premier plan n'a produit aucun événement puisque la fenêtre n'a pas changé.
     * La décision est donc **rejouée** sur le dernier package vu, par le même chemin et sous le même
     * anti-rebond qu'un événement ordinaire — `BlockingDecision.decide` est réutilisée telle quelle.
     *
     * Seul un nom de package **déjà reçu** par les événements est relu : aucune lecture de l'arbre
     * d'accessibilité, aucune inspection de fenêtre, donc rien au-delà de l'usage déclaré à Google
     * Play (§12.3). `NiumiBlockingAccessibilityServiceSourceTest` l'interdit jusque dans ce
     * commentaire — les jetons proscrits y sont cherchés littéralement, texte compris.
     *
     * Appelé depuis la coroutine du coordinateur, pas depuis le thread principal : l'écran 7 passe par
     * `WindowManager`, qui exige le thread principal. Le travail est donc reposté sur [refreshScope],
     * dont le dispatcher est `Main.immediate`.
     */
    private fun onBlockingActivated(state: BlockedPackagesState.Active) {
        val packageName = lastForegroundPackage ?: return
        refreshScope?.launch {
            // La projection est **relue** ici, pas reprise de [state] : entre la notification et ce
            // bloc reposté, un scan a pu libérer la session, et renvoyer alors l'utilisateur à
            // l'accueil serait un blocage après la fin de sa session. La session notifiée sert de
            // garde — la projection doit toujours décrire la même.
            val current = projection.current()
            if (current is BlockedPackagesState.Active && current.sessionId == state.sessionId) {
                applyDecisionFor(packageName)
            }
        }
    }

    /**
     * Fin du blocage (étape 39) : le scan a libéré la session, l'écran 7 n'a plus rien à expliquer.
     * Même repost sur le thread principal que [onBlockingActivated], et pour la même raison.
     */
    private fun onBlockingDeactivated() {
        refreshScope?.launch { overlayController?.hide() }
    }

    private fun applyDecisionFor(packageName: String) {
        when (
            val action =
                BlockingDecision.decide(
                    state = projection.current(),
                    foregroundPackage = packageName,
                    selfPackageName = applicationContext.packageName,
                )
        ) {
            BlockAction.None -> {
                // Le lanceur, l'interface système et Niumi gardent l'écran 7 : GLOBAL_ACTION_HOME
                // provoque aussitôt un événement du lanceur, qui l'effacerait avant qu'il soit lu
                // (ETAPE-05.md). Toute autre application non bloquée le retire, un appel entrant
                // en premier lieu (décision de Mehdi du 2026-10-22, étape 39 de la refonte).
                val home = homePackagesWhileShown
                if (overlayController?.isShowing == true &&
                    home != null &&
                    BlockScreenDismissal.dismisses(packageName, applicationContext.packageName, home)
                ) {
                    overlayController?.hide()
                }
            }

            is BlockAction.GoHome -> {
                if (shouldBlockNow(action.packageName)) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    showBlockScreen(action.displayName)
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
     * Le retour à l'accueil a déjà eu lieu quand l'écran 7 échoue : le blocage reste effectif,
     * seule l'explication manque. On journalise plutôt que de laisser remonter (SPEC_ANDROID
     * §17 : `OEM_RESTRICTION_SUSPECTED` couvre un refus de la couche système).
     *
     * Écran éteint, rien n'est posé : un blocage peut être décidé ainsi (début différé rejoué sur
     * le dernier paquet vu, §12.4), et l'extinction, déjà passée, ne retirerait plus l'écran 7, qui
     * paraîtrait par-dessus l'écran de verrouillage au rallumage. Le retour à l'accueil suffit.
     */
    private fun showBlockScreen(appName: String) {
        val power = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (power?.isInteractive == false) return
        val result = overlayController?.show(appName) ?: return
        if (result is OperationResult.Failure) {
            technicalEventLog.log(TechnicalEventType.OEM_RESTRICTION_SUSPECTED)
        } else if (result == OperationResult.Success) {
            homePackagesWhileShown = runCatching { packageQuery.homePackages() }.getOrNull()
        }
    }

    /**
     * « Ouvrir Niumi » de l'écran 7 : ouvre Niumi comme son icône du lanceur, sur un toucher de
     * l'utilisateur (SPEC_ANDROID §12.2). Ce n'est pas un moyen de bloquer : le retour à l'accueil a
     * déjà eu lieu. Un service d'accessibilité relié est exempté des restrictions de lancement en
     * arrière-plan ; un refus du système est journalisé, jamais propagé.
     */
    private fun openNiumi() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch == null) {
            technicalEventLog.log(TechnicalEventType.OEM_RESTRICTION_SUSPECTED)
            return
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            startActivity(launch)
        } catch (_: ActivityNotFoundException) {
            technicalEventLog.log(TechnicalEventType.OEM_RESTRICTION_SUSPECTED)
        } catch (_: SecurityException) {
            technicalEventLog.log(TechnicalEventType.OEM_RESTRICTION_SUSPECTED)
        }
    }

    override fun onInterrupt() {
        overlayController?.hide()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        binding.markDisconnected()
        overlayController?.hide()
        overlayController = null
        if (screenOffReceiverRegistered) {
            try {
                unregisterReceiver(screenOffReceiver)
            } catch (_: IllegalArgumentException) {
                // Déjà désenregistré par le système : rien à faire.
            }
            screenOffReceiverRegistered = false
        }
        refreshScope?.cancel()
        refreshScope = null
        return super.onUnbind(intent)
    }

    /** Filet : un service détruit sans `onUnbind` ne doit pas rester annoncé comme relié. */
    override fun onDestroy() {
        binding.markDisconnected()
        super.onDestroy()
    }

    private companion object {
        const val BLOCK_DEBOUNCE_MS = 1_000L
    }
}
