package com.niumi.database.directboot

import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.EffectStatus
import kotlinx.serialization.Serializable

/** Version du format de projection lui-même, indépendante de [DirectBootSnapshot.Active.domainSchemaVersion]. */
public const val DIRECT_BOOT_PROJECTION_SCHEMA_VERSION: Int = 1

/**
 * Projection partielle de Room dans le stockage protégé de l'appareil (SPEC_ANDROID §7.3,
 * SPEC_CORE_KMP §13). `Active` contient tous les champs requis pour reconstruire un
 * `SessionSnapshotDto` et appeler `NiumiCoreFacade.reduce` avant déverrouillage.
 *
 * Les champs sont des types de projection dédiés ([DirectBootBlockedPackage],
 * [DirectBootReceipt], [DirectBootEffect]), pas les types de production (`BlockedPackage`,
 * `EventReceipt`, `PendingEffect`) : le format persisté ne doit pas dériver avec la forme interne
 * de ces classes, même motif que le `@SerialName` figé sur `IncidentEffectPayloadDto` à
 * l'étape 9 (`ETAPE-09.md`, décision 3) et « les formats physiques restent natifs »
 * (SPEC_CORE_KMP §13).
 *
 * `WakeScheduleDto` est aplati en quatre champs (`localDate`, `localTime`, `zoneIdAtActivation`,
 * `triggerAtEpochMillis`), même convention que `AlarmSessionEntity` (voir `SessionSnapshotMapper`).
 * Les enums (`SessionStateDto`, `ReleaseTargetDto`, `SessionHealthDto`, `SessionEffectKindDto`,
 * `EffectStatus`) sont sérialisés par leur nom : comportement natif de kotlinx-serialization pour
 * un `enum class`, aucune annotation requise (déjà exploité par `EventFingerprint` à l'étape 9).
 *
 * Seul [Active] est jamais sérialisé (directement, via son propre sérialiseur) : [Corrupted]
 * représente un état de lecture, jamais écrit. L'interface scellée n'est donc pas elle-même
 * `@Serializable` — cela imposerait à [Corrupted] de l'être aussi pour la sérialisation
 * polymorphe, sans aucun appelant qui en ait besoin.
 */
public sealed interface DirectBootSnapshot {
    @Serializable
    public data class Active(
        val projectionSchemaVersion: Int = DIRECT_BOOT_PROJECTION_SCHEMA_VERSION,
        val domainSchemaVersion: Int,
        val domainRevision: Long,
        val sessionId: String,
        val localDate: String,
        val localTime: String,
        val zoneIdAtActivation: String,
        val triggerAtEpochMillis: Long,
        val state: SessionStateDto,
        val releaseTarget: ReleaseTargetDto?,
        val health: SessionHealthDto,
        val createdAtEpochMillis: Long,
        val armedAtEpochMillis: Long?,
        val ringingAtEpochMillis: Long?,
        val alarmSoundStoppedAtEpochMillis: Long?,
        val triggerElapsedAtEpochMillis: Long?,
        val nfcVerifiedAtEpochMillis: Long?,
        val releasingAtEpochMillis: Long?,
        val completedAtEpochMillis: Long?,
        val cancelledAtEpochMillis: Long?,
        val failureCode: String?,
        val ringtoneKey: String,
        val vibrationEnabled: Boolean,
        val boxId: String,
        val boxTokenSha256Hex: String,
        val blockedPackages: List<DirectBootBlockedPackage>,
        val eventReceipts: List<DirectBootReceipt>,
        val pendingEffects: List<DirectBootEffect>,
    ) : DirectBootSnapshot

    /** Fichier absent ou illisible (SPEC_CORE_KMP §13 : corruption traitée explicitement). */
    public data class Corrupted(
        val reason: String,
    ) : DirectBootSnapshot
}

@Serializable
public data class DirectBootBlockedPackage(
    val packageName: String,
    val displayNameSnapshot: String,
)

@Serializable
public data class DirectBootReceipt(
    val eventId: String,
    val sessionId: String,
    val payloadSha256Hex: String,
    val appliedRevision: Long,
    val receivedAtEpochMillis: Long,
)

@Serializable
public data class DirectBootEffect(
    val effectId: String,
    val sessionId: String,
    val revision: Long,
    val kind: SessionEffectKindDto,
    val ordinal: Int,
    val payloadJson: String?,
    val status: EffectStatus,
    val lastError: String?,
)
