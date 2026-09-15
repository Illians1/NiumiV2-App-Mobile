package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.BlockedPackage
import com.niumi.database.EventReceipt
import com.niumi.database.StoredDecision
import com.niumi.database.blocking.BlockedPackagesState
import com.niumi.database.directboot.DirectBootSnapshot
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Corruption explicite et non destructive (SPEC_CORE_KMP §13, « aucune suppression silencieuse » ;
 * SPEC_ANDROID §7.3, §18 ; étape 20). Trois cas distincts, chacun avec sa propre conclusion : voir
 * `DirectBootMerger.merge()` pour la réécriture, et `SessionReconciler.reconcile` pour la
 * consommation de son résultat, jusque-là jetée.
 */
class SessionReconcilerCorruptionTest {
    private suspend fun seedArmedSession(harness: TestCoordinatorHarness): SessionStateDto {
        val snapshot =
            SessionDtoFixtures.snapshotInState(SessionStateDto.ARMED).copy(health = SessionHealthDto.HEALTHY)
        harness.gateway.commit(
            StoredDecision(
                snapshot = snapshot,
                receipt = EventReceipt("seed", snapshot.sessionId, "hash", 1, 900L),
                effects = emptyList(),
                androidExtras = SessionDtoFixtures.extras(),
            ),
        )
        return snapshot.state
    }

    @Test
    fun aCorruptedDirectBootProjectionWithAValidRoomIsReportedAndRepaired() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.unlockState.isUserUnlocked = true
            harness.directBootStore.seed(DirectBootSnapshot.Corrupted("DIRECT_BOOT_MALFORMED_JSON"))
            seedArmedSession(harness)
            // Room « réel » de la fusion (distinct du gateway du coordinateur dans ce harnais) :
            // seedé avec la même session pour que la réécriture ait quelque chose à projeter.
            harness.roomSessionStore.seed(
                snapshot =
                    SessionDtoFixtures
                        .snapshotInState(
                            SessionStateDto.ARMED,
                        ).copy(health = SessionHealthDto.HEALTHY),
                extras = SessionDtoFixtures.extras(),
            )

            harness.coordinator.reconcile(ReconcileReason.USER_UNLOCKED)

            assertThat(harness.technicalEventLog.logged).contains(TechnicalEventType.SNAPSHOT_CORRUPTED)
            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .contains(IncidentCodes.SNAPSHOT_CORRUPTED)
            // Session non perdue : toujours ARMED, toujours présente.
            assertThat((harness.gateway.load() as LoadResult.Present).snapshot.state).isEqualTo(SessionStateDto.ARMED)
            // La projection Direct Boot est réécrite depuis Room, pas laissée corrompue.
            assertThat(harness.directBootStore.read()).isInstanceOf(DirectBootSnapshot.Active::class.java)
        }

    /**
     * Le journal technique, non dédupliqué, garde chaque détection ; l'incident, lui, n'est
     * consigné qu'une fois par session — même garde que le changement d'horloge (étape 19).
     * `roomSessionStore` reste vide ici : la réécriture n'a rien à projeter, donc le fichier reste
     * corrompu d'une passe à l'autre, ce qui est exactement ce que ce test veut exercer.
     */
    @Test
    fun theCorruptionIncidentIsRecordedOncePerSessionAcrossPasses() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.unlockState.isUserUnlocked = true
            harness.directBootStore.seed(DirectBootSnapshot.Corrupted("DIRECT_BOOT_IO_ERROR"))
            seedArmedSession(harness)

            harness.coordinator.reconcile(ReconcileReason.USER_UNLOCKED)
            harness.coordinator.reconcile(ReconcileReason.USER_UNLOCKED)

            assertThat(
                harness.gateway.incidentsRecorded.map { it.second.code }.count {
                    it ==
                        IncidentCodes.SNAPSHOT_CORRUPTED
                },
            ).isEqualTo(1)
            assertThat(harness.technicalEventLog.logged.count { it == TechnicalEventType.SNAPSHOT_CORRUPTED })
                .isEqualTo(2)
        }

    /**
     * Sans session lisible nulle part, ni `sessionId` ni révision n'existent pour porter un
     * `SessionIncident` : seul l'événement technique est possible. Rien n'est écrit, rien n'est
     * effacé, et le blocage déjà en place reste inchangé.
     */
    @Test
    fun bothStoresUnreadableDispatchNothingAndKeepTheBlockingProjection() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.unlockState.isUserUnlocked = true
            harness.directBootStore.seed(DirectBootSnapshot.Corrupted("DIRECT_BOOT_MALFORMED_JSON"))
            harness.gateway.forceUnreadable = "SQLITE_CORRUPT"
            val blockedState =
                BlockedPackagesState.Active(
                    SessionDtoFixtures.SESSION_ID,
                    setOf(BlockedPackage("com.example.app", "Exemple")),
                )
            harness.blockedPackagesProjection.state = blockedState

            val result = harness.coordinator.reconcile(ReconcileReason.USER_UNLOCKED)

            assertThat(result.sessionId).isNull()
            assertThat(harness.technicalEventLog.logged.count { it == TechnicalEventType.SNAPSHOT_CORRUPTED })
                .isEqualTo(1)
            assertThat(harness.gateway.incidentsRecorded).isEmpty()
            assertThat(harness.journal.calls).doesNotContain("gateway.commit")
            assertThat(harness.journal.calls).doesNotContain("gateway.clearActive")
            assertThat(harness.blockedPackagesProjection.current()).isEqualTo(blockedState)
            assertThat(harness.storageIntegrity.failure.value).isEqualTo("SQLITE_CORRUPT")
        }

    /**
     * L'accueil et l'écran de diagnostic lisent [StorageIntegrityState] pour décider d'une
     * redirection (§20) ; elle doit redevenir lisible dès qu'une passe ordinaire réussit, sans
     * quoi la redirection resterait bloquée après que le stockage soit redevenu accessible.
     */
    @Test
    fun aReadablePassClearsAPreviousStorageFailure() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.gateway.forceUnreadable = "SQLITE_CORRUPT"
            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)
            assertThat(harness.storageIntegrity.failure.value).isEqualTo("SQLITE_CORRUPT")
            harness.gateway.forceUnreadable = null

            harness.coordinator.reconcile(ReconcileReason.PROCESS_START)

            assertThat(harness.storageIntegrity.failure.value).isNull()
        }
}
