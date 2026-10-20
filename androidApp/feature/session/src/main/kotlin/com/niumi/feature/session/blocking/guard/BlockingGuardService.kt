package com.niumi.feature.session.blocking.guard

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.text.format.DateFormat
import com.niumi.feature.session.ui.WakeScheduleFormatter
import com.niumi.system.alarm.AlarmPendingIntentSpecs
import com.niumi.system.blocking.guard.BlockingGuardCommand
import com.niumi.system.blocking.guard.BlockingGuardDecision
import com.niumi.system.blocking.guard.BlockingGuardPhase
import com.niumi.system.blocking.guard.BlockingGuardPolicy
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.notification.AndroidNotificationChannelRegistrar
import com.niumi.system.notification.BlockingGuardNotificationFactory
import com.niumi.system.notification.BlockingGuardNotificationSpecs
import com.niumi.system.notification.NotificationSpec
import com.niumi.system.session.SessionPersistenceGateway
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
 * **Une notification écartée n'est jamais ramenée** (décision de Mehdi du 2026-10-19) : depuis
 * Android 14, l'utilisateur peut l'écarter sans arrêter le service, et c'est le service qui
 * protège, pas la notification (mesuré le même jour). Elle ne réapparaît qu'avec une nouvelle
 * commande — passage au coucher, heure changée —, que `startForeground` poste à nouveau.
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

    @DefaultDispatcher
    @Inject
    lateinit var defaultDispatcher: CoroutineDispatcher

    // `by lazy` : l'injection Hilt d'un Service se termine dans onCreate() (même motif
    // qu'`AlarmRingingService`).
    private val serviceJob = SupervisorJob()
    private val serviceScope by lazy { CoroutineScope(defaultDispatcher + serviceJob) }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // Poster sur un canal inconnu ferait rejeter la notification et tomber le service.
        notificationChannelRegistrar.registerAll()
        val command = intent?.let(::commandOf)
        goForeground(notificationFor(command))
        if (command == null) serviceScope.launch { recover(startId) }
        return START_STICKY
    }

    private suspend fun recover(startId: Int) {
        when (val decision = BlockingGuardPolicy.afterRestart(gateway.load())) {
            is BlockingGuardDecision.Keep -> {
                notificationManager.notify(NOTIFICATION_ID, notificationFor(decision.command))
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

    private fun goForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            // Le type `specialUse` n'existe qu'à partir d'Android 14 ; avant, le type du manifeste fait foi.
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notificationFor(command: BlockingGuardCommand?): Notification =
        notificationFactory.create(
            spec = specFor(command),
            openAppPendingIntent =
                command?.let {
                    pendingIntentFactory.create(
                        AlarmPendingIntentSpecs.show(it.sessionId),
                    )
                },
        )

    private fun specFor(command: BlockingGuardCommand?): NotificationSpec {
        if (command == null) return BlockingGuardNotificationSpecs.unknownPhase()
        val wakeTime = timeLabel(command.wakeAtEpochMillis, command.zoneId)
        return when (command.phase) {
            BlockingGuardPhase.BEFORE_BLOCKING -> {
                BlockingGuardNotificationSpecs.beforeBlocking(
                    wakeTimeLabel = wakeTime,
                    blockingTimeLabel = command.blockingStartsAtEpochMillis?.let { timeLabel(it, command.zoneId) },
                )
            }

            BlockingGuardPhase.BLOCKING -> {
                BlockingGuardNotificationSpecs.blocking(wakeTime)
            }
        }
    }

    private fun timeLabel(
        epochMillis: Long,
        zoneId: String,
    ): String? = WakeScheduleFormatter.formatTime(epochMillis, zoneId, DateFormat.is24HourFormat(this))

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
        /**
         * Premier identifiant libre : 1 sonnerie, 2 attente de scan, 3 et 5 à 9 avertissements,
         * 4 retiré et jamais réattribué.
         */
        const val NOTIFICATION_ID = 10
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_PHASE = "phase"
        const val EXTRA_WAKE_AT = "wakeAtEpochMillis"
        const val EXTRA_BLOCKING_AT = "blockingStartsAtEpochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
    }
}
