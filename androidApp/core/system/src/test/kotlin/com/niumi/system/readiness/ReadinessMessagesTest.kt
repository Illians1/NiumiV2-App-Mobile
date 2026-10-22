package com.niumi.feature.setup.readiness

import com.google.common.truth.Truth.assertThat
import com.niumi.system.readiness.ReadinessCheckId
import org.junit.Test

/**
 * SPEC_ANDROID §13 n'illustrait que cinq messages pour quatorze contrôles. Les cinq sont repris
 * mot pour mot ci-dessous ; les neuf autres ont été rédigés à l'étape 12b et ajoutés à §13 dans le
 * même changement. Le test prouve l'exhaustivité sur `ReadinessCheckId.entries` : un contrôle
 * ajouté sans message casse la compilation du `when` puis ce test.
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
    fun exactAlarmMessageMatchesTheSpecWordForWord() {
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.EXACT_ALARM)).isEqualTo(
            "Niumi ne peut pas programmer ce réveil, car l'accès aux alarmes exactes n'est pas " +
                "disponible sur cet appareil.",
        )
    }

    @Test
    fun notificationsMessageMatchesTheSpecWordForWord() {
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.NOTIFICATIONS))
            .isEqualTo(
                "Active les notifications de Niumi : c'est par elles que tu ouvres l'écran de scan " +
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
                    "du réveil sur l'écran de verrouillage. Désactive-le avant de démarrer la session.",
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
            "Le mode Ne pas déranger peut empêcher la sonnerie d'être audible. Vérifie qu'il " +
                "autorise les alarmes.",
        )
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

/**
 * Défaut trouvé sur appareil le 2026-09-11 : la liste affichait le message de remédiation à côté
 * d'un ✓ (« ✓ Le volume des alarmes est à zéro » alors que le contrôle passait), ce que
 * SPEC_ANDROID §15 interdit — « ne jamais afficher un faux état de fiabilité ». Un contrôle
 * satisfait doit être nommé, pas décrit par sa panne.
 */
class ReadinessLabelsTest {
    @Test
    fun everyCheckHasANonBlankLabel() {
        ReadinessCheckId.entries.forEach { id ->
            assertThat(ReadinessMessages.labelFor(id).isBlank()).isFalse()
        }
    }

    @Test
    fun everyLabelIsDistinct() {
        val labels = ReadinessCheckId.entries.map { ReadinessMessages.labelFor(it) }
        assertThat(labels.toSet()).hasSize(ReadinessCheckId.entries.size)
    }

    @Test
    fun noLabelRestatesTheFailureItDescribes() {
        // Un libellé nomme l'état attendu ; il ne peut pas être le message de panne lui-même.
        ReadinessCheckId.entries.forEach { id ->
            assertThat(ReadinessMessages.labelFor(id)).isNotEqualTo(ReadinessMessages.forCheck(id))
        }
    }

    /** SPEC_ANDROID §13 (Lot 8) : le contrôle parle de boîtiers au pluriel, mot pour mot. */
    @Test
    fun pairedBoxTextsMatchTheSpecWordForWord() {
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.PAIRED_BOX))
            .isEqualTo(
                "Aucun boîtier n'est associé. Associe au moins un boîtier Niumi : c'est l'un d'eux qui " +
                    "terminera ta session.",
            )
        assertThat(ReadinessMessages.labelFor(ReadinessCheckId.PAIRED_BOX)).isEqualTo("Boîtiers associés")
        assertThat(ReadinessMessages.actionLabelFor(ReadinessCheckId.PAIRED_BOX)).isEqualTo("Associer un boîtier")
        assertThat(ReadinessMessages.CHANGE_PAIRED_BOX_LABEL).isEqualTo("Gérer mes boîtiers")
    }

    @Test
    fun theSatisfiedPairedBoxDetailIsSingularForOneAndPluralBeyond() {
        assertThat(ReadinessMessages.pairedBoxCountDetail(1)).isEqualTo("1 boîtier associé")
        assertThat(ReadinessMessages.pairedBoxCountDetail(3)).isEqualTo("3 boîtiers associés")
        assertThat(ReadinessMessages.pairedBoxCountDetail(5)).isEqualTo("5 boîtiers associés")
    }
}
