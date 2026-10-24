package com.niumi.feature.setup.pairing

/**
 * Nom proposé à l'ajout d'un Niumi Spot (SPEC_ANDROID §15, Lot 8) : « Niumi Spot {n} », où `n` est le
 * plus petit entier qu'aucun Niumi Spot associé ne porte déjà sous ce nom. Décision du 2026-10-17 :
 * la règle d'origine (« Niumi Spots associés + 1 ») proposait un second « Niumi Spot 3 » après le retrait
 * de « Niumi Spot 2 » parmi trois. Ce n'est qu'une proposition : l'unicité des noms n'est pas un
 * invariant du dépôt, qui distingue les Niumi Spots par leur `boxId`.
 */
object DefaultBoxName {
    fun next(existingNames: Collection<String>): String {
        val taken = existingNames.toSet()
        return generateSequence(1) { it + 1 }
            .map { "Niumi Spot $it" }
            .first { it !in taken }
    }
}
