package com.niumi.feature.session.activation.fakes

import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage
import com.niumi.database.pairing.PairedBoxAddResult
import com.niumi.database.pairing.PairedBoxDto
import com.niumi.database.pairing.PairedBoxRemoveResult
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.readiness.ReadinessInput
import com.niumi.system.readiness.ReadinessReport
import com.niumi.system.session.AlarmSoundUpdateResult
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.ReconcileResult
import com.niumi.system.session.SessionCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Journal d'appels partagé entre les fakes, pour prouver l'ordre des points 1 à 6 de §9.2. */
class CallJournal {
    private val entries = mutableListOf<String>()

    fun record(entry: String) {
        entries += entry
    }

    fun snapshot(): List<String> = entries.toList()
}

/**
 * Deux rapports distincts : [reportForNull] répond au premier appel de `diagnose` (sans candidat,
 * ne sert qu'à lire `nowEpochMillis`), [reportForCandidate] au second (avec le candidat calculé),
 * seul transmis à `evaluateActivation`. Un diagnostic réel relit systématiquement ses sources ;
 * ce fake matérialise cette absence d'état interne.
 */
class RecordingReadinessChecker(
    private val journal: CallJournal,
    var reportForNull: ReadinessReport,
    var reportForCandidate: suspend (candidateTriggerAtEpochMillis: Long) -> ReadinessReport,
) : DeviceReadinessChecker {
    /** Dernière entrée reçue : le Lot 6 doit prouver que le début de blocage candidat y parvient. */
    var lastInput: ReadinessInput? = null
        private set

    override suspend fun check(input: ReadinessInput): ReadinessReport {
        lastInput = input
        val candidate = input.candidateTriggerAtEpochMillis
        return if (candidate == null) {
            journal.record("readiness.check(null)")
            reportForNull
        } else {
            journal.record("readiness.check(candidate)")
            reportForCandidate(candidate)
        }
    }
}

class RecordingTimeZoneProvider(
    private val journal: CallJournal,
    var zoneId: String,
) : TimeZoneProvider {
    override fun currentZoneId(): String {
        journal.record("timeZone.current")
        return zoneId
    }
}

class RecordingPairedBoxStore(
    private val journal: CallJournal,
    var boxes: List<PairedBoxDto>,
) : PairedBoxStore {
    var allCallCount = 0
        private set

    override suspend fun all(): List<PairedBoxDto> {
        journal.record("pairedBox.all")
        allCallCount++
        return boxes
    }

    override suspend fun add(
        credential: PairedBoxCredentialDto,
        name: String,
    ): PairedBoxAddResult {
        boxes = boxes + PairedBoxDto(credential, name)
        return PairedBoxAddResult.Added
    }

    override suspend fun rename(
        boxId: String,
        name: String,
    ) {
        boxes = boxes.map { if (it.credential.boxId == boxId) it.copy(name = name) else it }
    }

    override val points: Flow<List<PairedBoxDto>>
        get() = flowOf(boxes)

    override suspend fun remove(boxId: String): PairedBoxRemoveResult {
        boxes = boxes.filterNot { it.credential.boxId == boxId }
        return PairedBoxRemoveResult.Removed
    }
}

class RecordingAppSelectionStore(
    private val journal: CallJournal,
    var stored: List<BlockedPackage>,
) : AppSelectionStore {
    override suspend fun selectedCount(): Int = stored.size

    override suspend fun selection(): List<BlockedPackage> {
        journal.record("appSelection.selection")
        return stored
    }

    override suspend fun replace(selection: List<BlockedPackage>) {
        stored = selection
    }
}

class RecordingSessionCoordinator(
    private val journal: CallJournal,
) : SessionCoordinator {
    var result: DispatchResult = DispatchResult.Applied(snapshot = null, requiredEffectsSucceeded = false)
    var dispatchCount = 0
        private set
    var lastEvent: SessionEventDto? = null
        private set
    var lastExtras: AndroidSessionExtras? = null
        private set

    override suspend fun dispatch(
        event: SessionEventDto,
        extras: AndroidSessionExtras?,
    ): DispatchResult {
        journal.record("coordinator.dispatch(${event.kind})")
        dispatchCount++
        lastEvent = event
        lastExtras = extras
        return result
    }

    override suspend fun reconcile(reason: ReconcileReason): ReconcileResult =
        throw UnsupportedOperationException("Non utilisé par ArmSessionUseCase")

    override suspend fun updateAlarmSound(
        ringtoneKey: String,
        volumeRampSeconds: Int?,
    ): AlarmSoundUpdateResult = throw UnsupportedOperationException("Non utilisé par ArmSessionUseCase")
}
