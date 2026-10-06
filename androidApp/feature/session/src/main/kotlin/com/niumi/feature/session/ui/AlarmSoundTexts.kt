package com.niumi.feature.session.ui

import com.niumi.system.audio.AlarmSoundSettings
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.audio.VolumeRampDurations

/**
 * Résumé partagé des écrans 5, 6 et 7 (SPEC_ANDROID §15, Lot 7) : les trois écrans résument le
 * réglage par la même phrase, et ne peuvent donc pas la rédiger chacun de son côté.
 *
 * Les durées et les libellés sont associés à [VolumeRampDurations.SECONDS] et [NiumiRingtones.ALL]
 * par position plutôt que par des nombres recopiés ici (`MagicNumber` de detekt) : une
 * cinquième durée ou une cinquième sonnerie ajoutée au catalogue sans mise à jour de ces deux
 * tables échoue bruyamment plutôt que silencieusement.
 */
object AlarmSoundTexts {
    private val DURATION_LABELS: Map<Int, String> =
        VolumeRampDurations.SECONDS.zip(listOf("30 s", "1 min", "2 min", "5 min")).toMap()

    private val RINGTONE_LABELS: Map<String, String> = NiumiRingtones.ALL.associate { it.key to it.label }

    fun durationLabel(seconds: Int): String =
        DURATION_LABELS[seconds] ?: error("Durée de montée hors catalogue : $seconds")

    /**
     * « {Sonnerie} · volume constant » ou « {Sonnerie} · volume progressif sur {durée} ». Une clé
     * hors catalogue replie sur le libellé par défaut (même principe que `RingingSoundResolver`) :
     * l'affichage ne doit jamais rester vide pour un réglage que le moteur jouera quand même.
     */
    fun summary(settings: AlarmSoundSettings): String {
        val label = RINGTONE_LABELS[settings.ringtoneKey] ?: RINGTONE_LABELS.getValue(NiumiRingtones.DEFAULT_KEY)
        val rampSeconds = settings.volumeRampSeconds
        return if (rampSeconds == null) {
            "$label · volume constant"
        } else {
            "$label · volume progressif sur ${durationLabel(rampSeconds)}"
        }
    }
}
