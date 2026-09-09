package com.niumi.core.interop

import com.niumi.core.domain.DomainViolation
import com.niumi.core.domain.IncidentEffectPayload
import com.niumi.core.domain.SessionDecision
import com.niumi.core.domain.SessionEffect
import com.niumi.core.domain.SessionEffectPayload

// Mappers des effets et de la décision (SPEC_CORE_KMP §6). Séparés de `DtoMappers.kt` pour rester
// sous le seuil detekt `TooManyFunctions` (voir `ETAPE-08.md`). Aucun `toDomain()` : les effets et
// violations sont produits, jamais réinjectés dans `reduce()` — même convention que
// `BoxPayloadResult.toDto()` sans réciproque, posée à l'étape 2.

internal fun SessionEffectPayload.toDto(): SessionEffectPayloadDto =
    when (this) {
        is IncidentEffectPayload -> IncidentEffectPayloadDto(incident.toDto())
    }

internal fun SessionEffect.toDto(): SessionEffectDto =
    SessionEffectDto(effectId, kind, sessionId, revision, payload?.toDto())

internal fun DomainViolation.toDto(): DomainViolationDto = DomainViolationDto(code, message)

internal fun SessionDecision.toDto(): SessionDecisionDto =
    SessionDecisionDto(
        snapshot = snapshot?.toDto(),
        effects = effects.map { it.toDto() },
        violations = violations.map { it.toDto() },
    )
