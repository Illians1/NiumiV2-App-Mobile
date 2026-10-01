package com.niumi.database.migration

/**
 * Réécriture de la sonnerie unique du MVP (Lot 7, SPEC_ANDROID §7.2, §7.3). `niumi_alarm` a quitté
 * l'APK ; une session armée avant la mise à jour la porte encore, en Room comme dans la projection
 * Direct Boot. [MIGRATION_3_4] et `DirectBootMapper` la réécrivent en [REPLACEMENT_KEY].
 *
 * Défini ici et non dans `NiumiRingtones` : `:core:database` ne dépend pas de `:core:system`
 * (SPEC_ANDROID §6). `LegacyRingtoneTest` (`:core:system`) vérifie que les deux concordent. La
 * valeur est historique et ne suit pas un futur changement de sonnerie par défaut.
 */
object LegacyRingtone {
    const val KEY = "niumi_alarm"
    const val REPLACEMENT_KEY = "niumi_piano"

    fun migrate(ringtoneKey: String): String = if (ringtoneKey == KEY) REPLACEMENT_KEY else ringtoneKey
}
