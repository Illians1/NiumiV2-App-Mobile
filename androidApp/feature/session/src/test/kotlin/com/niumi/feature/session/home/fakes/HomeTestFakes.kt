package com.niumi.feature.session.home.fakes

import com.niumi.core.interop.BlockingScheduleDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.BlockedPackage
import com.niumi.feature.session.home.MinuteTicker
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.apps.InstalledApp
import com.niumi.system.apps.InstalledAppsSource
import com.niumi.system.plan.PlanApplyResult
import com.niumi.system.plan.PlanChangeReason
import com.niumi.system.plan.WakePlan
import com.niumi.system.plan.WakePlanScheduler
import com.niumi.system.plan.WakePlanStore
import com.niumi.system.readiness.ForegroundReadinessTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime
import java.time.ZoneId

const val PARIS = "Europe/Paris"

/** Instant d'une heure locale à Paris, « 2026-10-22T21:34 » (un jeudi). */
fun paris(local: String): Long =
    LocalDateTime
        .parse(local)
        .atZone(ZoneId.of(PARIS))
        .toInstant()
        .toEpochMilli()

class FakeWakePlanStore(
    initial: WakePlan = WakePlan.DEFAULT,
) : WakePlanStore {
    private val flow = MutableStateFlow(initial)

    override val plan: Flow<WakePlan> = flow

    val current: WakePlan get() = flow.value

    override suspend fun read(): WakePlan = flow.value

    override suspend fun write(plan: WakePlan) {
        flow.value = plan
    }
}

/**
 * Enregistre chaque `apply` et écrit le planning, comme le vrai scheduler hors session ; en
 * [PlanApplyResult.LockedBySession], ne l'écrit pas.
 */
class RecordingWakePlanScheduler(
    private val store: FakeWakePlanStore,
    var result: PlanApplyResult = PlanApplyResult.Unchanged,
) : WakePlanScheduler {
    /** Levée par `apply` quand renseignée : une écriture qui échoue. */
    var failure: Exception? = null

    val applied = mutableListOf<Pair<WakePlan, PlanChangeReason>>()

    override suspend fun apply(
        plan: WakePlan,
        reason: PlanChangeReason,
    ): PlanApplyResult {
        failure?.let { throw it }
        applied += plan to reason
        if (result != PlanApplyResult.LockedBySession) store.write(plan)
        return result
    }
}

class InMemoryAppSelection(
    var stored: List<BlockedPackage> = emptyList(),
) : AppSelectionStore {
    override suspend fun selectedCount(): Int = stored.size

    override suspend fun selection(): List<BlockedPackage> = stored

    override suspend fun replace(selection: List<BlockedPackage>) {
        stored = selection
    }
}

class FakeInstalledApps(
    var apps: List<InstalledApp> = emptyList(),
) : InstalledAppsSource {
    override suspend fun launchableApps(): List<InstalledApp> = apps
}

/** Surveillance §13.1 : on compte les évaluations demandées. */
class RecordingReadinessTrigger : ForegroundReadinessTrigger {
    var evaluations = 0
        private set

    override fun evaluateAsync() {
        evaluations++
    }
}

/** Les tests émettent eux-mêmes le battement de la minute. */
class ManualMinuteTicker : MinuteTicker {
    val flow = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    override fun ticks(): Flow<Unit> = flow
}

/** Session armée pour vendredi 23 octobre 2026 à 7:00, coucher jeudi à 22:30. */
fun armedSnapshot(
    blockingApplied: Boolean = false,
    state: SessionStateDto = SessionStateDto.ARMED,
    trigger: Long = paris("2026-10-23T07:00"),
    bedtime: Long = paris("2026-10-22T22:30"),
) = SessionSnapshotDto(
    schemaVersion = 3,
    revision = 1,
    sessionId = "11111111-1111-1111-1111-111111111111",
    wakeSchedule = WakeScheduleDto("2026-10-23", "07:00", PARIS, trigger),
    blockingSchedule = BlockingScheduleDto("2026-10-22", "22:30", bedtime),
    state = state,
    releaseTarget = null,
    health = SessionHealthDto.HEALTHY,
    createdAtEpochMillis = 1L,
    armedAtEpochMillis = 1L,
    blockingAppliedAtEpochMillis = if (blockingApplied) bedtime else null,
    ringingAtEpochMillis = null,
    alarmSoundStoppedAtEpochMillis = null,
    triggerElapsedAtEpochMillis = null,
    nfcVerifiedAtEpochMillis = null,
    releasingAtEpochMillis = null,
    completedAtEpochMillis = null,
    cancelledAtEpochMillis = null,
    failureCode = null,
)
