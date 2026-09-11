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
            .isEqualTo("Active les notifications pour que l'écran du réveil puisse s'afficher.")
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
    fun batteryMessageAsksForAConfirmationRatherThanClaimingDetection() {
        // §13 : le contrôle est satisfait par la confirmation de l'utilisateur, la détection
        // AOSP étant structurellement partielle. Le message doit le dire, pas le masquer.
        assertThat(ReadinessMessages.forCheck(ReadinessCheckId.BATTERY_OPTIMIZATION))
            .contains("confirme")
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
}
