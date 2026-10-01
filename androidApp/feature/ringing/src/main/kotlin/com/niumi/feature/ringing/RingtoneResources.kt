package com.niumi.feature.ringing

import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.audio.RingtoneResourceResolver

/**
 * Table `clé → R.raw` des sonneries de [NiumiRingtones] (SPEC_ANDROID §10.2, Lot 7). Elle vit ici
 * parce que `:feature:ringing` possède les fichiers ; `:core:system` ne peut pas référencer son `R`
 * (SPEC_ANDROID §6). Toute clé hors catalogue — `NiumiRingtones.LEGACY_KEY` comprise — se lit
 * `null` : c'est `RingingSoundResolver` qui la replie sur la sonnerie par défaut, avant d'arriver ici.
 *
 * La référence explicite à chaque ressource est aussi ce qui empêche `shrinkResources` de les
 * retirer de l'APK de publication.
 */
object RingtoneResources : RingtoneResourceResolver {
    private val resources: Map<String, Int> =
        mapOf(
            "niumi_bell" to R.raw.niumi_bell,
            "niumi_energique" to R.raw.niumi_energique,
            "niumi_oiseaux" to R.raw.niumi_oiseaux,
            "niumi_piano" to R.raw.niumi_piano,
        )

    override fun resourceId(ringtoneKey: String): Int? = resources[ringtoneKey]
}
