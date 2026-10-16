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

/**
 * Durées proposées, liste fermée comme les heures (plan maître, phase J, décision 3).
 *
 * Resserrée le 2026-10-08 : 30 s et 1 min retirées, remplacées par 10 min et 15 min. Un essai
 * utilisateur sur « Oiseaux » (montée 5 min, volume alarme 7/15) a jugé la montée perçue comme
 * trop rapide dès les premières secondes ; la courbe (`VolumeRampPolicy`) ne dépendant que de la
 * fraction écoulée de la durée, doubler celle-ci divise par deux la vitesse de montée en dB à
 * chaque instant — vérifié par le calcul, pas par un second essai à 10 min (voir
 * `ETAPE-28.md`).
 */
object VolumeRampDurations {
    val SECONDS: List<Int> = listOf(120, 300, 600, 900)

    /** 2 min, décision du 2026-09-30 : valeur par défaut et valeur proposée à l'activation de l'interrupteur. */
    const val DEFAULT_SECONDS = 120
}
