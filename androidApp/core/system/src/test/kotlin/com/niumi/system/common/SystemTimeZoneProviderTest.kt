package com.niumi.system.common

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.WakeScheduleInputDto
import com.niumi.core.interop.WakeScheduleStatusDto
import org.junit.Test
import java.time.ZoneId

/**
 * L'identifiant renvoyé doit être un fuseau IANA valide pour deux consommateurs distincts :
 * la JVM (`ZoneId.of`, utilisé par les adaptateurs natifs, SPEC_ANDROID §8) et `:shared:core`
 * (`kotlinx-datetime`, via `NiumiCoreFacade.computeWakeSchedule`, SPEC_CORE_KMP §8.1). Un simple
 * `assertThat(id).isNotEmpty()` ne prouverait pas cette compatibilité réelle entre les deux piles.
 */
class SystemTimeZoneProviderTest {
    private val provider: TimeZoneProvider = SystemTimeZoneProvider()

    @Test
    fun currentZoneIdIsAcceptedByJavaTime() {
        val zoneId = provider.currentZoneId()

        // Ne doit lever aucune exception.
        ZoneId.of(zoneId)
    }

    @Test
    fun currentZoneIdIsAcceptedByTheSharedFacade() {
        val zoneId = provider.currentZoneId()
        val facade = NiumiCoreFacade()

        val result =
            facade.computeWakeSchedule(
                WakeScheduleInputDto(
                    localTimeIso = "07:00",
                    zoneId = zoneId,
                    nowEpochMillis = 0L,
                ),
            )

        assertThat(result.status).isNotEqualTo(WakeScheduleStatusDto.UNKNOWN_ZONE)
    }
}
