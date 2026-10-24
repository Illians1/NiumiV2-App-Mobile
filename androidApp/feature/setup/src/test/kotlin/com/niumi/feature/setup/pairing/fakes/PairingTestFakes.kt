package com.niumi.feature.setup.pairing.fakes

import android.app.Activity
import com.niumi.core.diagnostics.PairedBoxLimit
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.logging.TechnicalEventEntry
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.database.pairing.PairedBoxAddResult
import com.niumi.database.pairing.PairedBoxDto
import com.niumi.database.pairing.PairedBoxRemoveResult
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.common.OperationResult
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

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

    /** Toute écriture, quelle qu'elle soit : un refus de l'écran se prouve par zéro. */
    var writeCallCount = 0
        private set

    /**
     * Retient `all()` tant qu'elle n'est pas complétée : la lecture Room suspend en production, et
     * c'est pendant cette suspension qu'une seconde lecture du même tag peut arriver.
     */
    var allGate: CompletableDeferred<Unit>? = null

    override suspend fun all(): List<PairedBoxDto> {
        allGate?.await()
        return boxes
    }

    /** Même ordre de refus que `RoomPairedBoxStore` : doublon d'abord, plafond ensuite. */
    override suspend fun add(
        credential: PairedBoxCredentialDto,
        name: String,
    ): PairedBoxAddResult {
        addCallCount++
        writeCallCount++
        return when {
            boxes.any { it.credential.boxId == credential.boxId } -> {
                PairedBoxAddResult.AlreadyPaired
            }

            boxes.size >= PairedBoxLimit.MAX -> {
                PairedBoxAddResult.LimitReached
            }

            else -> {
                boxes = boxes + PairedBoxDto(credential, name)
                PairedBoxAddResult.Added
            }
        }
    }

    override suspend fun rename(
        boxId: String,
        name: String,
    ) {
        writeCallCount++
        boxes = boxes.map { if (it.credential.boxId == boxId) it.copy(name = name) else it }
    }

    override val points: Flow<List<PairedBoxDto>>
        get() = flowOf(boxes)

    /** Même ordre de refus que `RoomPairedBoxStore` : inconnu d'abord, dernier Niumi Spot ensuite (étape 43). */
    override suspend fun remove(boxId: String): PairedBoxRemoveResult {
        writeCallCount++
        return when {
            boxes.none { it.credential.boxId == boxId } -> {
                PairedBoxRemoveResult.Unknown
            }

            boxes.size <= PairedBoxLimit.MIN -> {
                PairedBoxRemoveResult.LastBoxKept
            }

            else -> {
                boxes = boxes.filterNot { it.credential.boxId == boxId }
                PairedBoxRemoveResult.Removed
            }
        }
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
        onForeignTag: () -> Unit,
    ): OperationResult = OperationResult.Success

    override fun stop(activity: Activity) = Unit

    override val availability: NfcAvailability
        get() = availabilityValue
}
