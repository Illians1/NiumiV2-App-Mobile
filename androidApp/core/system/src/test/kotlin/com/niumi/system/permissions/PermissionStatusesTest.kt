package com.niumi.system.permissions

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.ReadinessSeverityDto
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.ReadinessCheck
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome
import com.niumi.system.readiness.ReadinessReport
import org.junit.Test

/**
 * Agrégat des quatorze contrôles de SPEC_ANDROID §13 en tuiles (écran 2, étape 36). Décision de
 * Mehdi du 2026-10-21 : batterie et verrou des récents ont chacun leur tuile ; celle du verrou
 * n'existe que là où le verrou existe.
 */
class PermissionStatusesTest {
    private fun report(
        vararg failed: ReadinessCheckId,
        notApplicable: Set<ReadinessCheckId> = emptySet(),
    ) = ReadinessReport(
        checks =
            ReadinessCheckId.entries.map { id ->
                val outcome =
                    when (id) {
                        in failed -> ReadinessOutcome.FAILED
                        in notApplicable -> ReadinessOutcome.NOT_APPLICABLE
                        else -> ReadinessOutcome.PASSED
                    }
                ReadinessCheck(id, ReadinessSeverityDto.BLOCKING_FOR_NIUMI_EXPERIENCE, outcome, actionOf(id))
            },
        appSelectionCount = 0,
        pairedBoxCount = 0,
        candidateTriggerAtEpochMillis = null,
        nowEpochMillis = 0L,
    )

    private fun actionOf(id: ReadinessCheckId): ReadinessAction =
        when (id) {
            ReadinessCheckId.EXACT_ALARM -> ReadinessAction.ShowExactAlarmDiagnostic
            ReadinessCheckId.NOTIFICATIONS -> ReadinessAction.RequestNotificationPermission
            ReadinessCheckId.ALARM_CHANNEL -> ReadinessAction.OpenChannelSettings("niumi_alarm_ringing")
            ReadinessCheckId.ACCESSIBILITY_SERVICE -> ReadinessAction.OpenAccessibilitySettings
            ReadinessCheckId.BATTERY_OPTIMIZATION -> ReadinessAction.OpenBatterySettings
            ReadinessCheckId.RECENTS_LOCK -> ReadinessAction.LockInRecents
            else -> ReadinessAction.Unsupported
        }

    private fun List<PermissionStatus>.of(permission: NiumiPermission) = single { it.permission == permission }

    @Test
    fun theFiveTilesComeInTheDecidedOrder() {
        // Décision de Mehdi du 2026-10-21 : batterie et verrou des récents ont chacun leur tuile,
        // juste après le blocage qu'ils protègent.
        assertThat(permissionStatusesOf(report()).map { it.permission })
            .containsExactly(
                NiumiPermission.ALARM,
                NiumiPermission.APP_BLOCKING,
                NiumiPermission.BATTERY,
                NiumiPermission.RECENTS_LOCK,
                NiumiPermission.NOTIFICATIONS,
            ).inOrder()
    }

    @Test
    fun allChecksPassedGrantsEveryTileWithNothingPending() {
        val statuses = permissionStatusesOf(report())

        assertThat(statuses.all { it.granted }).isTrue()
        assertThat(statuses.all { it.pending == null }).isTrue()
    }

    @Test
    fun alarmDependsOnTheExactAlarmCheckOnly() {
        val statuses = permissionStatusesOf(report(ReadinessCheckId.EXACT_ALARM))

        assertThat(statuses.of(NiumiPermission.ALARM).granted).isFalse()
        assertThat(statuses.of(NiumiPermission.ALARM).pending?.id).isEqualTo(ReadinessCheckId.EXACT_ALARM)
        assertThat(statuses.filter { it.permission != NiumiPermission.ALARM }.all { it.granted }).isTrue()
    }

    @Test
    fun eachDeviceCheckUngrantsItsOwnTileOnly() {
        mapOf(
            ReadinessCheckId.ACCESSIBILITY_SERVICE to NiumiPermission.APP_BLOCKING,
            ReadinessCheckId.BATTERY_OPTIMIZATION to NiumiPermission.BATTERY,
            ReadinessCheckId.RECENTS_LOCK to NiumiPermission.RECENTS_LOCK,
        ).forEach { (id, permission) ->
            val statuses = permissionStatusesOf(report(id))

            assertThat(statuses.of(permission).granted).isFalse()
            assertThat(statuses.of(permission).pending?.id).isEqualTo(id)
            assertThat(statuses.filter { it.permission != permission }.all { it.granted }).isTrue()
        }
    }

    @Test
    fun notificationsAskForThePermissionBeforeTheAlarmChannel() {
        val both = permissionStatusesOf(report(ReadinessCheckId.ALARM_CHANNEL, ReadinessCheckId.NOTIFICATIONS))
        assertThat(both.of(NiumiPermission.NOTIFICATIONS).pending?.id).isEqualTo(ReadinessCheckId.NOTIFICATIONS)

        val channel = permissionStatusesOf(report(ReadinessCheckId.ALARM_CHANNEL))
        assertThat(channel.of(NiumiPermission.NOTIFICATIONS).granted).isFalse()
        assertThat(channel.of(NiumiPermission.NOTIFICATIONS).pending?.id).isEqualTo(ReadinessCheckId.ALARM_CHANNEL)
    }

    @Test
    fun theRecentsLockTileIsHiddenWhereTheLockDoesNotExist() {
        // Verrou des récents hors HyperOS (§13) : rien à demander, donc pas de tuile (2026-10-21).
        val statuses = permissionStatusesOf(report(notApplicable = setOf(ReadinessCheckId.RECENTS_LOCK)))

        assertThat(statuses.map { it.permission }).doesNotContain(NiumiPermission.RECENTS_LOCK)
        assertThat(statuses).hasSize(4)
    }

    /**
     * Relecture de l'étape 36 : pendant la fenêtre de liaison (`UnlockSettling`, 30 s après le
     * démarrage du processus), un service inscrit mais pas encore relié vaut `NOT_APPLICABLE`. Ce
     * n'est pas « sans objet » mais « pas encore jugeable » : après un balayage sur HyperOS, le
     * service ne reviendra pas. La tuile ne se dit donc pas accordée.
     */
    @Test
    fun anAccessibilityServiceNotYetJudgeableLeavesTheBlockingTileToDo() {
        val blocking =
            permissionStatusesOf(report(notApplicable = setOf(ReadinessCheckId.ACCESSIBILITY_SERVICE)))
                .of(NiumiPermission.APP_BLOCKING)

        assertThat(blocking.granted).isFalse()
        assertThat(blocking.pending?.id).isEqualTo(ReadinessCheckId.ACCESSIBILITY_SERVICE)
        assertThat(blocking.isAwaitingJudgement).isTrue()
    }

    @Test
    fun aJudgedFailureIsNotAwaitingJudgement() {
        val blocking =
            permissionStatusesOf(
                report(ReadinessCheckId.ACCESSIBILITY_SERVICE),
            ).of(NiumiPermission.APP_BLOCKING)

        assertThat(blocking.isAwaitingJudgement).isFalse()
    }

    @Test
    fun checksWithoutATileNeverUngrantAnything() {
        // Q1 : Ne pas déranger, volume, NFC, boîtiers, applications et heure n'ont pas de tuile.
        val statuses =
            permissionStatusesOf(
                report(
                    ReadinessCheckId.NFC_PRESENT,
                    ReadinessCheckId.NFC_ENABLED,
                    ReadinessCheckId.PAIRED_BOX,
                    ReadinessCheckId.APP_SELECTION,
                    ReadinessCheckId.ALARM_VOLUME,
                    ReadinessCheckId.DND_TOTAL_SILENCE,
                    ReadinessCheckId.DND_OTHER_MODE,
                    ReadinessCheckId.FUTURE_TRIGGER,
                ),
            )

        assertThat(statuses.all { it.granted }).isTrue()
    }
}
