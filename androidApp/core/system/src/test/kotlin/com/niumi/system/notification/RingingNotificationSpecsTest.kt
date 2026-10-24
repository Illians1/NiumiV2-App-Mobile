package com.niumi.system.notification

import android.app.Notification
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §10.3 — textes exacts, `CATEGORY_ALARM`, `ongoing`, visibilité publique, et surtout
 * **aucune action** : garde-fou du principe « aucune action d'arrêt dans le parcours de sonnerie »
 * (SPEC_ANDROID §3, §10.2, §10.4), vérifié automatiquement à chaque modification.
 */
class RingingNotificationSpecsTest {
    /**
     * Lot 9 : la notification est devenue le chemin vers le scan, l'écran n'étant plus imposé
     * par-dessus le verrouillage. Le texte dit le geste ; la visibilité publique garde son contenu
     * lisible sur l'écran de verrouillage même si l'utilisateur masque le contenu sensible.
     */
    @Test
    fun theRingingNotificationOpensTheAlarmScreenWithoutForcingIt() {
        val spec = RingingNotificationSpecs.ringing()

        assertThat(spec.channelId).isEqualTo(NiumiNotificationChannels.alarmRinging.id)
        assertThat(spec.category).isEqualTo(Notification.CATEGORY_ALARM)
        assertThat(spec.title).isEqualTo("Ton réveil Niumi sonne")
        assertThat(spec.text).isEqualTo("Scanne ton Niumi Spot pour l'arrêter et retrouver tes applis.")
        assertThat(spec.ongoing).isTrue()
        assertThat(spec.visibilityPublic).isTrue()
    }

    /**
     * Mesuré le 2026-10-18 : l'alarme de secours fait republier la notification chaque minute, et
     * Android re-présentait chaque republication (bannière, vibration), y compris par-dessus
     * l'écran de réveil. Elle n'alerte qu'une fois tant qu'elle est affichée (§10.3).
     */
    @Test
    fun theRingingNotificationAlertsOnlyOnceWhileItIsShowing() {
        assertThat(RingingNotificationSpecs.ringing().alertOnlyOnce).isTrue()
    }

    @Test
    fun ringingSpecHasNoAction() {
        val spec = RingingNotificationSpecs.ringing()

        assertThat(spec.actions).isEmpty()
    }
}
