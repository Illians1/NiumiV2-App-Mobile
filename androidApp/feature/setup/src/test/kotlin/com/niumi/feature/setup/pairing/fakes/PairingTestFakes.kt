package com.niumi.feature.setup.pairing.fakes

import android.app.Activity
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.database.pairing.PairedBoxAddResult
import com.niumi.database.pairing.PairedBoxDto
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.common.OperationResult
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader

/**
 * Journal qui conserve le `detailsJson` en plus du type, contrairement au `FakeTechnicalEventLog`
 * de `:core:system` : SPEC_ANDROID §16 et SPEC_CORE_KMP §9.2 interdisent d'y écrire le token ou
 * son empreinte, ce qui ne peut se prouver qu'en inspectant le contenu.
 */
class RecordingTechnicalEventLog : TechnicalEventLog {
    data class Entry(
        val type: TechnicalEventType,
        val sessionId: String?,
        val detailsJson: String?,
    )

    val entries = mutableListOf<Entry>()

    val types: List<TechnicalEventType> get() = entries.map { it.type }

    override fun log(
        type: TechnicalEventType,
        sessionId: String?,
        detailsJson: String?,
    ) {
        entries += Entry(type, sessionId, detailsJson)
    }

    override suspend fun recent(): List<TechnicalEventEntry> = emptyList()
}

class FakePairedBoxStore(
    var boxes: List<PairedBoxDto> = emptyList(),
) : PairedBoxStore {
    var addCallCount = 0
        private set

    override suspend fun all(): List<PairedBoxDto> = boxes

    override suspend fun add(
        credential: PairedBoxCredentialDto,
        name: String,
    ): PairedBoxAddResult {
        addCallCount++
        return if (boxes.any { it.credential.boxId == credential.boxId }) {
            PairedBoxAddResult.AlreadyPaired
        } else {
            boxes = boxes + PairedBoxDto(credential, name)
            PairedBoxAddResult.Added
        }
    }

    override suspend fun rename(
        boxId: String,
        name: String,
    ) {
        boxes = boxes.map { if (it.credential.boxId == boxId) it.copy(name = name) else it }
    }

    override suspend fun remove(boxId: String) {
        boxes = boxes.filterNot { it.credential.boxId == boxId }
    }
}

/**
 * Seule `availability` est consultée par `PairingViewModel` ; `start`/`stop` appartiennent à
 * l'activité hôte et ne sont jamais appelés ici (aucune `Activity` n'existe en JVM).
 */
class FakeNfcReader(
    var availabilityValue: NfcAvailability = NfcAvailability.ENABLED,
) : NfcReader {
    override fun start(
        activity: Activity,
        onUri: (String) -> Unit,
        onUnreadable: () -> Unit,
    ): OperationResult = OperationResult.Success

    override fun stop(activity: Activity) = Unit

    override val availability: NfcAvailability
        get() = availabilityValue
}
