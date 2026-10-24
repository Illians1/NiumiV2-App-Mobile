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

    /**
     * 10 min, décision du 2026-10-23 (révision de l'étape 40, feuille à interrupteur) : valeur par
     * défaut d'une préférence jamais réglée et valeur proposée quand on allume l'interrupteur.
     * Remplace les 2 min du 2026-09-30 ; une session déjà armée garde la durée copiée à l'activation.
     */
    const val DEFAULT_SECONDS = 600

    /**
     * Ramène une durée lue (préférence ou extras de session) à la liste fermée (étape 40,
     * point de vigilance 4) : `null` reste `null` (volume constant), une valeur déjà dans
     * [SECONDS] est gardée, toute autre valeur (30 s et 1 min d'avant l'étape 28, par exemple)
     * prend la durée la plus proche — à égalité, la plus courte, pour que le plein volume
     * arrive plus tôt plutôt que plus tard. Une sonnerie n'est donc jamais muette.
     */
    fun sanitized(seconds: Int?): Int? =
        seconds?.let { value ->
            if (value in SECONDS) {
                value
            } else {
                SECONDS.minBy { candidate -> kotlin.math.abs(candidate - value) }
            }
        }
}
