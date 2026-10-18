package com.niumi.feature.setup.pairing

/**
 * Nom proposé à l'ajout d'un boîtier (SPEC_ANDROID §15, Lot 8) : « Boîtier {n} », où `n` est le
 * plus petit entier qu'aucun boîtier associé ne porte déjà sous ce nom. Décision du 2026-10-17 :
 * la règle d'origine (« boîtiers associés + 1 ») proposait un second « Boîtier 3 » après le retrait
 * de « Boîtier 2 » parmi trois. Ce n'est qu'une proposition : l'unicité des noms n'est pas un
 * invariant du dépôt, qui distingue les boîtiers par leur `boxId`.
 */
object DefaultBoxName {
    fun next(existingNames: Collection<String>): String {
        val taken = existingNames.toSet()
        return generateSequence(1) { it + 1 }
            .map { "Boîtier $it" }
            .first { it !in taken }
    }
}
