package com.niumi.system.session

import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.audio.AlarmVolumeSource
import com.niumi.system.blocking.AccessibilityServiceStatus
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader
import com.niumi.system.notification.NotificationAvailability

/** Construit un [SessionRuntimeStatus] pour une session donnée. */
fun interface SessionRuntimeStatusProbe {
    fun probe(sessionId: String): SessionRuntimeStatus
}

class DefaultSessionRuntimeStatusProbe(
    private val alarmScheduler: AlarmScheduler,
    private val accessibilityServiceStatus: AccessibilityServiceStatus,
    private val notificationAvailability: NotificationAvailability,
    private val nfcReader: NfcReader,
    private val alarmVolumeSource: AlarmVolumeSource,
) : SessionRuntimeStatusProbe {
    override fun probe(sessionId: String): SessionRuntimeStatus =
        SessionRuntimeStatus(
            alarmScheduled = alarmScheduler.isScheduled(sessionId),
            accessibilityReady = accessibilityServiceStatus.isEnabled(),
            notificationReady = notificationAvailability.areNotificationsEnabled(),
            fullScreenReady = notificationAvailability.canUseFullScreenIntent(),
            nfcReady = nfcReader.availability == NfcAvailability.ENABLED,
            audioReady = alarmVolumeSource.alarmStreamVolume() > 0,
        )
}
