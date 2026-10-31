package com.niumi.feature.session.home

import com.niumi.designsystem.drawing.DialGeometry
import com.niumi.designsystem.format.NiumiTimeFormat
import com.niumi.feature.session.ui.AlarmSoundTexts

/**
 * Textes de l'accueil (`design-plan/SPECIFICATION.md` §9, `TEXTES-DES-MAQUETTES.md` 15 à 17,
 * maquettes 18 à 20 du 2026-10-25), recopiés mot pour mot, apostrophe typographique (D13).
 *
 * Non écrits dans les maquettes : [CHOOSE_TIME], [HOURS] et [MINUTES] (libellés d'accessibilité du
 * cadran et des roues), le cas de la période de 24 h (« 24 h de blocage… », décision du
 * 2026-10-25) et la fenêtre de confirmation avant un blocage immédiat ([IMMEDIATE_BLOCKING_TITLE],
 * [immediateBlockingBody], [BLOCK_NOW], [CANCEL], décision du 2026-10-25, revus par Mehdi le même
 * jour), à valider par Mehdi au premier essai. [WAKE_DAYS], le titre des pastilles (décision de
 * Mehdi du 2026-10-26), n'est pas dessiné non plus.
 */
object HomeTexts {
    const val BEDTIME = "Coucher"
    const val WAKE = "Réveil"
    const val WAKE_IN = "Réveil dans"

    /** Centre du cadran avant le coucher (décision de Mehdi du 2026-10-31, remplace « Réveil dans »). */
    const val BEDTIME_IN = "Coucher dans"
    const val DISABLED = "Désactivé"
    const val WAKE_ENABLED = "Réveil activé"
    const val WAKE_DISABLED = "Réveil désactivé"
    const val NOTHING_PLANNED = "Aucun réveil ni blocage prévu"
    const val BLOCKING_IN_PROGRESS = "Blocage en cours"
    const val BLOCKED_UNTIL_SCAN = "Applications bloquées jusqu’au scan de ton Niumi Spot"

    /**
     * « Annuler le réveil » (D23, 2026-10-24) : la maquette 17 régénérée le 2026-10-25 dit
     * « session », par régression.
     */
    const val CANCEL_WAKE = "Annuler le réveil"

    /**
     * Bulle après l'annulation par scan (`SPECIFICATION.md` §11, D23). Même bulle que M10, sans la
     * coche de la maquette 27 (décision de Mehdi du 2026-10-25, étape 46).
     */
    const val WAKE_CANCELLED = "Réveil annulé. Tes applications sont débloquées."

    /** M10 : bulle en bas d'écran quand on touche un champ verrouillé (décision du 2026-10-25). */
    const val LOCKED_UNTIL_SCAN = "Modifiable après avoir scanné ton Niumi Spot."

    const val ONE_OFF = "Ponctuel"
    const val DAILY = "Quotidien"

    /**
     * Titre de la ligne des onglets, au-dessus des pastilles (décision de Mehdi du 2026-10-26, non
     * dessiné, à valider au premier essai) : un jour coché est un jour de réveil, le coucher a lieu
     * la veille quand son heure est plus tardive (contrat KMP §8.4).
     */
    const val WAKE_DAYS = "Jours de réveil"
    const val BLOCKED_APPS = "Applications bloquées"
    const val RINGTONE = AlarmSoundTexts.ROW_TITLE
    const val BEDTIME_TITLE = "Heure de coucher"
    const val WAKE_TITLE = "Heure de réveil"
    const val CONFIRM = "Valider"
    const val CLOSE = "Fermer"
    const val CHOOSE_TIME = "Choisir l’heure"
    const val HOURS = "Heures"
    const val MINUTES = "Minutes"

    /** Fenêtre avant un changement qui lancerait le blocage tout de suite (§2, décision du 2026-10-25). */
    const val IMMEDIATE_BLOCKING_TITLE = "Tes applications seront bloquées dès maintenant"
    const val BLOCK_NOW = "Bloquer maintenant"
    const val CANCEL = "Annuler"

    /** Pastilles, du lundi au dimanche (jours ISO 1 à 7). */
    val DAY_LABELS = listOf("L", "M", "M", "J", "V", "S", "D")
    val DAY_NAMES = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

    fun blockingIn(minutes: Int): String = "Blocage des apps dans ${countdown(minutes)}"

    /** « 8 h 30 de déconnexion » ; une période de 24 h n'est pas une nuit : « 24 h de blocage ». */
    fun sleep(minutes: Int): String =
        if (minutes >=
            DialGeometry.MINUTES_PER_DAY
        ) {
            FULL_DAY
        } else {
            NiumiTimeFormat.sleep(minutes)
        }

    /**
     * Compte à rebours : « 56 min », « 9 h 26 » ; à partir d'un jour, jours et heures entières,
     * « 1 j 4 h », « 2 j » (décision de Mehdi du 2026-10-25), jamais « 52 h 10 » ni « 24 h 00 ».
     */
    fun countdown(minutes: Int): String {
        if (minutes < DialGeometry.MINUTES_PER_DAY) return NiumiTimeFormat.duration(minutes)
        val days = minutes / DialGeometry.MINUTES_PER_DAY
        val hours = minutes % DialGeometry.MINUTES_PER_DAY / MINUTES_PER_HOUR
        return if (hours == 0) "$days j" else "$days j $hours h"
    }

    /** Texte de la même fenêtre : le coucher demandé, au format du cadran (« 22:30 », « 7:00 »). */
    fun immediateBlockingBody(bedtime: String): String =
        "Ton coucher de $bedtime est déjà passé. Tes applications resteront bloquées jusqu’au scan de ton Niumi Spot."

    fun iconsLabel(count: Int): String =
        if (count == 1) "Icône de l’application bloquée" else "Icônes des $count applications bloquées"

    fun moreApps(hidden: Int): String = "+$hidden"

    /**
     * Phrase du sélecteur, onglet « Coucher » (maquette 19), sur deux lignes : la durée, puis
     * « avant le réveil de 7:00 » d'un seul tenant, tenu par des espaces insécables (décision de Mehdi
     * du 2026-10-30 : la fenêtre rétrécie coupait la phrase et laissait l'heure seule).
     */
    fun sleepBeforeWake(
        minutes: Int,
        wake: String,
    ): String =
        if (minutes >=
            DialGeometry.MINUTES_PER_DAY
        ) {
            "$FULL_DAY\n${unbreakable("jusqu’au réveil de $wake")}"
        } else {
            "${sleep(minutes)}\n${unbreakable("avant le réveil de $wake")}"
        }

    /**
     * Phrase du sélecteur, onglet « Réveil » (maquette 20) : la durée, puis « après le coucher de
     * 22:30 » insécable.
     */
    fun sleepAfterBedtime(
        minutes: Int,
        bedtime: String,
    ): String =
        if (minutes >=
            DialGeometry.MINUTES_PER_DAY
        ) {
            "$FULL_DAY\n${unbreakable("depuis le coucher de $bedtime")}"
        } else {
            "${sleep(minutes)}\n${unbreakable("après le coucher de $bedtime")}"
        }

    private const val FULL_DAY = "24 h de blocage"

    /** Remplace chaque espace par une insécable (U+00A0) : le texte ne se coupe pas. */
    private fun unbreakable(text: String): String = text.replace(' ', '\u00A0')

    private const val MINUTES_PER_HOUR = 60
}
