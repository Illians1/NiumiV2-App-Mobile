package com.niumi.system.readiness.fakes

import android.app.Activity
import android.app.NotificationManager
import com.niumi.core.diagnostics.PairedBoxLimit
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.pairing.PairedBoxAddResult
import com.niumi.database.pairing.PairedBoxDto
import com.niumi.database.pairing.PairedBoxRemoveResult
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionSource
import com.niumi.system.audio.AlarmVolumeSource
import com.niumi.system.boot.fakes.FakeUnlockState
import com.niumi.system.common.OperationResult
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader
import com.niumi.system.notification.InterruptionFilterSource
import com.niumi.system.notification.NiumiNotificationChannels
import com.niumi.system.notification.NotificationAvailability
import com.niumi.system.notification.NotificationChannelStatus
import com.niumi.system.power.BatteryOptimizationStatus
import com.niumi.system.readiness.ReadinessSources
import com.niumi.system.readiness.UnlockSettling
import com.niumi.system.recents.RecentsLockState
import com.niumi.system.recents.RecentsLockStatus
import com.niumi.system.session.fakes.FakeAccessibilityServiceStatus
import com.niumi.system.session.fakes.FakeAlarmScheduler
import com.niumi.system.session.fakes.FakeUptimeClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

class FakeNfcReader(
    var availabilityValue: NfcAvailability = NfcAvailability.ENABLED,
) : NfcReader {
    override fun start(
        activity: Activity,
        onUri: (String) -> Unit,
        onUnreadable: () -> Unit,
        onForeignTag: () -> Unit,
    ): OperationResult = OperationResult.Success

    override fun stop(activity: Activity) = Unit

    override val availability: NfcAvailability
        get() = availabilityValue
}

/**
 * Un Niumi Spot associé par défaut : c'est l'état « tout vert » du contrôle `PAIRED_BOX` (Lot 8).
 * Mêmes refus que `RoomPairedBoxStore` : doublon, onzième Niumi Spot, dernier Niumi Spot retiré (étape 43).
 */
class FakePairedBoxStore(
    seed: List<PairedBoxDto> =
        listOf(PairedBoxDto(PairedBoxCredentialDto(1, "box-1", "a".repeat(64)), "Niumi Spot 1")),
) : PairedBoxStore {
    private val boxes = MutableStateFlow(seed)

    override val points: Flow<List<PairedBoxDto>> = boxes

    override suspend fun all(): List<PairedBoxDto> = boxes.value

    override suspend fun add(
        credential: PairedBoxCredentialDto,
        name: String,
    ): PairedBoxAddResult =
        when {
            boxes.value.any { it.credential.boxId == credential.boxId } -> {
                PairedBoxAddResult.AlreadyPaired
            }

            boxes.value.size >= PairedBoxLimit.MAX -> {
                PairedBoxAddResult.LimitReached
            }

            else -> {
                boxes.value += PairedBoxDto(credential, name)
                PairedBoxAddResult.Added
            }
        }

    override suspend fun rename(
        boxId: String,
        name: String,
    ) {
        boxes.value = boxes.value.map { if (it.credential.boxId == boxId) it.copy(name = name) else it }
    }

    override suspend fun remove(boxId: String): PairedBoxRemoveResult =
        when {
            boxes.value.none { it.credential.boxId == boxId } -> {
                PairedBoxRemoveResult.Unknown
            }

            boxes.value.size <= PairedBoxLimit.MIN -> {
                PairedBoxRemoveResult.LastBoxKept
            }

            else -> {
                boxes.value = boxes.value.filterNot { it.credential.boxId == boxId }
                PairedBoxRemoveResult.Removed
            }
        }

    /** Convenience de test, hors interface : « aucun Niumi Spot associé ». */
    fun clear() {
        boxes.value = emptyList()
    }

    /** Convenience de test, hors interface : force un nombre de Niumi Spots distincts. */
    fun seedCount(count: Int) {
        boxes.value =
            List(count) { index ->
                PairedBoxDto(PairedBoxCredentialDto(1, "box-$index", "a".repeat(64)), "Niumi Spot ${index + 1}")
            }
    }
}

class FakeAppSelectionSource(
    var count: Int = 3,
) : AppSelectionSource {
    override suspend fun selectedCount(): Int = count
}

class FakeNotificationAvailability(
    var notificationsEnabled: Boolean = true,
) : NotificationAvailability {
    override fun areNotificationsEnabled(): Boolean = notificationsEnabled
}

class FakeNotificationChannelStatus(
    var enabledChannels: MutableSet<String> =
        mutableSetOf(
            NiumiNotificationChannels.alarmRinging.id,
            NiumiNotificationChannels.sessionAwaitingScan.id,
        ),
) : NotificationChannelStatus {
    override fun isChannelEnabled(channelId: String): Boolean = channelId in enabledChannels
}

class FakeAlarmVolumeSource(
    var volume: Int = 7,
    var percent: Int? = 47,
) : AlarmVolumeSource {
    override fun alarmStreamVolume(): Int = volume

    override fun alarmStreamPercent(): Int? = percent

    override fun changes(): Flow<Unit> = emptyFlow()
}

class FakeInterruptionFilterSource(
    var filter: Int = NotificationManager.INTERRUPTION_FILTER_ALL,
) : InterruptionFilterSource {
    override fun currentInterruptionFilter(): Int = filter
}

/** Verrouillé par défaut : c'est l'état « tout vert » ; `UNSUPPORTED` reproduit un appareil sans ce mécanisme. */
class FakeRecentsLockStatus(
    var state: RecentsLockState = RecentsLockState.LOCKED,
) : RecentsLockStatus {
    override fun read(): RecentsLockState = state
}

class FakeBatteryOptimizationStatus(
    var ignoring: Boolean = true,
) : BatteryOptimizationStatus {
    override fun isIgnoringBatteryOptimizations(): Boolean = ignoring
}

/**
 * Assemble des sources toutes vertes : chaque test ne dégrade que la source qu'il examine,
 * ce qui garde une assertion par ligne du tableau de SPEC_ANDROID §13 lisible.
 */
class ReadinessTestSources(
    // Partagés avec `TestCoordinatorHarness` quand le moniteur doit voir les mêmes fakes que le
    // reste du coordinateur : sans cela, `harness.alarmScheduler.canScheduleExactValue = false`
    // n'atteindrait jamais le diagnostic.
    val alarmScheduler: FakeAlarmScheduler = FakeAlarmScheduler(),
    val accessibilityServiceStatus: FakeAccessibilityServiceStatus = FakeAccessibilityServiceStatus(),
) {
    val nfcReader = FakeNfcReader()
    val pairedBoxStore = FakePairedBoxStore()
    val appSelectionSource = FakeAppSelectionSource()
    val notificationAvailability = FakeNotificationAvailability()
    val notificationChannelStatus = FakeNotificationChannelStatus()
    val alarmVolumeSource = FakeAlarmVolumeSource()
    val interruptionFilterSource = FakeInterruptionFilterSource()
    val batteryOptimizationStatus = FakeBatteryOptimizationStatus()
    val recentsLockStatus = FakeRecentsLockStatus()

    /**
     * Déverrouillé par défaut : c'est l'état de tous les scénarios sauf ceux qui visent
     * explicitement la fenêtre Direct Boot (étape 19).
     */
    val unlockState = FakeUnlockState(isUserUnlocked = true)

    /**
     * Démarrage lointain par défaut (étape 25). La fenêtre de liaison ne compte qu'à partir de la
     * première lecture déverrouillée : un test qui ne met pas le service en `PENDING` ne la voit
     * jamais, et un test qui l'y met avance [uptimeClock] pour en sortir.
     */
    val uptimeClock = FakeUptimeClock()
    val unlockSettling = UnlockSettling(unlockState, uptimeClock)

    init {
        // Déverrouillé depuis longtemps par défaut : la fenêtre est ouverte puis refermée, pour que
        // les contrôles soient jugés normalement. Les tests de la fenêtre la rouvrent par [justUnlocked].
        unlockSettling.isSettling()
        uptimeClock.elapsedMillis += UnlockSettling.GRACE_MILLIS
    }

    /**
     * L'appareil vient d'être déverrouillé : la fenêtre de liaison s'ouvre à l'instant courant de
     * [uptimeClock], comme pour un processus vivant depuis le démarrage verrouillé qui voit passer
     * `USER_UNLOCKED`.
     */
    fun justUnlocked() {
        unlockState.isUserUnlocked = false
        unlockSettling.isSettling()
        unlockState.isUserUnlocked = true
        unlockSettling.isSettling()
    }

    fun build(): ReadinessSources =
        ReadinessSources(
            nfcReader = nfcReader,
            pairedBoxStore = pairedBoxStore,
            appSelectionSource = appSelectionSource,
            alarmScheduler = alarmScheduler,
            notificationAvailability = notificationAvailability,
            notificationChannelStatus = notificationChannelStatus,
            alarmVolumeSource = alarmVolumeSource,
            interruptionFilterSource = interruptionFilterSource,
            accessibilityServiceStatus = accessibilityServiceStatus,
            batteryOptimizationStatus = batteryOptimizationStatus,
            unlockState = unlockState,
            unlockSettling = unlockSettling,
            recentsLockStatus = recentsLockStatus,
        )
}
