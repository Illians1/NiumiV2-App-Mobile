package com.niumi.database.mapping

import com.niumi.core.interop.SessionEventDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.security.MessageDigest

/**
 * Empreinte canonique du payload d'un `SessionEventDto` (SPEC_CORE_KMP §6.1) : un événement déjà
 * reçu avec la même empreinte est reconnu sans nouvel appel à `reduce()` ni nouvel effet.
 *
 * `eventId` est retiré de l'objet racine avant sérialisation : deux envois du même événement avec
 * des `eventId` différents (ex. après un redémarrage de processus qui en régénère un) doivent
 * produire la même empreinte. `nfcProof` est déjà exclu par `@Transient` sur `SessionEventDto`.
 *
 * Les clés sont triées récursivement (indépendamment de l'ordre de déclaration du DTO) : sans ce
 * tri, une simple réorganisation cosmétique des champs dans `:shared:core` changerait toutes les
 * empreintes déjà écrites en base et casserait la déduplication des événements en vol. L'ordre des
 * tableaux, lui, est sémantique et n'est pas modifié.
 *
 * `MagicNumber` supprimé pour `hexOf` : `0xff`/`0x0f` sont le masque d'octet et le demi-octet de
 * l'encodage hexadécimal lui-même, comme `com.niumi.core.nfc.Sha256.hexOf` dans `:shared:core`.
 */
@Suppress("MagicNumber")
object EventFingerprint {
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = true
        }

    fun canonicalJson(event: SessionEventDto): String {
        val root = json.encodeToJsonElement(SessionEventDto.serializer(), event).jsonObject
        val withoutEventId = buildJsonObject { root.forEach { (key, value) -> if (key != "eventId") put(key, value) } }
        return canonicalize(withoutEventId).toString()
    }

    fun of(event: SessionEventDto): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(canonicalJson(event).toByteArray(Charsets.UTF_8))
        return hexOf(digest)
    }

    // Sans `String.format` (locale par défaut interdite par detekt `ImplicitDefaultLocale`),
    // même motif que `com.niumi.core.nfc.Sha256.hexOf`.
    private fun hexOf(bytes: ByteArray): String {
        val digits = "0123456789abcdef"
        val builder = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val value = byte.toInt() and 0xff
            builder.append(digits[value ushr 4])
            builder.append(digits[value and 0x0f])
        }
        return builder.toString()
    }

    private fun canonicalize(element: JsonElement): JsonElement =
        when (element) {
            is JsonObject -> {
                JsonObject(
                    element.entries
                        .sortedBy { it.key }
                        .associate { it.key to canonicalize(it.value) },
                )
            }

            is JsonArray -> {
                JsonArray(element.map(::canonicalize))
            }

            else -> {
                element
            }
        }
}
