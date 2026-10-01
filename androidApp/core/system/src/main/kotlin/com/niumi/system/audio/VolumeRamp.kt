package com.niumi.system.audio

/**
 * Montée progressive d'une sonnerie (SPEC_ANDROID §10.2, Lot 7). [startedAtEpochMillis] est
 * `ringingAtEpochMillis` du snapshot, pas l'instant où le son démarre : après une mort du
 * processus, la montée reprend là où elle en était. Son absence (`null`) signifie volume constant.
 */
data class VolumeRamp(
    val durationMs: Long,
    val startedAtEpochMillis: Long,
)

/** Durées proposées, liste fermée comme les heures (plan maître, phase J, décision 3). */
object VolumeRampDurations {
    val SECONDS: List<Int> = listOf(30, 60, 120, 300)

    /** 2 min, décision du 2026-09-30 : valeur par défaut et valeur proposée à l'activation de l'interrupteur. */
    const val DEFAULT_SECONDS = 120
}
