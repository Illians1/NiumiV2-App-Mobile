package com.niumi.system.readiness

import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.apps.AppSelectionSource
import com.niumi.system.audio.AlarmVolumeSource
import com.niumi.system.blocking.AccessibilityServiceStatus
import com.niumi.system.nfc.NfcReader
import com.niumi.system.notification.InterruptionFilterSource
import com.niumi.system.notification.NotificationAvailability
import com.niumi.system.notification.NotificationChannelStatus
import com.niumi.system.power.BatteryOptimizationStatus
import com.niumi.system.setup.SetupPreferences

/**
 * Les onze sources du tableau de SPEC_ANDROID §13, regroupées comme [ReconcilerSources][
 * com.niumi.system.session.ReconcilerSources] l'a été à l'étape 11 : un porteur unique plutôt
 * qu'une liste de paramètres ingérable. Toutes préexistent à l'étape 12 sauf
 * [NotificationChannelStatus], [InterruptionFilterSource], [BatteryOptimizationStatus],
 * [AppSelectionSource] et [SetupPreferences] — §7.1 exige de réutiliser les sondes existantes
 * plutôt que d'en redéfinir.
 *
 * [pairedBoxStore] est obligatoire depuis l'étape 13 : `RoomPairedBoxStore` (`:core:database`)
 * est désormais lié en production, la variante optionnelle (`DebugPairedBoxStore`) ne servant
 * plus que le POC de debug.
 */
data class ReadinessSources(
    val nfcReader: NfcReader,
    val pairedBoxStore: PairedBoxStore,
    val appSelectionSource: AppSelectionSource,
    val alarmScheduler: AlarmScheduler,
    val notificationAvailability: NotificationAvailability,
    val notificationChannelStatus: NotificationChannelStatus,
    val alarmVolumeSource: AlarmVolumeSource,
    val interruptionFilterSource: InterruptionFilterSource,
    val accessibilityServiceStatus: AccessibilityServiceStatus,
    val batteryOptimizationStatus: BatteryOptimizationStatus,
    val setupPreferences: SetupPreferences,
)
