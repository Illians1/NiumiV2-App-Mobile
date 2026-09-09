package com.niumi.database.logging

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Construit et filtre le contenu de `detailsJson` (SPEC_ANDROID §16, §17). Le nom de package
 * n'est autorisé que pour [TechnicalEventType.BLOCK_APPLIED] ; tout autre type ne peut porter
 * qu'un code d'erreur contrôlé. Toute autre clé — texte d'accessibilité, hash de token,
 * identifiant matériel — est supprimée avant écriture, quel que soit ce qu'un appelant a
 * construit : le garde-fou vit ici, en un seul point, plutôt que dans la discipline de chaque
 * appelant.
 */
object TechnicalEventDetails {
    private const val PACKAGE_NAME_KEY = "packageName"
    private const val ERROR_CODE_KEY = "errorCode"

    fun packageName(value: String): String = buildJsonObject { put(PACKAGE_NAME_KEY, JsonPrimitive(value)) }.toString()

    fun errorCode(value: String): String = buildJsonObject { put(ERROR_CODE_KEY, JsonPrimitive(value)) }.toString()

    fun sanitize(
        type: TechnicalEventType,
        rawJson: String?,
    ): String? {
        val allowedKey = allowedKeyFor(type)
        val parsed = rawJson?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
        val value = parsed?.get(allowedKey) ?: return null
        return buildJsonObject { put(allowedKey, value) }.toString()
    }

    private fun allowedKeyFor(type: TechnicalEventType): String =
        if (type == TechnicalEventType.BLOCK_APPLIED) PACKAGE_NAME_KEY else ERROR_CODE_KEY
}
