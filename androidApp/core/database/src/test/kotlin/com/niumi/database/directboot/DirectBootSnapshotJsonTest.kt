package com.niumi.database.directboot

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.ReleaseTarget
import com.niumi.core.domain.SessionEffectKind
import com.niumi.core.domain.SessionHealth
import com.niumi.core.domain.SessionState
import com.niumi.database.EffectStatus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Format JSON persisté du snapshot Direct Boot (SPEC_ANDROID §7.3). Un doré épinglé sur
 * [reference] attrape tout changement silencieux de forme : un tel changement romprait la lecture
 * d'un fichier déjà écrit par une version antérieure de l'application, avant que
 * `SessionReconciler` (étape 11) n'ait pu réconcilier depuis Room.
 */
class DirectBootSnapshotJsonTest {
    private val json = directBootJson

    private val reference =
        DirectBootSnapshot.Active(
            domainSchemaVersion = 1,
            domainRevision = 3,
            sessionId = "11111111-1111-1111-1111-111111111111",
            localDate = "2026-09-08",
            localTime = "07:00",
            zoneIdAtActivation = "Europe/Paris",
            triggerAtEpochMillis = 1_800_000_000_000L,
            state = SessionState.ARMED,
            releaseTarget = ReleaseTarget.COMPLETED,
            health = SessionHealth.HEALTHY,
            createdAtEpochMillis = 1_700_000_000_000L,
            armedAtEpochMillis = 1_700_000_001_000L,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
            ringtoneKey = "niumi_default",
            vibrationEnabled = true,
            boxId = "550e8400-e29b-41d4-a716-446655440000",
            boxTokenSha256Hex = "a".repeat(64),
            blockedPackages = listOf(DirectBootBlockedPackage("com.example.first", "Première application")),
            eventReceipts =
                listOf(
                    DirectBootReceipt(
                        eventId = "22222222-2222-2222-2222-222222222222",
                        sessionId = "11111111-1111-1111-1111-111111111111",
                        payloadSha256Hex = "b".repeat(64),
                        appliedRevision = 3,
                        receivedAtEpochMillis = 1_700_000_002_000L,
                    ),
                ),
            pendingEffects =
                listOf(
                    DirectBootEffect(
                        effectId = "11111111-1111-1111-1111-111111111111:3:SCHEDULE_ALARM:0",
                        sessionId = "11111111-1111-1111-1111-111111111111",
                        revision = 3,
                        kind = SessionEffectKind.SCHEDULE_ALARM,
                        ordinal = 0,
                        payloadJson = null,
                        status = EffectStatus.SUCCEEDED,
                        lastError = null,
                    ),
                ),
        )

    @Test
    fun `round trip preserves every field`() {
        val encoded = json.encodeToString(DirectBootSnapshot.Active.serializer(), reference)
        val decoded = json.decodeFromString(DirectBootSnapshot.Active.serializer(), encoded)

        assertThat(decoded).isEqualTo(reference)
    }

    @Test
    fun `golden json contains exactly the fields imposed by SPEC_ANDROID paragraph 7-3`() {
        val encoded = json.parseToJsonElement(json.encodeToString(DirectBootSnapshot.Active.serializer(), reference))
        val keys = encoded.jsonObjectKeys()

        assertThat(keys)
            .containsExactly(
                "projectionSchemaVersion",
                "domainSchemaVersion",
                "domainRevision",
                "sessionId",
                "localDate",
                "localTime",
                "zoneIdAtActivation",
                "triggerAtEpochMillis",
                "state",
                "releaseTarget",
                "health",
                "createdAtEpochMillis",
                "armedAtEpochMillis",
                "ringingAtEpochMillis",
                "alarmSoundStoppedAtEpochMillis",
                "triggerElapsedAtEpochMillis",
                "nfcVerifiedAtEpochMillis",
                "releasingAtEpochMillis",
                "completedAtEpochMillis",
                "cancelledAtEpochMillis",
                "failureCode",
                "ringtoneKey",
                "vibrationEnabled",
                "boxId",
                "boxTokenSha256Hex",
                "blockedPackages",
                "eventReceipts",
                "pendingEffects",
            )
    }

    @Test
    fun `enums are encoded by name, not ordinal`() {
        val encoded = json.encodeToString(DirectBootSnapshot.Active.serializer(), reference)

        assertThat(encoded).contains("\"state\":\"ARMED\"")
        assertThat(encoded).contains("\"releaseTarget\":\"COMPLETED\"")
        assertThat(encoded).contains("\"health\":\"HEALTHY\"")
    }

    @Test
    fun `unknown field is ignored`() {
        val encoded = json.encodeToString(DirectBootSnapshot.Active.serializer(), reference)
        val withExtraField = encoded.dropLast(1) + ""","futureField":"anything"}"""

        val decoded = json.decodeFromString(DirectBootSnapshot.Active.serializer(), withExtraField)

        assertThat(decoded).isEqualTo(reference)
    }

    @Test
    fun `truncated json fails to decode`() {
        val encoded = json.encodeToString(DirectBootSnapshot.Active.serializer(), reference)
        val truncated = encoded.dropLast(20)

        assertThrows(SerializationException::class.java) {
            json.decodeFromString(DirectBootSnapshot.Active.serializer(), truncated)
        }
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectKeys(): Set<String> = (this as JsonObject).keys
}
