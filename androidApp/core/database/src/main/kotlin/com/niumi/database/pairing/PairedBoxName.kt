package com.niumi.database.pairing

/**
 * SPEC_ANDROID §11.1 : un nom de Niumi Spot fait 1 à 30 caractères après `trim()`, un nom vide ou
 * blanc est refusé (Lot 8).
 */
object PairedBoxName {
    const val MAX_LENGTH = 30

    fun sanitize(raw: String): String? {
        val trimmed = raw.trim()
        return trimmed.takeIf { it.isNotEmpty() && it.length <= MAX_LENGTH }
    }
}
