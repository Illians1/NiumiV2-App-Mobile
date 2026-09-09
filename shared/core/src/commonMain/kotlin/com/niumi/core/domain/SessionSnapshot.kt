package com.niumi.core.domain

/**
 * Snapshot canonique d'une session (SPEC_CORE_KMP §7.1). `schemaVersion` vaut `1` pour le MVP.
 * `revision` augmente à chaque décision persistée. Toutes les dates exposées sont en millisecondes
 * Unix.
 */
public data class SessionSnapshot(
    val schemaVersion: Int,
    val revision: Long,
    val sessionId: String,
    val wakeSchedule: WakeSchedule,
    val state: SessionState,
    val releaseTarget: ReleaseTarget?,
    val health: SessionHealth,
    val createdAtEpochMillis: Long,
    val armedAtEpochMillis: Long?,
    val ringingAtEpochMillis: Long?,
    val alarmSoundStoppedAtEpochMillis: Long?,
    val triggerElapsedAtEpochMillis: Long?,
    val nfcVerifiedAtEpochMillis: Long?,
    val releasingAtEpochMillis: Long?,
    val completedAtEpochMillis: Long?,
    val cancelledAtEpochMillis: Long?,
    val failureCode: String?,
) {
    public companion object {
        /** Version du schéma de [SessionSnapshot] pour le MVP (SPEC_CORE_KMP §7.1). */
        public const val SCHEMA_VERSION: Int = 1
    }
}
