package com.niumi.database.mapping

import com.niumi.database.SessionBox
import com.niumi.database.entity.SessionBoxEntity

/** Aller-retour `SessionBox ↔ SessionBoxEntity` (SPEC_ANDROID §7.2, Lot 8). */
fun SessionBox.toEntity(
    sessionId: String,
    position: Int,
): SessionBoxEntity =
    SessionBoxEntity(
        sessionId = sessionId,
        boxId = boxId,
        tokenSha256 = tokenSha256Hex,
        name = name,
        position = position,
    )

fun SessionBoxEntity.toDomain(): SessionBox =
    SessionBox(
        boxId = boxId,
        tokenSha256Hex = tokenSha256,
        name = name,
    )
