package com.niumi.system.blocking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Le service d'accessibilité annonce lui-même sa liaison dans ce processus (SPEC_ANDROID §13.1,
 * décision du 2026-10-15) : faux tant qu'`onServiceConnected` n'a pas eu lieu, donc faux dans tout
 * processus neuf — y compris celui qu'Android relance après une mort du processus, quand HyperOS ne
 * relie plus le service.
 */
class InMemoryAccessibilityServiceBindingTest {
    private val binding = InMemoryAccessibilityServiceBinding()

    @Test
    fun isNotBoundByDefault() {
        assertThat(binding.isBound).isFalse()
    }

    @Test
    fun isBoundAfterMarkConnected() {
        binding.markConnected()

        assertThat(binding.isBound).isTrue()
    }

    @Test
    fun isNotBoundAfterMarkDisconnected() {
        binding.markConnected()

        binding.markDisconnected()

        assertThat(binding.isBound).isFalse()
    }

    /** `onServiceConnected` peut être rappelé à chaque reconnexion. */
    @Test
    fun markConnectedIsIdempotent() {
        binding.markConnected()
        binding.markConnected()

        assertThat(binding.isBound).isTrue()
    }
}
