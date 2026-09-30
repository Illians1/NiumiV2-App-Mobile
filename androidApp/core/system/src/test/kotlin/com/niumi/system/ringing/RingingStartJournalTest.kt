package com.niumi.system.ringing

import com.google.common.truth.Truth.assertThat
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.OperationResult
import org.junit.Test

/**
 * `RINGING_STARTED` dit que le son a **démarré** (SPEC_ANDROID §17). Mesuré le 2026-09-29 : le chien
 * de garde relance le service toutes les 60 s pendant `RINGING`, et chaque relance réécrivait
 * l'événement alors que le son tournait déjà — une sonnerie de 30 minutes en écrivait une trentaine
 * dans un journal limité à 200 entrées.
 */
class RingingStartJournalTest {
    @Test
    fun aSoundThatReallyStartsIsJournaled() {
        assertThat(RingingStartJournal.eventsFor(OperationResult.Success))
            .containsExactly(TechnicalEventType.RINGING_STARTED)
    }

    /** Relance du chien de garde, ou commande répétée : le son tournait déjà. */
    @Test
    fun aSoundAlreadyPlayingIsNotJournaledAgain() {
        assertThat(RingingStartJournal.eventsFor(OperationResult.AlreadySatisfied)).isEmpty()
    }

    /** Aucun son : l'échec seul, sans un `RINGING_STARTED` qui laisserait croire le contraire. */
    @Test
    fun aSoundThatFailsToStartIsJournaledAsAFailureOnly() {
        assertThat(RingingStartJournal.eventsFor(OperationResult.Failure("ANDROID_AUDIO_START_FAILED")))
            .containsExactly(TechnicalEventType.AUDIO_START_FAILED)
    }
}
