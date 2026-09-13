package com.niumi.feature.ringing

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.alarm.AlarmPendingIntentSpecs
import com.niumi.system.audio.AlarmAudioEngine
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.common.OperationResult
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.notification.AndroidNotificationChannelRegistrar
import com.niumi.system.notification.RingingNotificationAction
import com.niumi.system.notification.RingingNotificationFactory
import com.niumi.system.notification.RingingNotificationWatch
import com.niumi.system.power.WakeLockHolder
import com.niumi.system.ringing.RingingRecovery
import com.niumi.system.ringing.RingingServiceRecovery
import com.niumi.system.ringing.ServiceCommand
import com.niumi.system.ringing.ServiceCommandExtras
import com.niumi.system.session.LoadResult
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionPersistenceGateway
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service de sonnerie en premier plan (SPEC_ANDROID §10.2). Exécute l'effet `START_RINGING` de
 * façon idempotente et **n'écrit jamais `SessionState`** : depuis l'étape 17, `RINGING` ne vient
 * que du moteur, via `AlarmTriggerHandler` puis `StartRingingExecutor`.
 *
 * Aucune action `STOP` dans l'intent, la notification ou le binding (§10.2).
 *
 * Reconstruction (`intent == null`, après une mort de processus) : le service passe d'abord au
 * premier plan avec une notification silencieuse — `startForeground()` doit être appelé sans
 * attendre, alors que la lecture du snapshot est suspendue — puis applique la décision de
 * [RingingServiceRecovery].
 */
@AndroidEntryPoint
class AlarmRingingService : Service() {
    @Inject
    lateinit var audioEngine: AlarmAudioEngine

    @Inject
    lateinit var wakeLockHolder: WakeLockHolder

    @Inject
    lateinit var notificationFactory: RingingNotificationFactory

    @Inject
    lateinit var notificationChannelRegistrar: AndroidNotificationChannelRegistrar

    @Inject
    lateinit var pendingIntentFactory: AndroidPendingIntentFactory

    @Inject
    lateinit var technicalEventLog: TechnicalEventLog

    @Inject
    lateinit var gateway: SessionPersistenceGateway

    @Inject
    lateinit var coordinator: SessionCoordinator

    @DefaultDispatcher
    @Inject
    lateinit var defaultDispatcher: CoroutineDispatcher

    private val serviceJob = SupervisorJob()

    // `by lazy` : l'injection Hilt d'un Service se termine dans onCreate(), donc les champs
    // @Inject ne sont pas encore renseignés au moment où les propriétés de la classe
    // s'initialisent. Premier accès réel dans onStartCommand(), toujours après onCreate().
    private val serviceScope by lazy { CoroutineScope(defaultDispatcher + serviceJob) }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }
    private val keyguardManager by lazy { getSystemService(KeyguardManager::class.java) }
    private var wakeLockRenewalJob: Job? = null
    private var notificationWatchJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // Idempotent, et indispensable ici : le service peut démarrer sur une alarme exacte
        // dans un processus recréé, avant tout passage par l'interface. Poster sur un canal
        // inexistant ferait rejeter la notification et tuerait le service au démarrage.
        notificationChannelRegistrar.registerAll()

        if (intent == null) {
            startForeground(NOTIFICATION_ID, silentNotification())
            serviceScope.launch { recover(startId) }
            return START_STICKY
        }

        val extras =
            ServiceCommandExtras(
                sessionId = intent.getStringExtra(AlarmReceiver.EXTRA_SESSION_ID),
                revision =
                    intent
                        .getLongExtra(AlarmReceiver.EXTRA_REVISION, -1L)
                        .takeIf { intent.hasExtra(AlarmReceiver.EXTRA_REVISION) },
            )
        return when (val command = ServiceCommand.from(extras)) {
            is ServiceCommand.Valid -> {
                startRinging(command.sessionId)
                START_STICKY
            }

            is ServiceCommand.Invalid -> {
                startForeground(NOTIFICATION_ID, silentNotification())
                stopSelf(startId)
                START_NOT_STICKY
            }
        }
    }

    /**
     * `stopSelf(startId)` et non `stopSelf()` : `START_STICKY` peut avoir empilé une commande plus
     * récente entre-temps, qu'il ne faut pas tuer.
     */
    private suspend fun recover(startId: Int) {
        val loaded = gateway.load()
        val sessionId = (loaded as? LoadResult.Present)?.snapshot?.sessionId
        technicalEventLog.log(TechnicalEventType.PROCESS_RECREATED, sessionId = sessionId)

        when (val recovery = RingingServiceRecovery.decide(loaded)) {
            is RingingRecovery.ResumeRinging -> {
                startRinging(recovery.sessionId)
            }

            RingingRecovery.ReconcileAndStop, RingingRecovery.StopOnCorruptedSnapshot -> {
                coordinator.reconcile(ReconcileReason.SERVICE_RECREATED)
                stopSelf(startId)
            }

            RingingRecovery.StopWithoutTouchingState -> {
                stopSelf(startId)
            }
        }
    }

    private fun startRinging(sessionId: String) {
        startForeground(
            NOTIFICATION_ID,
            alarmNotification(sessionId, fullScreen = true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        wakeLockHolder.acquire()
        scheduleWakeLockRenewal()
        watchNotification(sessionId)
        // Idempotent : une reprise après mort de processus ne double jamais le son.
        val result = audioEngine.start(ringtoneKey = RINGTONE_KEY, vibrationEnabled = true)
        technicalEventLog.log(TechnicalEventType.RINGING_STARTED, sessionId = sessionId)
        if (result is OperationResult.Failure) {
            technicalEventLog.log(TechnicalEventType.AUDIO_START_FAILED, sessionId = sessionId)
        }
    }

    /** §10.2 : garantir que l'écran de réveil reste atteignable tant que la session sonne. */
    private fun watchNotification(sessionId: String) {
        notificationWatchJob?.cancel()
        notificationWatchJob =
            serviceScope.launch {
                while (isActive) {
                    delay(RingingNotificationWatch.CHECK_INTERVAL_MS)
                    republishNotificationIfNeeded(sessionId)
                }
            }
    }

    private fun republishNotificationIfNeeded(sessionId: String) {
        val isPosted = notificationManager.activeNotifications.any { it.id == NOTIFICATION_ID }
        when (RingingNotificationWatch.decide(isPosted, deviceInUse(powerManager, keyguardManager))) {
            RingingNotificationAction.NONE -> Unit
            RingingNotificationAction.REPUBLISH_WITH_FULL_SCREEN -> republish(sessionId, fullScreen = true)
            RingingNotificationAction.REPUBLISH_SILENTLY -> republish(sessionId, fullScreen = false)
        }
    }

    private fun republish(
        sessionId: String,
        fullScreen: Boolean,
    ) {
        notificationManager.notify(NOTIFICATION_ID, alarmNotification(sessionId, fullScreen))
    }

    private fun alarmNotification(
        sessionId: String,
        fullScreen: Boolean,
    ) = notificationFactory.create(
        pendingIntentFactory.create(AlarmPendingIntentSpecs.fullScreen(sessionId)),
        fullScreen = fullScreen,
    )

    private fun scheduleWakeLockRenewal() {
        wakeLockRenewalJob?.cancel()
        wakeLockRenewalJob =
            serviceScope.launch {
                while (isActive) {
                    delay(WAKE_LOCK_RENEWAL_INTERVAL_MS)
                    wakeLockHolder.renew()
                }
            }
    }

    private fun silentNotification() = notificationFactory.create(alarmScreenPendingIntent = null)

    override fun onDestroy() {
        wakeLockRenewalJob?.cancel()
        notificationWatchJob?.cancel()
        serviceJob.cancel()
        audioEngine.stop()
        wakeLockHolder.release()
        super.onDestroy()
    }

    private companion object {
        const val NOTIFICATION_ID = 1
        const val RINGTONE_KEY = "niumi_alarm"
        const val WAKE_LOCK_RENEWAL_INTERVAL_MS = 8 * 60 * 1000L
    }
}

/**
 * Écran allumé **et** appareil déverrouillé : l'utilisateur se sert de son téléphone (§10.2).
 *
 * Fonction de fichier plutôt que membre de la classe : [AlarmRingingService] est au plafond
 * `TooManyFunctions` de detekt (11). Même motif que `UnlockAwarePersistenceGateway`.
 */
private fun deviceInUse(
    powerManager: PowerManager?,
    keyguardManager: KeyguardManager?,
): Boolean = powerManager?.isInteractive == true && keyguardManager?.isDeviceLocked != true
