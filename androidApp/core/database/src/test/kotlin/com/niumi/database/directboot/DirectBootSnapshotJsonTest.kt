package com.niumi.database.directboot

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.ReleaseTarget
import com.niumi.core.domain.SessionEffectKind
import com.niumi.core.domain.SessionHealth
import com.niumi.core.domain.SessionState
import com.niumi.core.interop.isBlockingPending
import com.niumi.database.EffectStatus
import com.niumi.database.SessionBox
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
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
                "blockingLocalDate",
                "blockingLocalTime",
                "blockingStartsAtEpochMillis",
                "blockingAppliedAtEpochMillis",
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
                "volumeRampSeconds",
                "boxId",
                "boxTokenSha256Hex",
                "boxes",
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

    /**
     * Lot 7, SPEC_ANDROID §7.3 : un fichier écrit en v2 — sans `volumeRampSeconds`, avec la sonnerie
     * du MVP — se lit en volume constant et sur la sonnerie qui la remplace. Et surtout, un blocage
     * différé encore en attente y reste en attente : la v2 porte déjà les champs `blocking*`, et la
     * relire comme un fichier v1 lèverait le blocage de la session.
     */
    @Test
    fun `a version two file stays readable, pending blocking included`() {
        val versionTwo =
            reference.copy(
                projectionSchemaVersion = 2,
                blockingLocalDate = "2026-09-07",
                blockingLocalTime = "22:30",
                blockingStartsAtEpochMillis = 1_750_000_000_000L,
                blockingAppliedAtEpochMillis = null,
                ringtoneKey = "niumi_alarm",
            )
        val encoded = json.encodeToJsonElement(DirectBootSnapshot.Active.serializer(), versionTwo).jsonObject
        val file = JsonObject(encoded - "volumeRampSeconds").toString()

        val decoded = json.decodeFromString(DirectBootSnapshot.Active.serializer(), file)

        assertThat(decoded.toExtras().volumeRampSeconds).isNull()
        assertThat(decoded.toExtras().ringtoneKey).isEqualTo("niumi_piano")
        assertThat(decoded.toSnapshotDto().blockingSchedule.startsAtEpochMillis).isEqualTo(1_750_000_000_000L)
        assertThat(decoded.toSnapshotDto().isBlockingPending).isTrue()
    }

    @Test
    fun `a version three file keeps its ramp and ringtone`() {
        val versionThree =
            reference.copy(projectionSchemaVersion = 3, ringtoneKey = "niumi_oiseaux", volumeRampSeconds = 300)

        val decoded =
            json.decodeFromString(
                DirectBootSnapshot.Active.serializer(),
                json.encodeToString(DirectBootSnapshot.Active.serializer(), versionThree),
            )

        assertThat(decoded.projectionSchemaVersion).isEqualTo(3)
        assertThat(decoded.toExtras().ringtoneKey).isEqualTo("niumi_oiseaux")
        assertThat(decoded.toExtras().volumeRampSeconds).isEqualTo(300)
    }

    /**
     * Lot 8, SPEC_ANDROID §7.3 : un vrai fichier v3 — écrit par le code de l'étape 28, avant ce
     * lot, ajouté en ressource de test (`directboot/projection-v3.json`) — se lit comme une liste
     * d'un seul boîtier, sans nom.
     */
    @Test
    fun `a real version three file reads as a list of one box`() {
        val file =
            checkNotNull(javaClass.getResourceAsStream("/directboot/projection-v3.json")) {
                "ressource de test manquante : directboot/projection-v3.json"
            }.bufferedReader().readText()

        val decoded = json.decodeFromString(DirectBootSnapshot.Active.serializer(), file)

        assertThat(decoded.projectionSchemaVersion).isEqualTo(3)
        assertThat(decoded.toExtras().boxes)
            .containsExactly(SessionBox("550e8400-e29b-41d4-a716-446655440000", "a".repeat(64), name = ""))
    }

    /** Lot 8, SPEC_ANDROID §7.3 : l'écriture courante (v4) porte la liste complète, sans les noms. */
    @Test
    fun `a version four file round trips its boxes`() {
        val twoBoxes =
            listOf(
                DirectBootBox("11111111-1111-1111-1111-111111111111", "a".repeat(64)),
                DirectBootBox("22222222-2222-2222-2222-222222222222", "b".repeat(64)),
            )
        val versionFour = reference.copy(boxes = twoBoxes)

        val decoded =
            json.decodeFromString(
                DirectBootSnapshot.Active.serializer(),
                json.encodeToString(DirectBootSnapshot.Active.serializer(), versionFour),
            )

        assertThat(decoded.projectionSchemaVersion).isEqualTo(4)
        assertThat(decoded.boxes).isEqualTo(twoBoxes)
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectKeys(): Set<String> = (this as JsonObject).keys
}
