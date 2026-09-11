package com.niumi.feature.setup.pairing.fakes

import android.app.Activity
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
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
    var credential: PairedBoxCredentialDto? = null,
) : PairedBoxStore {
    var replaceCallCount = 0
    var clearCallCount = 0

    override suspend fun current(): PairedBoxCredentialDto? = credential

    override suspend fun replace(credential: PairedBoxCredentialDto) {
        replaceCallCount++
        this.credential = credential
    }

    override suspend fun clear() {
        clearCallCount++
        credential = null
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
