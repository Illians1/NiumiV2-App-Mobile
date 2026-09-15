package com.niumi.system.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** SPEC_ANDROID §18, §20 : état minimal, lu par l'accueil et l'écran de diagnostic. */
class StorageIntegrityStateTest {
    @Test
    fun startsReadable() {
        assertThat(StorageIntegrityState().failure.value).isNull()
    }

    @Test
    fun aSuccessfulLoadClearsAPreviousFailure() {
        val state = StorageIntegrityState()
        state.reportUnreadable("SQLITE_CORRUPT")

        state.reportReadable()

        assertThat(state.failure.value).isNull()
    }

    @Test
    fun theReasonIsKeptUntilAReadableLoad() {
        val state = StorageIntegrityState()

        state.reportUnreadable("SQLITE_CORRUPT")

        assertThat(state.failure.value).isEqualTo("SQLITE_CORRUPT")
    }
}
