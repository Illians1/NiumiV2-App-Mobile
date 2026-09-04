package com.niumi.core.nfc

import kotlin.io.encoding.Base64
import kotlin.io.encoding.Base64.PaddingOption

// Fonctions de classification de caractères privées au fichier, hors de l'objet
// `BoxPayloadParser` pour rester sous le seuil detekt `TooManyFunctions`.
private fun Char.isLowerCaseHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f'

private fun Char.isBase64UrlChar(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '-' || this == '_'

/**
 * Parseur strict du payload NFC canonique (SPEC_CORE_KMP §9.1) :
 * `niumi://box/v1/{boxId}?token={token}`. Découpage manuel sur `://`, `/`, `?`, `&`, `=` — jamais
 * `java.net.URI` ni regex permissive, pour ne jamais accepter une forme qu'une normalisation
 * rendrait équivalente à la forme canonique (§9.1, dernier alinéa).
 *
 * Les contrôles sont ordonnés : le premier qui échoue détermine le statut retourné. Cet ordre
 * n'est pas fixé par SPEC_CORE_KMP §9.3, qui ne liste que l'ensemble des statuts possibles ; il a
 * été choisi pour cette étape et documenté dans `ETAPE-02.md`.
 */
public object BoxPayloadParser {
    private const val MAX_PAYLOAD_BYTES = 96
    private const val SUPPORTED_PROTOCOL_VERSION = 1
    private const val TOKEN_BYTE_LENGTH = 16
    private const val TOKEN_ENCODED_LENGTH = 22
    private const val BOX_ID_LENGTH = 36
    private const val SCHEME = "niumi"
    private const val HOST = "box"
    private const val VERSION_SEGMENT = "v1"
    private const val TOKEN_KEY = "token"
    private const val SCHEME_SEPARATOR = "://"

    // Longueurs des cinq groupes hexadécimaux d'un UUID canonique 8-4-4-4-12.
    private val BOX_ID_GROUP_LENGTHS = listOf(8, 4, 4, 4, 12)

    // Derniers caractères Base64 URL valides pour un token de 16 octets : 22 caractères encodent
    // 132 bits utiles sur les 4 derniers, dont seulement 8 bits utiles restent après les 15
    // premiers octets pleins — les 4 bits de bourrage du dernier caractère doivent donc être
    // nuls. Seules les valeurs 0, 16, 32 et 48 de l'alphabet Base64 URL (A, Q, g, w) satisfont
    // cette contrainte.
    private const val VALID_TOKEN_TAIL_CHARS = "AQgw"

    private val base64UrlNoPadding = Base64.UrlSafe.withPadding(PaddingOption.ABSENT)

    private data class SchemeResult(
        val status: BoxPayloadStatus?,
        val afterScheme: String?,
    )

    private data class PathComponents(
        val status: BoxPayloadStatus?,
        val authority: String,
        val segmentsAfterAuthority: List<String>,
        val rawQuery: String?,
    )

    private data class TokenExtraction(
        val status: BoxPayloadStatus?,
        val tokenValue: String?,
    )

    // Validateur multi-étapes à clauses de garde : chaque étape est déléguée à une fonction dédiée
    // (voir ci-dessous), ce qui maintient la complexité cyclomatique et la longueur de cette
    // fonction sous les seuils detekt. Le nombre de points de sortie reste néanmoins élevé — c'est
    // la forme la plus lisible pour une chaîne de contrôles séquentiels dont le premier échec
    // détermine le résultat ; restructurer en une unique expression sans `return` nuirait à la
    // lisibilité sans supprimer de bogue. Décision documentée dans `ETAPE-02.md`.
    @Suppress("ReturnCount")
    public fun parse(uri: String): BoxPayloadResult {
        checkPreconditions(uri)?.let { return invalid(it) }

        val schemeResult = extractAfterScheme(uri)
        if (schemeResult.status != null) return invalid(schemeResult.status)

        val components = splitPathAndQuery(schemeResult.afterScheme.orEmpty())
        if (components.status != null) return invalid(components.status)

        checkAuthority(components.authority)?.let { return invalid(it) }
        checkVersionSegment(components.segmentsAfterAuthority)?.let { return invalid(it) }

        val boxId = components.segmentsAfterAuthority[1]
        if (!isCanonicalBoxId(boxId)) return invalid(BoxPayloadStatus.INVALID_BOX_ID)

        val tokenExtraction = extractTokenValue(components.rawQuery)
        if (tokenExtraction.status != null) return invalid(tokenExtraction.status)

        val tokenBytes =
            decodeToken(tokenExtraction.tokenValue.orEmpty()) ?: return invalid(BoxPayloadStatus.INVALID_TOKEN)

        return BoxPayloadResult(
            status = BoxPayloadStatus.VALID,
            payload = BoxPayload(protocolVersion = SUPPORTED_PROTOCOL_VERSION, boxId = boxId, tokenBytes = tokenBytes),
        )
    }

    private fun invalid(status: BoxPayloadStatus) = BoxPayloadResult(status, payload = null)

    private fun checkPreconditions(uri: String): BoxPayloadStatus? =
        when {
            uri.encodeToByteArray().size > MAX_PAYLOAD_BYTES -> BoxPayloadStatus.PAYLOAD_TOO_LONG
            uri.isEmpty() || containsControlOrSurrogateChar(uri) -> BoxPayloadStatus.MALFORMED_URI
            uri.contains('%') -> BoxPayloadStatus.UNEXPECTED_COMPONENT
            else -> null
        }

    private fun containsControlOrSurrogateChar(uri: String): Boolean = uri.any { it.isISOControl() || it.isSurrogate() }

    private fun extractAfterScheme(uri: String): SchemeResult {
        val separatorIndex = uri.indexOf(SCHEME_SEPARATOR)
        val scheme = if (separatorIndex > 0) uri.substring(0, separatorIndex) else null
        return when {
            separatorIndex <= 0 -> SchemeResult(BoxPayloadStatus.MALFORMED_URI, afterScheme = null)
            scheme != SCHEME -> SchemeResult(BoxPayloadStatus.UNSUPPORTED_SCHEME, afterScheme = null)
            else -> SchemeResult(status = null, afterScheme = uri.substring(separatorIndex + SCHEME_SEPARATOR.length))
        }
    }

    private fun splitPathAndQuery(afterScheme: String): PathComponents {
        val fragmentSplit = afterScheme.split('#', limit = 2)
        if (fragmentSplit.size > 1) {
            return PathComponents(
                status = BoxPayloadStatus.UNEXPECTED_COMPONENT,
                authority = "",
                segmentsAfterAuthority = emptyList(),
                rawQuery = null,
            )
        }

        val pathAndQuery = fragmentSplit[0]
        val queryIndex = pathAndQuery.indexOf('?')
        val authorityAndPath = if (queryIndex >= 0) pathAndQuery.substring(0, queryIndex) else pathAndQuery
        val rawQuery = if (queryIndex >= 0) pathAndQuery.substring(queryIndex + 1) else null
        val pathSegments = authorityAndPath.split('/')

        return PathComponents(
            status = null,
            authority = pathSegments.first(),
            segmentsAfterAuthority = pathSegments.drop(1),
            rawQuery = rawQuery,
        )
    }

    private fun checkAuthority(authority: String): BoxPayloadStatus? =
        when {
            authority.contains('@') || authority.contains(':') -> BoxPayloadStatus.UNEXPECTED_COMPONENT
            authority != HOST -> BoxPayloadStatus.UNSUPPORTED_HOST
            else -> null
        }

    private fun checkVersionSegment(segmentsAfterAuthority: List<String>): BoxPayloadStatus? {
        val firstSegment = segmentsAfterAuthority.firstOrNull()
        return when {
            firstSegment != VERSION_SEGMENT -> BoxPayloadStatus.UNSUPPORTED_VERSION
            segmentsAfterAuthority.size != 2 -> BoxPayloadStatus.UNEXPECTED_COMPONENT
            else -> null
        }
    }

    private fun extractTokenValue(rawQuery: String?): TokenExtraction {
        val params = rawQuery?.split('&').orEmpty()
        val tokenParam = params.singleOrNull()?.split('=', limit = 2)
        return when {
            rawQuery.isNullOrEmpty() -> {
                TokenExtraction(BoxPayloadStatus.MISSING_TOKEN, tokenValue = null)
            }

            params.size != 1 -> {
                TokenExtraction(BoxPayloadStatus.UNEXPECTED_COMPONENT, tokenValue = null)
            }

            tokenParam == null || tokenParam.size != 2 || tokenParam[0] != TOKEN_KEY -> {
                TokenExtraction(BoxPayloadStatus.UNEXPECTED_COMPONENT, tokenValue = null)
            }

            else -> {
                TokenExtraction(status = null, tokenValue = tokenParam[1])
            }
        }
    }

    // Boucle de validation par groupe, à clauses de garde : voir la justification sur `parse`.
    @Suppress("ReturnCount")
    private fun isCanonicalBoxId(candidate: String): Boolean {
        if (candidate.length != BOX_ID_LENGTH) return false
        var index = 0
        for ((groupIndex, groupLength) in BOX_ID_GROUP_LENGTHS.withIndex()) {
            val group = candidate.substring(index, index + groupLength)
            if (!group.all { it.isLowerCaseHexDigit() }) return false
            index += groupLength
            val isLastGroup = groupIndex == BOX_ID_GROUP_LENGTHS.lastIndex
            if (!isLastGroup) {
                if (candidate.getOrNull(index) != '-') return false
                index++
            }
        }
        return true
    }

    // Chaîne de contrôles indépendants sur le token : voir la justification sur `parse`. La
    // capture est volontairement muette (voir commentaire sur l'exception) : seul un statut
    // typé doit atteindre l'appelant, jamais le détail de l'exception ni le token lui-même.
    @Suppress("ReturnCount")
    private fun decodeToken(token: String): ByteArray? {
        if (token.length != TOKEN_ENCODED_LENGTH) return null
        if (!token.all { it.isBase64UrlChar() }) return null
        if (token.last() !in VALID_TOKEN_TAIL_CHARS) return null

        @Suppress("SwallowedException")
        val decoded =
            try {
                base64UrlNoPadding.decode(token)
            } catch (malformedBase64: IllegalArgumentException) {
                return null
            }
        return decoded.takeIf { it.size == TOKEN_BYTE_LENGTH }
    }
}
