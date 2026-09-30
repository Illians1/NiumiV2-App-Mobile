package com.niumi.system.readiness

import com.niumi.database.directboot.UnlockState
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
import com.niumi.system.recents.RecentsLockStatus

/**
 * Les dix sources du tableau de SPEC_ANDROID §13, regroupées comme [ReconcilerSources][
 * com.niumi.system.session.ReconcilerSources] l'a été à l'étape 11 : un porteur unique plutôt
 * qu'une liste de paramètres ingérable. Toutes préexistent à l'étape 12 sauf
 * [NotificationChannelStatus], [InterruptionFilterSource], [BatteryOptimizationStatus] et
 * [AppSelectionSource] — §7.1 exige de réutiliser les sondes existantes plutôt que d'en redéfinir.
 * `SetupPreferences` en est sorti à l'étape 25 : l'exemption d'énergie est désormais détectée, et
 * non plus confirmée par l'utilisateur.
 *
 * [pairedBoxStore] est obligatoire depuis l'étape 13 : `RoomPairedBoxStore` (`:core:database`)
 * est désormais la seule liaison, en production comme en debug.
 *
 * [unlockState] rejoint le groupe à l'étape 19 : avant le premier déverrouillage, le contrôle du
 * service d'accessibilité ne peut rien dire de vrai et doit être neutralisé — voir
 * [AndroidDeviceReadinessChecker.sessionChecks].
 *
 * [unlockSettling] le rejoint à l'étape 25, pour la même raison juste **après** le déverrouillage,
 * tant qu'Android relie encore le service ([UnlockSettling]).
 *
 * [recentsLockStatus] aussi (étape 25) : sur HyperOS, seul le verrou de Niumi dans les récents
 * protège le blocage de « Tout effacer » ([com.niumi.system.recents.RecentsLockStatus]).
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
    val unlockState: UnlockState,
    val unlockSettling: UnlockSettling,
    val recentsLockStatus: RecentsLockStatus,
)
