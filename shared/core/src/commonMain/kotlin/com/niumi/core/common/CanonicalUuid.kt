package com.niumi.core.common

// Fonction de classification de caractère privée au fichier, hors de l'objet `CanonicalUuid`
// pour rester sous le seuil detekt `TooManyFunctions` (même motif que `nfc/BoxPayloadParser.kt`).
private fun Char.isLowerCaseHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f'

/**
 * Règle de format UUID partagée entre le parseur NFC (`boxId`, SPEC_CORE_KMP §9.1) et la
 * validation des identifiants d'événement et de session du moteur (`eventId`, `sessionId`,
 * SPEC_CORE_KMP §4, §5.2, §7). Extrait de `nfc.BoxPayloadParser` à l'étape 7 plutôt que dupliqué :
 * un identifiant Niumi est toujours un UUID canonique 8-4-4-4-12, hexadécimal minuscule, sans
 * accolades ni préfixe. Voir `ETAPE-07.md`.
 */
internal object CanonicalUuid {
    private const val UUID_LENGTH = 36

    // Longueurs des cinq groupes hexadécimaux d'un UUID canonique 8-4-4-4-12.
    private val GROUP_LENGTHS = listOf(8, 4, 4, 4, 12)

    // Boucle de validation par groupe, à clauses de garde : la forme la plus lisible pour une
    // chaîne de contrôles séquentiels dont le premier échec détermine le résultat (même décision
    // que `BoxPayloadParser.isCanonicalBoxId`, documentée dans `ETAPE-02.md`).
    @Suppress("ReturnCount")
    internal fun isCanonical(candidate: String): Boolean {
        if (candidate.length != UUID_LENGTH) return false
        var index = 0
        for ((groupIndex, groupLength) in GROUP_LENGTHS.withIndex()) {
            val group = candidate.substring(index, index + groupLength)
            if (!group.all { it.isLowerCaseHexDigit() }) return false
            index += groupLength
            val isLastGroup = groupIndex == GROUP_LENGTHS.lastIndex
            if (!isLastGroup) {
                if (candidate.getOrNull(index) != '-') return false
                index++
            }
        }
        return true
    }
}
