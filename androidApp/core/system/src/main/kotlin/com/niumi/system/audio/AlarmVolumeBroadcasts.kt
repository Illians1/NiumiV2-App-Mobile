package com.niumi.system.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Filtre pur de [alarmVolumeChanges], testable en JVM. Un flux absent de l'annonce (`-1`) fait
 * relire par prudence : relire coûte une lecture d'`AudioManager`, manquer un changement laisse
 * un pourcentage faux à l'écran.
 */
object AlarmVolumeBroadcasts {
    fun concernsAlarm(streamType: Int): Boolean =
        streamType == AudioManager.STREAM_ALARM || streamType == UNKNOWN_STREAM
}

/**
 * Émet chaque fois que le volume d'alarme a pu changer, tant que le flux est collecté (feuille
 * Sonnerie, pourcentage en direct, révision du 2026-10-24 ; SPEC_ANDROID §15).
 *
 * Le retour au premier plan ne suffit pas : le panneau de volume de HyperOS ne prend pas le focus
 * de la fenêtre (mesuré le 2026-09-29, SPEC_ANDROID §13.1), aucun `ON_RESUME` ne suit un réglage.
 * Android n'offre pas d'écouteur public du volume ; `AudioService` diffuse
 * `android.media.VOLUME_CHANGED_ACTION` à chaque changement, quel que soit le panneau. La
 * constante est `@hide` : la chaîne est recopiée, sans réflexion, donc hors des restrictions
 * d'API cachées. Un appareil qui ne la diffuserait pas retombe sur la relecture à `ON_RESUME`.
 *
 * `RECEIVER_NOT_EXPORTED` : l'annonce vient de `system_server`, comme `USER_UNLOCKED`
 * (`SystemEventsRegistrar`), et un receveur non exporté accepte `system`.
 */
fun Context.alarmVolumeChanges(): Flow<Unit> =
    callbackFlow {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    val streamType = intent?.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, UNKNOWN_STREAM) ?: return
                    if (AlarmVolumeBroadcasts.concernsAlarm(streamType)) trySend(Unit)
                }
            }
        registerReceiver(receiver, IntentFilter(VOLUME_CHANGED_ACTION), Context.RECEIVER_NOT_EXPORTED)
        awaitClose { unregisterReceiver(receiver) }
    }

private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
private const val UNKNOWN_STREAM = -1
