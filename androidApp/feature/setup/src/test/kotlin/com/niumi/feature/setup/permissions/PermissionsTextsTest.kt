package com.niumi.feature.setup.permissions

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Écran 2 (`design-plan/SPECIFICATION.md` §5, `TEXTES-DES-MAQUETTES.md` §03) : recopie mot pour
 * mot, avec l'apostrophe typographique (D13). Textes revus par Mehdi le 2026-10-21 : introduction,
 * titre selon le nombre de tuiles, tuiles « Batterie » et « Verrouiller Niumi » (nom du 2026-10-22).
 */
class PermissionsTextsTest {
    @Test
    fun theHeaderAndButtonsAreQuotedWordForWord() {
        assertThat(PermissionsTexts.BODY).isEqualTo("Sans elles, Niumi ne peut pas fonctionner.")
        assertThat(PermissionsTexts.AUTHORIZE).isEqualTo("Autoriser")
        assertThat(PermissionsTexts.CONTINUE).isEqualTo("Continuer")
        assertThat(PermissionsTexts.BACK).isEqualTo("Retour")
        assertThat(PermissionsTexts.GRANTED).isEqualTo("Accordée")
        assertThat(PermissionsTexts.CLOSE).isEqualTo("Fermer")
    }

    @Test
    fun theThreeTilesAreQuotedWordForWord() {
        assertThat(PermissionsTexts.ALARM_NAME).isEqualTo("Alarme")
        assertThat(
            PermissionsTexts.ALARM_PHRASE,
        ).isEqualTo("Pour que ton réveil sonne à l’heure, même téléphone verrouillé.")
        assertThat(PermissionsTexts.BLOCKING_NAME).isEqualTo("Blocage des applications")
        assertThat(PermissionsTexts.BLOCKING_PHRASE)
            .isEqualTo("Pour bloquer les applications que tu choisis, et seulement celles-là.")
        assertThat(PermissionsTexts.BATTERY_NAME).isEqualTo("Batterie")
        assertThat(
            PermissionsTexts.BATTERY_PHRASE,
        ).isEqualTo("Pour que le téléphone n’arrête pas Niumi pendant la nuit.")
        assertThat(PermissionsTexts.RECENTS_LOCK_NAME).isEqualTo("Verrouiller Niumi")
        assertThat(PermissionsTexts.RECENTS_LOCK_PHRASE)
            .isEqualTo("Pour que Niumi reste actif quand tu vides les applications récentes.")
        assertThat(PermissionsTexts.NOTIFICATIONS_NAME).isEqualTo("Notifications")
        assertThat(PermissionsTexts.NOTIFICATIONS_PHRASE)
            .isEqualTo("Pour pouvoir te tenir informé de tout ce qui est important.")
    }

    /** Le titre compte les tuiles affichées : cinq sur HyperOS, quatre ailleurs (2026-10-21). */
    @Test
    fun theTitleCountsTheTilesShown() {
        assertThat(PermissionsTexts.title(tileCount = 3)).isEqualTo("Trois autorisations")
        assertThat(PermissionsTexts.title(tileCount = 4)).isEqualTo("Quatre autorisations")
        assertThat(PermissionsTexts.title(tileCount = 5)).isEqualTo("Cinq autorisations")
    }

    @Test
    fun noTextCarriesAStraightApostrophe() {
        assertThat(allTexts.filter { it.contains('\'') }).isEmpty()
    }

    @Test
    fun noTextSwitchesToTheFormalAddress() {
        assertThat(allTexts.filter { FORMAL_ADDRESS.containsMatchIn(it) }).isEmpty()
    }

    private companion object {
        val FORMAL_ADDRESS = Regex("""\b([Vv]ous|[Vv]otre|[Vv]os)\b""")

        val allTexts =
            listOf(
                PermissionsTexts.title(tileCount = 4),
                PermissionsTexts.title(tileCount = 5),
                PermissionsTexts.BODY,
                PermissionsTexts.ALARM_NAME,
                PermissionsTexts.ALARM_PHRASE,
                PermissionsTexts.BLOCKING_NAME,
                PermissionsTexts.BLOCKING_PHRASE,
                PermissionsTexts.BATTERY_NAME,
                PermissionsTexts.BATTERY_PHRASE,
                PermissionsTexts.RECENTS_LOCK_NAME,
                PermissionsTexts.RECENTS_LOCK_PHRASE,
                PermissionsTexts.NOTIFICATIONS_NAME,
                PermissionsTexts.NOTIFICATIONS_PHRASE,
                PermissionsTexts.AUTHORIZE,
                PermissionsTexts.CONTINUE,
                PermissionsTexts.BACK,
                PermissionsTexts.GRANTED,
                PermissionsTexts.CLOSE,
            )
    }
}
