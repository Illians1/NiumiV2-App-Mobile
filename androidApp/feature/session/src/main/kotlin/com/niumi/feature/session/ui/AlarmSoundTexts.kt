package com.niumi.feature.session.ui

import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.audio.VolumeRampDurations

/**
 * Résumé de la ligne « Sonnerie » (SPECIFICATION.md §9, maquette 16) : [name] le son choisi,
 * [detail] « Progressive · {durée} » ou `null` quand la sonnerie progressive est sur « Non » (D5,
 * D6, tranchés le 2026-10-23 — la ligne n'affiche alors que le nom).
 */
data class AlarmSoundSummary(
    val name: String,
    val detail: String?,
)

/**
 * Résumé partagé des écrans 5, 6 et 7 (SPECIFICATION.md §9, étape 40) : les trois écrans résument le
 * réglage de la même façon, et ne peuvent donc pas la rédiger chacun de son côté.
 *
 * Les durées et les libellés sont associés à [VolumeRampDurations.SECONDS] et [NiumiRingtones.ALL]
 * par position plutôt que par des nombres recopiés ici (`MagicNumber` de detekt) : une
 * cinquième durée ou une cinquième sonnerie ajoutée au catalogue sans mise à jour de ces deux
 * tables échoue bruyamment plutôt que silencieusement.
 */
object AlarmSoundTexts {
    const val ROW_TITLE = "Sonnerie"

    private val DURATION_LABELS: Map<Int, String> =
        VolumeRampDurations.SECONDS.zip(listOf("2 min", "5 min", "10 min", "15 min")).toMap()

    private val RINGTONE_LABELS: Map<String, String> = NiumiRingtones.ALL.associate { it.key to it.label }

    fun durationLabel(seconds: Int): String =
        DURATION_LABELS[seconds] ?: error("Durée de montée hors catalogue : $seconds")

    /**
     * Assainit [settings] avant de résumer (point de vigilance 4) : une clé hors catalogue replie
     * sur le libellé par défaut (même principe que `RingingSoundResolver`), une durée hors liste
     * prend la plus proche — l'affichage ne doit jamais rester vide, ni faire planter
     * [durationLabel], pour un réglage que le moteur jouera quand même.
     */
    fun summary(settings: AlarmSoundSettings): AlarmSoundSummary {
        val sanitized = settings.sanitized()
        val label = RINGTONE_LABELS[sanitized.ringtoneKey] ?: RINGTONE_LABELS.getValue(NiumiRingtones.DEFAULT_KEY)
        val rampSeconds = sanitized.volumeRampSeconds
        return AlarmSoundSummary(
            name = label,
            detail = rampSeconds?.let { "Progressive · ${durationLabel(it)}" },
        )
    }
}
