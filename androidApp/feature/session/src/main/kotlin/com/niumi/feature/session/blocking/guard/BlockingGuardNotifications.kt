package com.niumi.feature.session.blocking.guard

import android.app.Notification
import android.content.Context
import android.text.format.DateFormat
import com.niumi.feature.session.ui.WakeScheduleFormatter
import com.niumi.system.alarm.AlarmPendingIntentSpecs
import com.niumi.system.blocking.guard.BlockingGuardCommand
import com.niumi.system.blocking.guard.BlockingGuardPhase
import com.niumi.system.intent.AndroidPendingIntentFactory
import com.niumi.system.notification.BlockingGuardNotificationFactory
import com.niumi.system.notification.BlockingGuardNotificationSpecs
import com.niumi.system.notification.NiumiNotificationIds
import com.niumi.system.notification.NotificationSpec
import com.niumi.system.notification.RingingNotificationFactory
import com.niumi.system.notification.ScanRequestNotificationFactory
import com.niumi.system.notification.ScanRequestPendingIntentSpecs

/**
 * La notification que [BlockingGuardService] tient au premier plan, phase par phase
 * (SPEC_ANDROID §12.5, décisions de Mehdi du 2026-10-22) :
 *
 * - avant l'alarme, la sienne, sous [NiumiNotificationIds.BLOCKING_GUARD] ;
 * - pendant la sonnerie, celle de la sonnerie, sous [NiumiNotificationIds.RINGING] ;
 * - en attente de scan, celle de la demande de scan, sous [NiumiNotificationIds.SCAN_REQUEST].
 *
 * Partager une notification exige le même identifiant **et le même contenu** : le dernier qui
 * poste remplace l'autre (mesuré le 2026-10-22). D'où les mêmes fabriques et les mêmes intents
 * qu'`AlarmRingingService.alarmNotification` et `AndroidScanRequestNotifier.present`.
 */
internal class BlockingGuardNotifications(
    private val context: Context,
    private val guardFactory: BlockingGuardNotificationFactory,
    private val ringingFactory: RingingNotificationFactory,
    private val scanRequestFactory: ScanRequestNotificationFactory,
    private val pendingIntentFactory: AndroidPendingIntentFactory,
) {
    fun idFor(command: BlockingGuardCommand?): Int =
        when (command?.phase) {
            BlockingGuardPhase.RINGING -> NiumiNotificationIds.RINGING
            BlockingGuardPhase.AWAITING_SCAN -> NiumiNotificationIds.SCAN_REQUEST
            else -> NiumiNotificationIds.BLOCKING_GUARD
        }

    fun foregroundNotificationFor(command: BlockingGuardCommand?): Notification =
        when (command?.phase) {
            BlockingGuardPhase.RINGING -> {
                val alarmScreen = pendingIntentFactory.create(AlarmPendingIntentSpecs.alarmScreen(command.sessionId))
                ringingFactory.create(alarmScreen)
            }

            BlockingGuardPhase.AWAITING_SCAN -> {
                val tap = pendingIntentFactory.create(ScanRequestPendingIntentSpecs.tap(command.sessionId))
                scanRequestFactory.create(tap)
            }

            else -> {
                guardNotificationFor(command)
            }
        }

    private fun guardNotificationFor(command: BlockingGuardCommand?): Notification =
        guardFactory.create(
            spec = specFor(command),
            openAppPendingIntent =
                command?.let {
                    pendingIntentFactory.create(AlarmPendingIntentSpecs.show(it.sessionId))
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

            // Seul `RELEASING` sans notification déjà posée arrive ici (relance du processus juste
            // après le scan) : un texte vrai quelle que soit la phase. Les deux autres phases
            // reprennent la notification de l'alarme ([foregroundNotificationFor]).
            BlockingGuardPhase.RINGING,
            BlockingGuardPhase.AWAITING_SCAN,
            BlockingGuardPhase.RELEASING,
            -> {
                BlockingGuardNotificationSpecs.unknownPhase()
            }
        }
    }

    private fun timeLabel(
        epochMillis: Long,
        zoneId: String,
    ): String? = WakeScheduleFormatter.formatTime(epochMillis, zoneId, DateFormat.is24HourFormat(context))
}
