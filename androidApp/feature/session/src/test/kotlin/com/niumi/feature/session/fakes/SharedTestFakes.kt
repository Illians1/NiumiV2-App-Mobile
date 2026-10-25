package com.niumi.feature.session.fakes

import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.BlockedPackage
import com.niumi.database.pairing.PairedBoxAddResult
import com.niumi.database.pairing.PairedBoxDto
import com.niumi.database.pairing.PairedBoxRemoveResult
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.audio.AlarmSoundPreferences
import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.common.Clock
import com.niumi.system.common.TimeZoneProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Fakes locaux : ceux de `:core:system/src/test` ne sont pas exportés (aucun `testFixtures` dans ce dépôt). */
class FakeClock(
    var now: Long,
) : Clock {
    override fun nowEpochMillis(): Long = now
}

class FakeTimeZoneProvider(
    var zoneId: String,
) : TimeZoneProvider {
    override fun currentZoneId(): String = zoneId
}

/** Journal d'appels partagé entre les fakes, pour prouver l'ordre des points 1 à 6 de §9.2. */
class CallJournal {
    private val entries = mutableListOf<String>()

    fun record(entry: String) {
        entries += entry
    }

    fun snapshot(): List<String> = entries.toList()
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

class FakeAlarmSoundPreferences(
    var settings: AlarmSoundSettings = AlarmSoundSettings(),
) : AlarmSoundPreferences {
    override suspend fun read(): AlarmSoundSettings = settings

    override suspend fun write(settings: AlarmSoundSettings) {
        this.settings = settings
    }
}
