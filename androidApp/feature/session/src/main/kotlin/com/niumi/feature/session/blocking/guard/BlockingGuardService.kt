package com.niumi.feature.session.blocking.guard

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.niumi.system.blocking.guard.BlockingGuardCommand
import com.niumi.system.blocking.guard.BlockingGuardDecision
import com.niumi.system.blocking.guard.BlockingGuardNotificationWatch
import com.niumi.system.blocking.guard.BlockingGuardPhase
import com.niumi.system.blocking.guard.BlockingGuardPolicy
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.notification.AndroidNotificationChannelRegistrar
import com.niumi.system.notification.BlockingGuardNotificationFactory
import com.niumi.system.notification.NiumiNotificationIds
import com.niumi.system.notification.RingingNotificationAction
import com.niumi.system.notification.RingingNotificationFactory
import com.niumi.system.notification.ScanRequestNotificationFactory
import com.niumi.system.session.SessionPersistenceGateway
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service de garde de session (SPEC_ANDROID §12.5, correctif A8 du 2026-10-19). Il ne fait rien
 * d'autre que tenir le processus au premier plan, avec sa notification, de l'armement à la fin de
 * la session : c'est ce qui empêche le nettoyeur de HyperOS de tuer Niumi quand l'utilisateur
 * balaie sa carte dans les récents, et donc Android de laisser le service d'accessibilité
 * « planté » et le blocage sans effet.
 *
 * Volontairement bête : démarré, mis à jour et arrêté par `BlockingGuardWatcher` (`:core:system`),
 * il ne décide de rien tant qu'il reçoit une commande. Relancé par le système sans commande après
 * une mort du processus (`START_STICKY`), il passe d'abord au premier plan — `startForeground` ne
 * peut pas attendre une lecture — puis relit lui-même la persistance
 * (`BlockingGuardPolicy.afterRestart`) : la réconciliation ne publie rien quand aucune session
 * n'existe plus, l'observateur ne pourrait donc pas l'arrêter.
 *
 * **De l'alarme au scan, il reprend la notification de l'alarme** (décisions de Mehdi du
 * 2026-10-22) : `startForeground` avec le même identifiant et le même contenu que la sonnerie
 * ([NiumiNotificationIds.RINGING]) ou que la demande de scan ([NiumiNotificationIds.SCAN_REQUEST]).
 * Android garde une notification tant qu'un service de premier plan du paquet l'utilise : une
 * seule reste affichée (mesuré le 2026-10-22 sur Xiaomi 25080RABDG / Android 16 et sur émulateur).
 * En `RELEASING`, moins d'une seconde après le scan, il garde la notification qu'il tient.
 *
 * **Une notification écartée n'est ramenée qu'en attente de scan** ([BlockingGuardNotificationWatch]).
 * Avant l'alarme, elle ne fait qu'informer : depuis Android 14, l'utilisateur peut l'écarter sans
 * arrêter le service, et c'est le service qui protège, pas la notification (mesuré le 2026-10-19).
 * Elle ne réapparaît alors qu'avec une nouvelle commande — passage au coucher ou à l'alarme, heure
 * changée —, que `startForeground` poste à nouveau. Pendant la sonnerie, c'est
 * `AlarmRingingService` qui ramène la notification partagée.
 */
@AndroidEntryPoint
class BlockingGuardService : Service() {
    @Inject
    lateinit var notificationFactory: BlockingGuardNotificationFactory

    @Inject
    lateinit var notificationChannelRegistrar: AndroidNotificationChannelRegistrar

    @Inject
    lateinit var pendingIntentFactory: AndroidPendingIntentFactory

    @Inject
    lateinit var gateway: SessionPersistenceGateway

    @Inject
    lateinit var ringingNotificationFactory: RingingNotificationFactory

    @Inject
    lateinit var scanRequestNotificationFactory: ScanRequestNotificationFactory

    @DefaultDispatcher
    @Inject
    lateinit var defaultDispatcher: CoroutineDispatcher

    // `by lazy` : l'injection Hilt d'un Service se termine dans onCreate() (même motif
    // qu'`AlarmRingingService`).
    private val serviceJob = SupervisorJob()
    private val serviceScope by lazy { CoroutineScope(defaultDispatcher + serviceJob) }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val notifications by lazy {
        BlockingGuardNotifications(
            context = this,
            guardFactory = notificationFactory,
            ringingFactory = ringingNotificationFactory,
            scanRequestFactory = scanRequestNotificationFactory,
            pendingIntentFactory = pendingIntentFactory,
        )
    }

    // `onStartCommand` tourne sur le fil principal, `recover` dans [serviceScope] : les deux
    // passent par [present], sous [lock].
    private val lock = Any()
    private var foregroundId: Int? = null
    private var watchJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // Poster sur un canal inconnu ferait rejeter la notification et tomber le service.
        notificationChannelRegistrar.registerAll()
        val command = intent?.let(::commandOf)
        present(command)
        if (command == null) serviceScope.launch { recover(startId) }
        return START_STICKY
    }

    private suspend fun recover(startId: Int) {
        when (val decision = BlockingGuardPolicy.afterRestart(gateway.load())) {
            is BlockingGuardDecision.Keep -> {
                present(decision.command)
            }

            // `stopSelf(startId)` : une commande plus récente de l'observateur garde le service.
            BlockingGuardDecision.Release -> {
                stopSelf(startId)
            }

            BlockingGuardDecision.Unknown -> {
                Unit
            }
        }
    }

    /**
     * Passe au premier plan avec la notification de la phase. En `RELEASING`, un service déjà au
     * premier plan ne reposte rien : repasser à sa propre notification ferait apparaître un texte
     * une fraction de seconde après le scan.
     */
    private fun present(command: BlockingGuardCommand?) {
        synchronized(lock) {
            if (command?.phase == BlockingGuardPhase.RELEASING && foregroundId != null) return
            val id = notifications.idFor(command)
            goForeground(id, notifications.foregroundNotificationFor(command))
            foregroundId = id
            watch(command)
        }
    }

    /** Appelé sous [lock]. Ne surveille qu'en attente de scan ([BlockingGuardNotificationWatch]). */
    private fun watch(command: BlockingGuardCommand?) {
        watchJob?.cancel()
        watchJob = null
        if (command == null || !BlockingGuardNotificationWatch.isWatched(command.phase)) return
        watchJob =
            serviceScope.launch {
                while (isActive) {
                    delay(BlockingGuardNotificationWatch.CHECK_INTERVAL_MS)
                    republishIfDismissed(command)
                }
            }
    }

    private fun republishIfDismissed(command: BlockingGuardCommand) {
        val id = notifications.idFor(command)
        val isPosted = notificationManager.activeNotifications.any { it.id == id }
        if (BlockingGuardNotificationWatch.decide(command.phase, isPosted) == RingingNotificationAction.REPUBLISH) {
            notificationManager.notify(id, notifications.foregroundNotificationFor(command))
        }
    }

    private fun goForeground(
        id: Int,
        notification: Notification,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            // Le type `specialUse` n'existe qu'à partir d'Android 14 ; avant, le type du manifeste fait foi.
            startForeground(id, notification)
        }
    }

    private fun commandOf(intent: Intent): BlockingGuardCommand? =
        BlockingGuardCommand.fromExtras(
            sessionId = intent.getStringExtra(EXTRA_SESSION_ID),
            phase = intent.getStringExtra(EXTRA_PHASE),
            wakeAtEpochMillis = intent.longExtraOrNull(EXTRA_WAKE_AT),
            blockingStartsAtEpochMillis = intent.longExtraOrNull(EXTRA_BLOCKING_AT),
            zoneId = intent.getStringExtra(EXTRA_ZONE_ID),
        )

    private fun Intent.longExtraOrNull(key: String): Long? = if (hasExtra(key)) getLongExtra(key, 0L) else null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Notification propre de la garde, avant l'alarme ([NiumiNotificationIds]). */
        const val NOTIFICATION_ID = NiumiNotificationIds.BLOCKING_GUARD
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_PHASE = "phase"
        const val EXTRA_WAKE_AT = "wakeAtEpochMillis"
        const val EXTRA_BLOCKING_AT = "blockingStartsAtEpochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
    }
}
