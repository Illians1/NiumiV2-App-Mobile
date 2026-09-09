package com.niumi.database.converter

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EffectStatus
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Chaque convertisseur Room stocke un enum par son `name` (jamais son `ordinal`, qui ne survivrait
 * pas à l'ajout d'une constante) et fait l'aller-retour pour chaque constante existante ; un nom
 * inconnu en base est une corruption qui doit remonter explicitement (SPEC_CORE_KMP §13).
 */
class EnumConvertersTest {
    private val sessionConverters = SessionEnumConverters()
    private val effectConverters = EffectEnumConverters()
    private val incidentConverters = IncidentEnumConverters()

    @Test
    fun sessionStateRoundTripsForEveryConstant() {
        SessionStateDto.entries.forEach { state ->
            val stored = sessionConverters.fromSessionState(state)
            assertThat(stored).isEqualTo(state.name)
            assertThat(sessionConverters.toSessionState(stored)).isEqualTo(state)
        }
    }

    @Test
    fun releaseTargetRoundTripsIncludingNull() {
        assertThat(sessionConverters.toReleaseTarget(null)).isNull()
        assertThat(sessionConverters.fromReleaseTarget(null)).isNull()

        ReleaseTargetDto.entries.forEach { target ->
            val stored = sessionConverters.fromReleaseTarget(target)
            assertThat(stored).isEqualTo(target.name)
            assertThat(sessionConverters.toReleaseTarget(stored)).isEqualTo(target)
        }
    }

    @Test
    fun sessionHealthRoundTripsForEveryConstant() {
        SessionHealthDto.entries.forEach { health ->
            val stored = sessionConverters.fromSessionHealth(health)
            assertThat(stored).isEqualTo(health.name)
            assertThat(sessionConverters.toSessionHealth(stored)).isEqualTo(health)
        }
    }

    @Test
    fun unknownSessionStateNameIsNotSilentlySwallowed() {
        assertThrows(IllegalArgumentException::class.java) { sessionConverters.toSessionState("NOT_A_STATE") }
    }

    @Test
    fun sessionEffectKindRoundTripsForEveryConstant() {
        SessionEffectKindDto.entries.forEach { kind ->
            val stored = effectConverters.fromSessionEffectKind(kind)
            assertThat(stored).isEqualTo(kind.name)
            assertThat(effectConverters.toSessionEffectKind(stored)).isEqualTo(kind)
        }
    }

    @Test
    fun effectStatusRoundTripsForEveryConstant() {
        EffectStatus.entries.forEach { status ->
            val stored = effectConverters.fromEffectStatus(status)
            assertThat(stored).isEqualTo(status.name)
            assertThat(effectConverters.toEffectStatus(stored)).isEqualTo(status)
        }
    }

    @Test
    fun unknownEffectKindNameIsNotSilentlySwallowed() {
        assertThrows(IllegalArgumentException::class.java) { effectConverters.toSessionEffectKind("NOT_A_KIND") }
    }

    @Test
    fun incidentSeverityRoundTripsForEveryConstant() {
        IncidentSeverityDto.entries.forEach { severity ->
            val stored = incidentConverters.fromIncidentSeverity(severity)
            assertThat(stored).isEqualTo(severity.name)
            assertThat(incidentConverters.toIncidentSeverity(stored)).isEqualTo(severity)
        }
    }

    @Test
    fun platformRoundTripsForEveryConstant() {
        PlatformDto.entries.forEach { platform ->
            val stored = incidentConverters.fromPlatform(platform)
            assertThat(stored).isEqualTo(platform.name)
            assertThat(incidentConverters.toPlatform(stored)).isEqualTo(platform)
        }
    }

    @Test
    fun unknownIncidentSeverityNameIsNotSilentlySwallowed() {
        assertThrows(IllegalArgumentException::class.java) {
            incidentConverters.toIncidentSeverity("NOT_A_SEVERITY")
        }
    }
}
