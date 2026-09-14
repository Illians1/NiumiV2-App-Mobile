package com.niumi.system.boot

import com.google.common.truth.Truth.assertThat
import com.niumi.system.session.ReconcileReason
import org.junit.Test

/**
 * Table action → [ReconcileReason] de SPEC_ANDROID §9.3. Seule partie décidable de
 * [SystemEventsReceiver], le receveur lui-même n'étant pas instanciable en JVM (pas de Robolectric
 * dans le dépôt).
 */
class SystemEventReasonsTest {
    @Test
    fun lockedBootCompletedReconcilesBeforeUnlock() {
        assertThat(SystemEventReasons.reasonOf("android.intent.action.LOCKED_BOOT_COMPLETED"))
            .isEqualTo(ReconcileReason.LOCKED_BOOT)
    }

    @Test
    fun bootCompletedReconcilesAfterUnlock() {
        assertThat(SystemEventReasons.reasonOf("android.intent.action.BOOT_COMPLETED"))
            .isEqualTo(ReconcileReason.BOOT)
    }

    @Test
    fun myPackageReplacedReconcilesAfterAnUpdate() {
        assertThat(SystemEventReasons.reasonOf("android.intent.action.MY_PACKAGE_REPLACED"))
            .isEqualTo(ReconcileReason.PACKAGE_REPLACED)
    }

    /** `ACTION_TIME_CHANGED` porte l'action `TIME_SET` : le nom de la constante n'est pas l'action. */
    @Test
    fun timeSetIsTheActionBehindTimeChanged() {
        assertThat(SystemEventReasons.reasonOf("android.intent.action.TIME_SET"))
            .isEqualTo(ReconcileReason.TIME_CHANGED)
    }

    @Test
    fun timezoneChangedReconciles() {
        assertThat(SystemEventReasons.reasonOf("android.intent.action.TIMEZONE_CHANGED"))
            .isEqualTo(ReconcileReason.TIMEZONE_CHANGED)
    }

    @Test
    fun anUnknownOrAbsentActionReconcilesNothing() {
        assertThat(SystemEventReasons.reasonOf("android.intent.action.SCREEN_ON")).isNull()
        assertThat(SystemEventReasons.reasonOf(null)).isNull()
    }

    /**
     * `USER_UNLOCKED` n'est pas dans la table : Android ne le délivre qu'aux receveurs enregistrés
     * à chaud. `SystemEventsRegistrar` fournit sa raison directement. Le déclarer ici laisserait
     * croire qu'un filtre de manifeste suffirait.
     */
    @Test
    fun userUnlockedIsNotHandledByTheManifestReceiver() {
        assertThat(SystemEventReasons.reasonOf("android.intent.action.USER_UNLOCKED")).isNull()
    }

    /** Le manifeste et la table doivent lister exactement les mêmes cinq actions. */
    @Test
    fun everyManifestActionHasAReason() {
        assertThat(SystemEventReasons.manifestActions).hasSize(5)
        SystemEventReasons.manifestActions.forEach { action ->
            assertThat(SystemEventReasons.reasonOf(action)).isNotNull()
        }
    }
}
