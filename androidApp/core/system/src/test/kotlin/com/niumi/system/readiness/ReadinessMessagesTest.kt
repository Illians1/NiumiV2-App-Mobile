package com.niumi.system.readiness

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §13 n'illustrait que cinq messages pour quatorze contrôles. Les cinq sont repris
 * mot pour mot ci-dessous ; les neuf autres ont été rédigés à l'étape 12b et ajoutés à §13 dans le
 * même changement. Le test prouve l'exhaustivité sur `ReadinessCheckId.entries` : un contrôle
 * ajouté sans message casse la compilation du `when` puis ce test.
 *
 * Étape 36 : déplacé de `:feature:setup` avec ses textes, que l'écran 2 et le récapitulatif
 * affichent tous deux ; apostrophe typographique (D13).
 */
class ReadinessMessagesTest {
    @Test
    fun everyCheckHasANonBlankMessage() {
        ReadinessCheckId.entries.forEach { id ->
            assertThat(ReadinessMessages.forCheck(id).isBlank()).isFalse()
        }
    }

    @Test
    fun everyMessageIsDistinct() {
        val messages = ReadinessCheckId.entries.map { ReadinessMessages.forCheck(it) }
        assertThat(messages.toSet()).hasSize(ReadinessCheckId.entries.size)
    }

    @Test
    fun noTextUsesAStraightApostrophe() {
        val texts =
            ReadinessCheckId.entries.flatMap {
                listOf(
                    ReadinessMessages.forCheck(it),
                    ReadinessMessages.actionLabelFor(it),
                )
            } +
                ReadinessMessages.EXACT_ALARM_DIAGNOSTIC
        texts.forEach { assertThat(it).doesNotContain("'") }
    }

    @Test
    fun exactAlarmMessageMatchesTheSpecWordForWord() {
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.EXACT_ALARM)).isEqualTo(
            "Niumi ne peut pas programmer ce réveil, car l’accès aux alarmes exactes n’est pas " +
                "disponible sur cet appareil.",
        )
    }

    @Test
    fun notificationsMessageMatchesTheSpecWordForWord() {
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.NOTIFICATIONS))
            .isEqualTo(
                "Active les notifications de Niumi : c’est par elles que tu ouvres l’écran de scan " +
                    "quand ton réveil sonne.",
            )
    }

    /**
     * Réécrit à l'étape 32 d'après la mesure : en silence total, ni son, ni vibration, ni
     * notification visible sur l'écran de verrouillage.
     */
    @Test
    fun totalSilenceMessageMatchesTheSpecWordForWord() {
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.DND_TOTAL_SILENCE))
            .isEqualTo(
                "Le silence total coupe le son et la vibration des alarmes et cache la notification " +
                    "du réveil sur l’écran de verrouillage. Désactive-le avant de démarrer la session.",
            )
    }

    @Test
    fun nfcEnabledMessageMatchesTheSpecWordForWord() {
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.NFC_ENABLED))
            .isEqualTo("Le NFC est désactivé. Active-le avant de démarrer la session.")
    }

    @Test
    fun alarmVolumeMessageMatchesTheSpecWordForWord() {
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.ALARM_VOLUME))
            .isEqualTo("Le volume des alarmes est à zéro. Augmente-le avant de continuer.")
    }

    @Test
    fun otherDoNotDisturbModeMessageMatchesTheSpecWordForWord() {
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.DND_OTHER_MODE)).isEqualTo(
            "Le mode Ne pas déranger peut empêcher la sonnerie d’être audible. Vérifie qu’il " +
                "autorise les alarmes.",
        )
    }

    /** Feuilles de la tuile « Blocage » (étape 36, Q1) : texte et bouton de §13, mot pour mot. */
    @Test
    fun batteryAndRecentsTextsMatchTheSpecWordForWord() {
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.BATTERY_OPTIMIZATION)).isEqualTo(
            "Les restrictions de batterie peuvent geler Niumi et désactiver le blocage sans " +
                "prévenir. Autorise Niumi à fonctionner sans restriction.",
        )
        assertThat(ReadinessMessages.actionLabelFor(ReadinessCheckId.BATTERY_OPTIMIZATION))
            .isEqualTo("Lever les restrictions de batterie")
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.RECENTS_LOCK)).isEqualTo(
            "Sur ce téléphone, « Tout effacer » dans les applications récentes arrête le blocage, " +
                "sauf si Niumi y est verrouillé. Ouvre les applications récentes, appuie longuement " +
                "sur Niumi et touche le cadenas. C’est à faire une seule fois.",
        )
        assertThat(ReadinessMessages.actionLabelFor(ReadinessCheckId.RECENTS_LOCK)).isEqualTo("J’ai verrouillé Niumi")
    }

    @Test
    fun batteryMessageNamesTheSettingWithoutAskingForAConfirmation() {
        // §13, étape 25 : l'exemption est détectée. Demander une confirmation ferait croire
        // qu'un mot suffit, alors que seul le réglage compte.
        val message = ReadinessMessages.forCheck(ReadinessCheckId.BATTERY_OPTIMIZATION)

        assertThat(message).contains("sans restriction")
        assertThat(message).doesNotContain("confirme")
    }

    @Test
    fun everyActionHasANonBlankLabel() {
        ReadinessCheckId.entries.forEach { id ->
            assertThat(ReadinessMessages.actionLabelFor(id).isBlank()).isFalse()
        }
    }
}
