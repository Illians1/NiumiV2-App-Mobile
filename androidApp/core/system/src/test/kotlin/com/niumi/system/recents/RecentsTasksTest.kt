package com.niumi.system.recents

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Étape 25 : seule la tâche de l'activité principale perd sa carte pendant une session. La tâche de
 * l'écran de réveil, déjà exclue des récents par le manifeste, ne doit jamais y être réintroduite.
 */
class RecentsTasksTest {
    @Test
    fun theMainActivityTaskIsTheOneToHide() {
        assertThat(RecentsTasks.isMainTask(MAIN, MAIN)).isTrue()
    }

    @Test
    fun theAlarmScreenTaskIsNeverTouched() {
        assertThat(RecentsTasks.isMainTask("com.niumi.feature.ringing.AlarmActivity", MAIN)).isFalse()
    }

    @Test
    fun aTaskWithoutBaseActivityIsNeverTouched() {
        assertThat(RecentsTasks.isMainTask(null, MAIN)).isFalse()
    }

    private companion object {
        const val MAIN = "com.niumi.app.MainActivity"
    }
}
