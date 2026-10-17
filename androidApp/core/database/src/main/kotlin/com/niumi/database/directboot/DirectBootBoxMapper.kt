package com.niumi.database.directboot

import com.niumi.database.SessionBox

/**
 * Aller-retour `SessionBox ↔ DirectBootBox` (SPEC_ANDROID §7.3, Lot 8), à part de
 * `DirectBootMapper.kt` : ce fichier approche le plafond detekt `TooManyFunctions` (11).
 */
internal fun SessionBox.toProjection(): DirectBootBox = DirectBootBox(boxId, tokenSha256Hex)

internal fun DirectBootBox.toDomain(): SessionBox = SessionBox(boxId, tokenSha256Hex, name = "")

/**
 * Reconstruction d'un fichier antérieur à la v4 (Lot 8) : un seul boîtier, sans nom, depuis les
 * deux anciens champs à plat. Une liste vide (`boxId` jamais vide en pratique sur un fichier
 * réellement écrit par une version antérieure) n'est pas un cas traité ici : `DirectBootMapper`
 * n'a jamais écrit un `boxId` vide.
 */
internal fun DirectBootSnapshot.Active.legacyBox(): SessionBox = SessionBox(boxId, boxTokenSha256Hex, name = "")
