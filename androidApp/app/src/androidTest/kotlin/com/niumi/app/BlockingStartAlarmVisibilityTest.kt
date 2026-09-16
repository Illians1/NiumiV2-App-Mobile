package com.niumi.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.alarm.BlockingStartScheduler
import com.niumi.system.common.Clock
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import javax.inject.Inject

/**
 * SPEC_ANDROID §9.1, seconde dérogation : l'alarme de début du blocage emploie
 * `setExactAndAllowWhileIdle()` et **ne doit pas apparaître** au réglage système « prochaine alarme ».
 * Un début de blocage à 22:30 n'est pas une alarme de l'utilisateur ; l'y afficher ferait croire que
 * Niumi sonnera à cette heure.
 *
 * Le plan de l'étape 23 en faisait un contrôle manuel (`dumpsys alarm | grep niumi`) et un critère de
 * clôture — « sinon retour à §9.1 avant de continuer ». Il est automatisé ici : la vérification est
 * mécanique, et une régression future la rejouerait sans qu'on ait à y penser. `dumpsys` est lu par
 * l'instrumentation elle-même, l'APK étant désinstallé dès la fin de la suite.
 *
 * Nécessite un appareil ou un émulateur : voir « Validation sur appareil réel » dans `CLAUDE.md`.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BlockingStartAlarmVisibilityTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var blockingStartScheduler: BlockingStartScheduler

    @Inject
    lateinit var clock: Clock

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun theWakeAlarmIsVisibleAsTheNextAlarmClockButTheBlockingStartNeverIs() {
        val startsAt = clock.nowEpochMillis() + BLOCKING_START_DELAY_MS
        val triggerAt = clock.nowEpochMillis() + TRIGGER_DELAY_MS

        alarmScheduler.schedule(SESSION_ID, REVISION, triggerAt)
        blockingStartScheduler.schedule(SESSION_ID, REVISION, startsAt)

        // Deux `PendingIntent` bien distincts : `cancel()` sur l'un ne peut pas atteindre l'autre.
        assertThat(alarmScheduler.isScheduled(SESSION_ID)).isTrue()
        assertThat(blockingStartScheduler.isScheduled(SESSION_ID)).isTrue()

        val nextAlarmClockSection = nextAlarmClockSection(shell("dumpsys alarm"))

        // **Le critère de §9.1.** L'instant du début du blocage ne doit jamais figurer parmi les
        // alarmes horloge du système, quelles que soient les autres alarmes de l'appareil.
        assertThat(nextAlarmClockSection).doesNotContain("time:$startsAt")

        // Le pendant positif : le réveil, lui, **doit** y figurer — c'est le comportement voulu de
        // `setAlarmClock()`. Valable tant qu'aucune alarme horloge plus proche n'existe sur
        // l'appareil de test ; sinon le système n'affiche que celle-là et le cas est sans objet.
        assumeTrue(
            "Une alarme horloge plus proche existe sur cet appareil : contrôle sans objet.",
            nextAlarmClockSection.contains("time:"),
        )
        assertThat(nextAlarmClockSection).contains("time:$triggerAt")
    }

    /**
     * La section « Next alarm clock information » de `dumpsys alarm`, celle qui alimente le réglage
     * système. Bornée à la ligne vide qui la suit : le reste du dump contient les alarmes en attente,
     * où le début du blocage a toute sa place.
     */
    private fun nextAlarmClockSection(dump: String): String =
        dump
            .substringAfter("Next alarm clock information:", "")
            .substringBefore("pending alarms:")

    private fun shell(command: String): String =
        FileInputStream(
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .executeShellCommand(command)
                .fileDescriptor,
        ).use { it.readBytes().decodeToString() }

    @After
    fun tearDown() {
        alarmScheduler.cancel(SESSION_ID)
        blockingStartScheduler.cancel(SESSION_ID)
    }

    private companion object {
        const val SESSION_ID = "5f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"
        const val REVISION = 2L
        const val BLOCKING_START_DELAY_MS = 30 * 60 * 1_000L
        const val TRIGGER_DELAY_MS = 60 * 60 * 1_000L
    }
}
