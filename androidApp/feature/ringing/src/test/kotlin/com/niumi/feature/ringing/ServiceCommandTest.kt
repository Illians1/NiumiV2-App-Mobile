package com.niumi.feature.ringing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §16 : valider tous les extras reçus par les receivers et services, jamais
 * d'exception. `AlarmReceiver` et `AlarmRingingService` reçoivent leurs extras sous forme d'un
 * [ServiceCommandExtras] pur (Kotlin uniquement, sans `Intent` ni `Bundle`), testable en JVM.
 */
class ServiceCommandTest {
    private val validSessionId = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"

    @Test
    fun validExtrasProduceAValidCommand() {
        val command =
            ServiceCommand.from(ServiceCommandExtras(sessionId = validSessionId, revision = 1L))

        assertThat(command).isEqualTo(ServiceCommand.Valid(validSessionId, 1L))
    }

    @Test
    fun missingSessionIdIsInvalid() {
        val command = ServiceCommand.from(ServiceCommandExtras(sessionId = null, revision = 1L))

        assertThat(command).isInstanceOf(ServiceCommand.Invalid::class.java)
    }

    @Test
    fun nonCanonicalSessionIdIsInvalid() {
        val command =
            ServiceCommand.from(ServiceCommandExtras(sessionId = "not-a-uuid", revision = 1L))

        assertThat(command).isInstanceOf(ServiceCommand.Invalid::class.java)
    }

    @Test
    fun uppercaseSessionIdIsInvalid() {
        val command =
            ServiceCommand.from(
                ServiceCommandExtras(sessionId = validSessionId.uppercase(), revision = 1L),
            )

        assertThat(command).isInstanceOf(ServiceCommand.Invalid::class.java)
    }

    @Test
    fun missingRevisionIsInvalid() {
        val command =
            ServiceCommand.from(ServiceCommandExtras(sessionId = validSessionId, revision = null))

        assertThat(command).isInstanceOf(ServiceCommand.Invalid::class.java)
    }

    @Test
    fun zeroOrNegativeRevisionIsInvalid() {
        val zero =
            ServiceCommand.from(ServiceCommandExtras(sessionId = validSessionId, revision = 0L))
        val negative =
            ServiceCommand.from(ServiceCommandExtras(sessionId = validSessionId, revision = -1L))

        assertThat(zero).isInstanceOf(ServiceCommand.Invalid::class.java)
        assertThat(negative).isInstanceOf(ServiceCommand.Invalid::class.java)
    }
}
