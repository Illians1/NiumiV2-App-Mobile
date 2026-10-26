package com.niumi.feature.ringing

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Étape 46, constaté sur appareil le 2026-10-26 : chaque sortie de l'écran de réveil empilait une
 * nouvelle `MainActivity` (quatre dans la tâche), et l'état de l'accueil déjà ouvert — la bulle
 * « Réveil annulé… » — restait dans un exemplaire caché. La sortie revient à l'exemplaire existant,
 * qui reçoit la destination par `onNewIntent`.
 */
class AlarmExitIntentTest {
    @Test
    fun leavingTheAlarmScreenReusesTheExistingMainActivity() {
        val flags = AlarmExitIntent.FLAGS

        assertThat(flags and Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
        assertThat(flags and Intent.FLAG_ACTIVITY_CLEAR_TOP).isNotEqualTo(0)
        assertThat(flags and Intent.FLAG_ACTIVITY_SINGLE_TOP).isNotEqualTo(0)
    }
}
