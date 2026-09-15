package com.niumi.database.logging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * SPEC_ANDROID §17 : 200 événements maximum, la plus ancienne supprimée au-delà ; le nom de
 * package n'est accepté que pour `BLOCK_APPLIED` (filtre porté par [TechnicalEventDetails]) ;
 * chaque événement porte le modèle de l'appareil, la version Android et la version de
 * l'application.
 */
class InMemoryTechnicalEventLogTest {
    private val deviceContext =
        DeviceContext(deviceModel = "Pixel Test", androidVersion = "16 (API 36)", appVersion = "1.0.0 (1)")
    private val log = InMemoryTechnicalEventLog(deviceContext, nowEpochMillis = { 42L })

    @Test
    fun keeps200MostRecentEntriesAndDropsTheOldest() =
        runTest {
            repeat(201) { index ->
                log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "session-$index")
            }

            val entries = log.recent()

            assertThat(entries).hasSize(200)
            assertThat(entries.first().sessionId).isEqualTo("session-1")
            assertThat(entries.last().sessionId).isEqualTo("session-200")
        }

    @Test
    fun packageNameIsKeptForBlockApplied() =
        runTest {
            log.log(
                TechnicalEventType.BLOCK_APPLIED,
                sessionId = "s1",
                detailsJson = TechnicalEventDetails.packageName("com.example.app"),
            )

            assertThat(log.recent().single().detailsJson)
                .isEqualTo(TechnicalEventDetails.packageName("com.example.app"))
        }

    @Test
    fun packageNameIsDroppedForAnyOtherType() =
        runTest {
            log.log(
                TechnicalEventType.ALARM_RECEIVED,
                sessionId = "s1",
                detailsJson = TechnicalEventDetails.packageName("com.example.app"),
            )

            assertThat(log.recent().single().detailsJson).isNull()
        }

    @Test
    fun entryCarriesTheInjectedTimestamp() =
        runTest {
            log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "s1")

            assertThat(log.recent().single().occurredAtEpochMillis).isEqualTo(42L)
        }

    // §17 : « Chaque événement contient seulement l'heure, le type, l'identifiant de session, le
    // modèle de l'appareil, la version Android, la version de l'application et un code d'erreur
    // contrôlé. » Le contexte est ajouté par l'implémentation, jamais par l'appelant : la
    // signature de `log()` ne l'expose pas.
    @Test
    fun entryCarriesTheDeviceContext() =
        runTest {
            log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "s1")

            val entry = log.recent().single()
            assertThat(entry.deviceModel).isEqualTo("Pixel Test")
            assertThat(entry.androidVersion).isEqualTo("16 (API 36)")
            assertThat(entry.appVersion).isEqualTo("1.0.0 (1)")
        }

    /** Étape 20 : le versement dans Room au déverrouillage doit voir tout ce qui a été écrit. */
    @Test
    fun drainReturnsEverything() {
        log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "s1")
        log.log(TechnicalEventType.RINGING_STARTED, sessionId = "s1")

        val drained = log.drain()

        assertThat(drained.map { it.type }).containsExactly(
            TechnicalEventType.ALARM_RECEIVED,
            TechnicalEventType.RINGING_STARTED,
        )
    }

    /** Vidange atomique : jamais rejouée deux fois, sans quoi le versement dans Room dupliquerait. */
    @Test
    fun drainEmptiesTheLogAtomically() =
        runTest {
            log.log(TechnicalEventType.ALARM_RECEIVED, sessionId = "s1")

            log.drain()

            assertThat(log.drain()).isEmpty()
            assertThat(log.recent()).isEmpty()
        }
}
