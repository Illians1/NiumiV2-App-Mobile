package com.niumi.system.plan

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Décodage pur du planning stocké dans `niumi_wake_plan` (étape 43) : ce que le `DataStore` contient
 * littéralement devient un [WakePlan], champ par champ, sans dépendance Android. Valeurs par défaut
 * de `design-plan/SPECIFICATION.md` §2 et §9 : interrupteur éteint, lundi à vendredi, 7:00, 22:30.
 */
class WakePlanStoreTest {
    @Test
    fun theDefaultPlanIsOffOnWeekdaysAtSevenWithBedtimeAtTenThirty() {
        assertThat(WakePlan.DEFAULT)
            .isEqualTo(
                WakePlan(
                    enabled = false,
                    enabledIsoDays = setOf(1, 2, 3, 4, 5),
                    wakeTimeIso = "07:00",
                    bedtimeIso = "22:30",
                ),
            )
    }

    @Test
    fun nothingWrittenReadsAsTheDefaultPlan() {
        assertThat(
            decodeWakePlan(enabled = null, days = null, wakeTime = null, bedtime = null),
        ).isEqualTo(WakePlan.DEFAULT)
    }

    @Test
    fun aWrittenPlanRoundTrips() {
        val plan = WakePlan(enabled = true, enabledIsoDays = setOf(6, 7), wakeTimeIso = "09:15", bedtimeIso = "01:00")

        val decoded =
            decodeWakePlan(plan.enabled, encodeIsoDays(plan.enabledIsoDays), plan.wakeTimeIso, plan.bedtimeIso)

        assertThat(decoded).isEqualTo(plan)
    }

    /** « Aucun jour coché » (réveil ponctuel) est un planning valide, jamais relu comme le défaut. */
    @Test
    fun noDayCheckedRoundTripsAsAnEmptySet() {
        val decoded =
            decodeWakePlan(enabled = true, days = encodeIsoDays(emptySet()), wakeTime = "07:00", bedtime = "22:30")

        assertThat(decoded.enabledIsoDays).isEmpty()
    }

    @Test
    fun daysAreEncodedInIsoOrder() {
        assertThat(encodeIsoDays(setOf(7, 1, 3))).isEqualTo("1,3,7")
    }

    /** Une valeur illisible retombe sur le défaut de son seul champ : le reste du planning est conservé. */
    @Test
    fun anUnreadableFieldFallsBackToItsOwnDefaultOnly() {
        val decoded = decodeWakePlan(enabled = true, days = "1,x,9", wakeTime = "25:00", bedtime = "23:45")

        assertThat(decoded)
            .isEqualTo(
                WakePlan(
                    enabled = true,
                    enabledIsoDays = WakePlan.DEFAULT.enabledIsoDays,
                    wakeTimeIso = "07:00",
                    bedtimeIso = "23:45",
                ),
            )
    }

    @Test
    fun aValidPlanIsAccepted() {
        WakePlan(
            enabled = true,
            enabledIsoDays = setOf(1, 7),
            wakeTimeIso = "06:30",
            bedtimeIso = "23:00",
        ).requireValid()
    }

    @Test
    fun anInvalidPlanIsRefusedBeforeAnyWrite() {
        listOf(
            WakePlan.DEFAULT.copy(enabledIsoDays = setOf(0)),
            WakePlan.DEFAULT.copy(enabledIsoDays = setOf(8)),
            WakePlan.DEFAULT.copy(wakeTimeIso = "7:00"),
            WakePlan.DEFAULT.copy(bedtimeIso = "22h30"),
        ).forEach { plan ->
            val thrown = assertThrows(IllegalArgumentException::class.java) { plan.requireValid() }
            assertThat(thrown).hasMessageThat().isEqualTo("WAKE_PLAN_INVALID")
        }
    }
}
