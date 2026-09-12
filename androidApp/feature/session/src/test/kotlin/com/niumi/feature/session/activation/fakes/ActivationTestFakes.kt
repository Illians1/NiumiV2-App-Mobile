package com.niumi.feature.session.activation.fakes

import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.readiness.ReadinessInput
import com.niumi.system.readiness.ReadinessReport
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.ReconcileResult
import com.niumi.system.session.SessionCoordinator

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
    var reportForCandidate: (candidateTriggerAtEpochMillis: Long) -> ReadinessReport,
) : DeviceReadinessChecker {
    override suspend fun check(input: ReadinessInput): ReadinessReport {
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
    var credential: PairedBoxCredentialDto?,
) : PairedBoxStore {
    var currentCallCount = 0
        private set

    override suspend fun current(): PairedBoxCredentialDto? {
        journal.record("pairedBox.current")
        currentCallCount++
        return credential
    }

    override suspend fun replace(credential: PairedBoxCredentialDto) {
        this.credential = credential
    }

    override suspend fun clear() {
        credential = null
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
}
