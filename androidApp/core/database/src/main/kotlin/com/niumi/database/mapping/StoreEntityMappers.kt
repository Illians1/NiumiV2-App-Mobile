package com.niumi.database.mapping

import com.niumi.database.BlockedPackage
import com.niumi.database.EventReceipt
import com.niumi.database.PendingEffect
import com.niumi.database.entity.BlockedAppEntity
import com.niumi.database.entity.SessionEffectOutboxEntity
import com.niumi.database.entity.SessionEventReceiptEntity

fun BlockedPackage.toEntity(sessionId: String): BlockedAppEntity =
    BlockedAppEntity(sessionId = sessionId, packageName = packageName, displayNameSnapshot = displayNameSnapshot)

fun BlockedAppEntity.toBlockedPackage(): BlockedPackage = BlockedPackage(packageName, displayNameSnapshot)

fun EventReceipt.toEntity(): SessionEventReceiptEntity =
    SessionEventReceiptEntity(
        eventId = eventId,
        sessionId = sessionId,
        payloadSha256Hex = payloadSha256Hex,
        appliedRevision = appliedRevision,
        receivedAtEpochMillis = receivedAtEpochMillis,
    )

fun SessionEventReceiptEntity.toDomain(): EventReceipt =
    EventReceipt(eventId, sessionId, payloadSha256Hex, appliedRevision, receivedAtEpochMillis)

/** [updatedAtEpochMillis] initial : l'horodatage de réception du reçu, aucune horloge requise. */
fun PendingEffect.toEntity(updatedAtEpochMillis: Long): SessionEffectOutboxEntity =
    SessionEffectOutboxEntity(
        effectId = effectId,
        sessionId = sessionId,
        revision = revision,
        kind = kind,
        ordinal = ordinal,
        payloadJson = payloadJson,
        status = status,
        lastError = lastError,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

fun SessionEffectOutboxEntity.toPendingEffect(): PendingEffect =
    PendingEffect(effectId, sessionId, revision, kind, ordinal, payloadJson, status, lastError)
