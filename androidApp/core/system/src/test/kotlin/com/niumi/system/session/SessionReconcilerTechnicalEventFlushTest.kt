package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.system.session.fakes.TestCoordinatorHarness
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Versement du journal technique d'avant déverrouillage (SPEC_ANDROID §17, §9.3 ; étape 20). Le
 * vidage doit avoir lieu au même titre que la fusion Direct Boot, y compris — surtout — quand il
 * n'y a aucune projection à fusionner : c'est de loin le cas le plus fréquent (une fois par
 * démarrage, la fusion elle-même n'ayant rien à absorber au-delà de la première fois).
 */
class SessionReconcilerTechnicalEventFlushTest {
    @Test
    fun theFlushRunsEvenWhenThereIsNoProjectionToMerge() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.unlockState.isUserUnlocked = true
            // Aucune session, aucune projection Direct Boot : `directBootMerger.merge()` sort
            // immédiatement sur `NothingToMerge`. Le vidage, lui, doit tout de même avoir eu lieu.
            harness.coordinator.reconcile(ReconcileReason.USER_UNLOCKED)

            assertThat(harness.technicalEventFlush.callCount).isEqualTo(1)
        }

    @Test
    fun theFlushDoesNotRunOnANonMergingReason() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.unlockState.isUserUnlocked = true

            harness.coordinator.reconcile(ReconcileReason.FOREGROUND)

            assertThat(harness.technicalEventFlush.callCount).isEqualTo(0)
        }
}
