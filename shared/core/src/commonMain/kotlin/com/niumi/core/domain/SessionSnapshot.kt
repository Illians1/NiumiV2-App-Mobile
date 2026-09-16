package com.niumi.core.domain

/**
 * Snapshot canonique d'une session (SPEC_CORE_KMP §7.1). `schemaVersion` vaut `2` depuis le contrat
 * 1.3 (`1` pour le MVP d'origine) : un snapshot de version 1 se lit comme un blocage immédiat dont
 * `blockingAppliedAtEpochMillis` vaut `createdAtEpochMillis`. `revision` augmente à chaque décision
 * persistée. Toutes les dates exposées sont en millisecondes Unix.
 */
public data class SessionSnapshot(
    val schemaVersion: Int,
    val revision: Long,
    val sessionId: String,
    val wakeSchedule: WakeSchedule,
    val blockingSchedule: BlockingSchedule,
    val state: SessionState,
    val releaseTarget: ReleaseTarget?,
    val health: SessionHealth,
    val createdAtEpochMillis: Long,
    val armedAtEpochMillis: Long?,
    val blockingAppliedAtEpochMillis: Long?,
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
        /** Version du schéma de [SessionSnapshot] pour le contrat 1.3 (SPEC_CORE_KMP §7.1). */
        public const val SCHEMA_VERSION: Int = 2
    }
}
