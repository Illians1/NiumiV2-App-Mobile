package com.niumi.feature.session.success

import com.niumi.designsystem.format.NiumiTimeFormat
import java.time.LocalTime

/**
 * Écran 11 · Réussite (étape 46), recopié de `design-plan/SPECIFICATION.md` §15 et de
 * `TEXTES-DES-MAQUETTES.md` §42, apostrophe typographique (D13). Le titre ne prend pas de point
 * final, les phrases dessous oui (décision de Mehdi du 2026-10-26). La maquette ne montre que
 * « 4 minutes » : les autres délais sont accordés (« Moins d’une minute », « 1 minute »), puis
 * écrits comme les durées de l'app dès 60 min (« 1 h 15 », §2) — décision de Mehdi du 2026-10-25.
 * Vérifié par `SuccessTextsTest`.
 */
object SuccessTexts {
    /** Début en 600 de la comparaison (maquette 42). */
    const val BRAVO = "Bravo !"
    const val APPS_BACK = "Tes applications sont de nouveau accessibles."
    const val BACK_HOME = "Retour à l’accueil"

    private const val MINUTES_PER_HOUR = 60

    fun standingAt(time: LocalTime): String = "Debout à ${NiumiTimeFormat.clock(time)}"

    fun afterAlarm(minutes: Int): String =
        if (minutes == 0) "Moins d’une minute après la sonnerie." else "${amount(minutes)} après la sonnerie."

    /** Suite de [BRAVO] : « C’est 11 minutes de moins qu’avant Niumi. » */
    fun savedComparison(minutes: Int): String = "C’est ${amount(minutes)} de moins qu’avant Niumi."

    private fun amount(minutes: Int): String =
        when {
            minutes >= MINUTES_PER_HOUR -> NiumiTimeFormat.duration(minutes)
            minutes == 1 -> "1 minute"
            else -> "$minutes minutes"
        }
}
