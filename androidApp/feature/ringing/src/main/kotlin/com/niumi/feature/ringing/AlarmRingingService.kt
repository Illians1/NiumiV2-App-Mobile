package com.niumi.feature.ringing

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.alarm.AlarmPendingIntentSpecs
import com.niumi.system.audio.AlarmAudioEngine
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.common.OperationResult
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.notification.AndroidNotificationChannelRegistrar
import com.niumi.system.notification.RingingNotificationFactory
import com.niumi.system.power.WakeLockHolder
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
 * façon idempotente, sans écrire directement `RINGING` — ni ce service ni `AlarmReceiver` ne
 * modifient `SessionState` : c'est un écart assumé de cette étape (le moteur KMP n'existe pas
 * encore, voir `ETAPE-03.md`), pas une exception permanente à la règle.
 *
 * Aucune action `STOP` dans l'intent, la notification ou le binding (SPEC_ANDROID §10.2).
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

    @DefaultDispatcher
    @Inject
    lateinit var defaultDispatcher: CoroutineDispatcher

    private val serviceJob = SupervisorJob()

    // `by lazy` : l'injection Hilt d'un Service se termine dans onCreate(), donc les champs
    // @Inject ne sont pas encore renseignés au moment où les propriétés de la classe
    // s'initialisent. Premier accès réel dans scheduleWakeLockRenewal(), appelé depuis
    // onStartCommand(), toujours après onCreate().
    private val serviceScope by lazy { CoroutineScope(defaultDispatcher + serviceJob) }
    private var wakeLockRenewalJob: Job? = null

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
            // Reconstruction depuis le snapshot : traitée à l'étape 17 avec le coordinateur.
            // En attendant, rester silencieux au premier plan plutôt que de sonner sans
            // connaître la session concernée.
            technicalEventLog.log(TechnicalEventType.PROCESS_RECREATED)
            startForeground(NOTIFICATION_ID, silentNotification())
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

    private fun startRinging(sessionId: String) {
        val fullScreenPendingIntent =
            pendingIntentFactory.create(AlarmPendingIntentSpecs.fullScreen(sessionId))
        startForeground(
            NOTIFICATION_ID,
            notificationFactory.create(fullScreenPendingIntent),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        wakeLockHolder.acquire()
        scheduleWakeLockRenewal()
        val result = audioEngine.start(ringtoneKey = RINGTONE_KEY, vibrationEnabled = true)
        technicalEventLog.log(TechnicalEventType.RINGING_STARTED, sessionId = sessionId)
        if (result is OperationResult.Failure) {
            technicalEventLog.log(TechnicalEventType.AUDIO_START_FAILED, sessionId = sessionId)
        }
    }

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

    private fun silentNotification() = notificationFactory.create(fullScreenPendingIntent = null)

    override fun onDestroy() {
        wakeLockRenewalJob?.cancel()
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
