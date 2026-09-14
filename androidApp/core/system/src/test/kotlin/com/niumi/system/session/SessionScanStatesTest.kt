package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionStateDto
import org.junit.Test

/**
 * [SESSION_SCAN_STATES] est la règle partagée entre `SessionReconciler` — qui republie la
 * notification de §10.5 sur ces états — et `SessionReadinessWatcher`, qui décide d'y déclencher une
 * réconciliation au passage au premier plan (étape 19). Deux copies divergeraient.
 */
class SessionScanStatesTest {
    @Test
    fun theScanStatesAreExactlyTheTwoStatesWhereABoxScanIsTheOnlyWayOut() {
        assertThat(SESSION_SCAN_STATES)
            .containsExactly(SessionStateDto.AWAITING_NFC, SessionStateDto.TRIGGERED_AWAITING_NFC)
    }

    /** Une session qui attend un scan est toujours en cours : la notification a donc un objet. */
    @Test
    fun noScanStateIsFinal() {
        assertThat(SESSION_SCAN_STATES.none { it in SESSION_FINAL_STATES }).isTrue()
        assertThat(SESSION_SCAN_STATES.all { it.isSessionInProgress() }).isTrue()
    }

    /**
     * Garde d'exhaustivité : une valeur ajoutée à `SessionStateDto` sans être classée ici ferait
     * échouer ce test plutôt que de passer silencieusement dans la catégorie « ni scan ni final ».
     */
    @Test
    fun everyStateIsAccountedFor() {
        val others =
            setOf(
                SessionStateDto.PREPARING,
                SessionStateDto.ARMED,
                SessionStateDto.RINGING,
                SessionStateDto.RELEASING,
            )

        assertThat(SESSION_SCAN_STATES + SESSION_FINAL_STATES + others)
            .containsExactlyElementsIn(SessionStateDto.entries)
    }
}
