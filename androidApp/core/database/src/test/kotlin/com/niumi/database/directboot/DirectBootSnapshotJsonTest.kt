package com.niumi.database.directboot

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.ReleaseMeans
import com.niumi.core.domain.ReleaseTarget
import com.niumi.core.domain.SessionEffectKind
import com.niumi.core.domain.SessionHealth
import com.niumi.core.domain.SessionState
import com.niumi.core.interop.isBlockingPending
import com.niumi.database.EffectStatus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
            releasedBy = ReleaseMeans.EMERGENCY_EXIT,
            disarmedAtEpochMillis = 1_700_000_003_000L,
            ringtoneKey = "niumi_default",
            vibrationEnabled = true,
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
                "releasedBy",
                "disarmedAtEpochMillis",
                "ringtoneKey",
                "vibrationEnabled",
                "volumeRampSeconds",
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
     * Étape 43, SPEC_ANDROID §7.3 : un vrai fichier v3 — écrit par le code de l'étape 28, ajouté en
     * ressource de test (`directboot/projection-v3.json`) — reste lisible malgré ses clés `boxId` et
     * `boxTokenSha256Hex`, que la projection ne porte plus. Ni moyen de libération ni désarmement.
     */
    @Test
    fun `a real version three file stays readable without its box fields`() {
        val file =
            checkNotNull(javaClass.getResourceAsStream("/directboot/projection-v3.json")) {
                "ressource de test manquante : directboot/projection-v3.json"
            }.bufferedReader().readText()

        val decoded = json.decodeFromString(DirectBootSnapshot.Active.serializer(), file)

        assertThat(decoded.projectionSchemaVersion).isEqualTo(3)
        assertThat(decoded.sessionId).isEqualTo("11111111-1111-1111-1111-111111111111")
        assertThat(decoded.releasedBy).isNull()
        assertThat(decoded.disarmedAtEpochMillis).isNull()
    }

    /**
     * Étape 43, SPEC_ANDROID §7.3 : un fichier v4 — liste de Niumi Spots figée, sans `releasedBy` ni
     * `disarmedAtEpochMillis` — se lit, la liste ignorée et les deux champs nuls ; la façade en
     * déduit le moyen de libération (SPEC_CORE_KMP §7.1).
     */
    @Test
    fun `a version four file is read with its boxes ignored`() {
        val encoded = json.encodeToJsonElement(DirectBootSnapshot.Active.serializer(), reference).jsonObject
        val boxes =
            JsonArray(
                listOf(
                    JsonObject(
                        mapOf(
                            "boxId" to JsonPrimitive("550e8400-e29b-41d4-a716-446655440000"),
                            "tokenSha256Hex" to JsonPrimitive("a".repeat(64)),
                        ),
                    ),
                ),
            )
        val file =
            JsonObject(
                encoded - "releasedBy" - "disarmedAtEpochMillis" +
                    ("projectionSchemaVersion" to JsonPrimitive(4)) +
                    ("boxes" to boxes),
            ).toString()

        val decoded = json.decodeFromString(DirectBootSnapshot.Active.serializer(), file)

        assertThat(decoded.projectionSchemaVersion).isEqualTo(4)
        assertThat(decoded.releasedBy).isNull()
        assertThat(decoded.disarmedAtEpochMillis).isNull()
        assertThat(
            decoded.copy(
                projectionSchemaVersion = 5,
                releasedBy = reference.releasedBy,
                disarmedAtEpochMillis = reference.disarmedAtEpochMillis,
            ),
        ).isEqualTo(reference)
    }

    /** Étape 43 : l'écriture courante est en v5 et porte le moyen de libération et le désarmement. */
    @Test
    fun `the current projection is version five and round trips the release means`() {
        val decoded =
            json.decodeFromString(
                DirectBootSnapshot.Active.serializer(),
                json.encodeToString(DirectBootSnapshot.Active.serializer(), reference),
            )

        assertThat(DIRECT_BOOT_PROJECTION_SCHEMA_VERSION).isEqualTo(PROJECTION_VERSION_WITH_RELEASE_MEANS)
        assertThat(decoded.projectionSchemaVersion).isEqualTo(5)
        assertThat(decoded.releasedBy).isEqualTo(ReleaseMeans.EMERGENCY_EXIT)
        assertThat(decoded.disarmedAtEpochMillis).isEqualTo(1_700_000_003_000L)
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectKeys(): Set<String> = (this as JsonObject).keys
}
