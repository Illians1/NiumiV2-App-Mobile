package com.niumi.feature.session.active.fakes

import android.app.Activity
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage
import com.niumi.database.EffectStatus
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.StoredDecision
import com.niumi.database.incident.SessionIncidentsReader
import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.audio.VibrationController
import com.niumi.system.common.OperationResult
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader
import com.niumi.system.nfc.NfcScanHandler
import com.niumi.system.nfc.ScanOutcome
import com.niumi.system.readiness.ForegroundReadinessTrigger
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionPersistenceGateway
import kotlinx.coroutines.CompletableDeferred

/** Fakes locaux des écrans 7, 9 et 11 : `:core:system/src/test` n'est pas exporté (aucun `testFixtures`). */
class RecordingForegroundReadinessTrigger : ForegroundReadinessTrigger {
    var evaluations: Int = 0
        private set

    override fun evaluateAsync() {
        evaluations++
    }
}

class FakeSessionIncidentsReader(
    var incidents: List<SessionIncidentDto> = emptyList(),
) : SessionIncidentsReader {
    override suspend fun incidents(sessionId: String): List<SessionIncidentDto> = incidents
}

/**
 * Ne répond qu'à [load] : l'écran 7 ne lit que les `extras` d'une session déjà décidée, et toute
 * écriture depuis un écran serait une faute que ces `error()` transforment en échec de test.
 */
class FakeSessionPersistenceGateway(
    var result: LoadResult = LoadResult.Absent,
) : SessionPersistenceGateway {
    override suspend fun load(): LoadResult = result

    override suspend fun commit(decision: StoredDecision) = error("COMMIT_FROM_SCREEN")

    override suspend fun receipt(eventId: String): EventReceipt? = error("RECEIPT_FROM_SCREEN")

    override suspend fun pendingEffects(sessionId: String): List<PendingEffect> = error("EFFECTS_FROM_SCREEN")

    override suspend fun markEffect(
        effectId: String,
        status: EffectStatus,
        error: String?,
    ) = error("MARK_EFFECT_FROM_SCREEN")

    override suspend fun clearActive(sessionId: String) = error("CLEAR_ACTIVE_FROM_SCREEN")

    override suspend fun recordIncident(
        sessionId: String,
        incident: SessionIncidentDto,
    ): OperationResult = error("RECORD_INCIDENT_FROM_SCREEN")
}

fun presentSession(
    snapshot: SessionSnapshotDto,
    blockedPackages: List<BlockedPackage>,
): LoadResult.Present =
    LoadResult.Present(
        snapshot = snapshot,
        extras =
            AndroidSessionExtras(
                boxId = "550e8400-e29b-41d4-a716-446655440000",
                boxTokenSha256Hex = "a".repeat(64),
                ringtoneKey = "niumi_alarm",
                vibrationEnabled = true,
                blockedPackages = blockedPackages,
            ),
        pendingEffects = emptyList(),
    )

/**
 * Seule `availability` est consultée par les tests JVM : `start`/`stop` exigent une `Activity`,
 * qui n'est pas instanciable hors instrumentation, et les tests pilotent donc directement les
 * callbacks que le Reader Mode câble. Même convention que le `FakeNfcReader` de `:feature:setup`.
 */
class FakeNfcReader(
    var availabilityValue: NfcAvailability = NfcAvailability.ENABLED,
) : NfcReader {
    override fun start(
        activity: Activity,
        onUri: (String) -> Unit,
        onUnreadable: () -> Unit,
    ): OperationResult = error("READER_MODE_NOT_TESTABLE_IN_JVM")

    override fun stop(activity: Activity) = error("READER_MODE_NOT_TESTABLE_IN_JVM")

    override val availability: NfcAvailability
        get() = availabilityValue
}

class ScriptedNfcScanHandler(
    var outcome: ScanOutcome = ScanOutcome.Ignored,
) : NfcScanHandler {
    var calls: Int = 0
        private set

    /**
     * Bloque la lecture jusqu'à ce que le test la relâche, pour qu'un second scan puisse arriver
     * pendant que le premier court encore — la seule situation où la garde de réentrance joue.
     */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun onUriRead(uri: String): ScanOutcome {
        calls++
        gate?.await()
        return outcome
    }
}

class RecordingVibrationController : VibrationController {
    var errorVibrations: Int = 0
        private set

    override fun startRepeating() = error("ALARM_VIBRATION_FROM_SCAN_SCREEN")

    override fun vibrateError() {
        errorVibrations++
    }

    override fun stop() = error("ALARM_VIBRATION_FROM_SCAN_SCREEN")
}

class RecordingTechnicalEventLog : TechnicalEventLog {
    val logged = mutableListOf<TechnicalEventType>()

    override fun log(
        type: TechnicalEventType,
        sessionId: String?,
        detailsJson: String?,
    ) {
        logged += type
    }

    override suspend fun recent(): List<TechnicalEventEntry> = emptyList()
}
