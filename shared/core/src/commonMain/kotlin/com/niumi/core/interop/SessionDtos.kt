package com.niumi.core.interop

import com.niumi.core.domain.IncidentSeverity
import com.niumi.core.domain.Platform
import com.niumi.core.domain.ReleaseTarget
import com.niumi.core.domain.SessionEffectKind
import com.niumi.core.domain.SessionEventKind
import com.niumi.core.domain.SessionHealth
import com.niumi.core.domain.SessionState
import com.niumi.core.nfc.NfcVerificationProof
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Miroirs DTO des types de SPEC_CORE_KMP §5 à §7 (§14 : « DTO concrets, sans type de plateforme,
 * enums stables »). Les enums du domaine sont ré-exportés par `typealias` plutôt que dupliqués :
 * ce sont déjà des énumérations stables ([SessionState], [SessionEventKind]…) et un mapping manuel
 * n'isolerait rien (le typealias est effacé à la compilation), au prix de sept enums recopiés et
 * de leurs conversions. Décision validée avec l'utilisateur le 2026-09-08, voir `ETAPE-08.md`.
 * Convention héritée de l'étape 2 (`BoxPayloadStatus`, `BoxVerificationStatus` déjà réutilisés
 * tels quels dans `NfcDtos.kt`), étendue ici aux enums du moteur.
 */
public typealias SessionStateDto = SessionState

public typealias SessionEventKindDto = SessionEventKind

public typealias SessionEffectKindDto = SessionEffectKind

public typealias SessionHealthDto = SessionHealth

public typealias ReleaseTargetDto = ReleaseTarget

public typealias PlatformDto = Platform

public typealias IncidentSeverityDto = IncidentSeverity

@Serializable
public data class WakeScheduleDto(
    val localDateIso: String,
    val localTimeIso: String,
    val zoneIdAtActivation: String,
    val triggerAtEpochMillis: Long,
)

@Serializable
public data class AppSelectionSummaryDto(
    val count: Int,
)

@Serializable
public data class ActivationRequestDto(
    val wakeSchedule: WakeScheduleDto,
    val appSelection: AppSelectionSummaryDto,
)

@Serializable
public data class SessionIncidentDto(
    val code: String,
    val severity: IncidentSeverityDto,
    val occurredAtEpochMillis: Long,
    val platform: PlatformDto,
)

@Serializable
public data class SessionSnapshotDto(
    val schemaVersion: Int,
    val revision: Long,
    val sessionId: String,
    val wakeSchedule: WakeScheduleDto,
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
)

/**
 * [nfcProof] traverse par référence, jamais sérialisé (SPEC_CORE_KMP §14 ; point de vigilance 4 du
 * plan) : [NfcVerificationProof] n'a ni constructeur public ni forme sérialisable. `@Transient`
 * exige une valeur par défaut : `null` ici, jamais utilisée en dehors d'une (dé)sérialisation
 * explicite de ce DTO, qui ne fait pas partie de son usage normal à la frontière `interop`.
 */
@Serializable
public data class SessionEventDto(
    val eventId: String,
    val sessionId: String,
    val kind: SessionEventKindDto,
    val occurredAtEpochMillis: Long,
    val expectedRevision: Long?,
    val activationRequest: ActivationRequestDto?,
    @Transient val nfcProof: NfcVerificationProof? = null,
    val failureCode: String?,
    val incident: SessionIncidentDto?,
)

/** Charge d'effet (SPEC_CORE_KMP §6), miroir de [com.niumi.core.domain.SessionEffectPayload]. */
@Serializable
public sealed interface SessionEffectPayloadDto

@Serializable
public data class IncidentEffectPayloadDto(
    val incident: SessionIncidentDto,
) : SessionEffectPayloadDto

@Serializable
public data class SessionEffectDto(
    val effectId: String,
    val kind: SessionEffectKindDto,
    val sessionId: String,
    val revision: Long,
    val payload: SessionEffectPayloadDto?,
)

@Serializable
public data class DomainViolationDto(
    val code: String,
    val message: String,
)

@Serializable
public data class SessionDecisionDto(
    val snapshot: SessionSnapshotDto?,
    val effects: List<SessionEffectDto>,
    val violations: List<DomainViolationDto>,
)
