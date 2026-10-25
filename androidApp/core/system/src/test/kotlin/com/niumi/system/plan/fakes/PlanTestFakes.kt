package com.niumi.system.plan.fakes

import com.niumi.database.BlockedPackage
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.audio.AlarmSoundPreferences
import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.common.TimeZoneProvider
import com.niumi.system.plan.WakePlan
import com.niumi.system.plan.WakePlanStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime
import java.time.ZoneId

/** Planning en mémoire, mêmes refus que `DataStoreWakePlanStore` (planning invalide). */
class InMemoryWakePlanStore(
    initial: WakePlan = WakePlan.DEFAULT,
) : WakePlanStore {
    private val state = MutableStateFlow(initial)
    var writeCount = 0
        private set

    /** Simule un `DataStore` qui lève à l'écriture (`IOException`). */
    var writeFails = false

    override val plan: Flow<WakePlan> = state

    val current: WakePlan
        get() = state.value

    override suspend fun read(): WakePlan = state.value

    override suspend fun write(plan: WakePlan) {
        if (writeFails) throw java.io.IOException("DATASTORE_WRITE_FAILED")
        plan.requireValid()
        writeCount++
        state.value = plan
    }
}

class InMemoryAppSelectionStore(
    var stored: List<BlockedPackage> = listOf(BlockedPackage("com.example.chat", "Chat")),
) : AppSelectionStore {
    override suspend fun selectedCount(): Int = stored.size

    override suspend fun selection(): List<BlockedPackage> = stored

    override suspend fun replace(selection: List<BlockedPackage>) {
        stored = selection
    }
}

class InMemoryAlarmSoundPreferences(
    var settings: AlarmSoundSettings = AlarmSoundSettings(),
) : AlarmSoundPreferences {
    override suspend fun read(): AlarmSoundSettings = settings

    override suspend fun write(settings: AlarmSoundSettings) {
        this.settings = settings
    }
}

class MutableTimeZoneProvider(
    var zoneId: String = PARIS,
) : TimeZoneProvider {
    override fun currentZoneId(): String = zoneId
}

const val PARIS = "Europe/Paris"

/** Instant d'une heure locale (`2026-10-23T07:00`), pour écrire les scénarios dans le fuseau de l'utilisateur. */
fun epochMillisAt(
    localDateTimeIso: String,
    zoneId: String = PARIS,
): Long =
    LocalDateTime
        .parse(localDateTimeIso)
        .atZone(ZoneId.of(zoneId))
        .toInstant()
        .toEpochMilli()
