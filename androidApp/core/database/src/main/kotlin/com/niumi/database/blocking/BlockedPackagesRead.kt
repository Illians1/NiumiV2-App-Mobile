package com.niumi.database.blocking

/**
 * Résultat d'une lecture de la projection persistée. Distingue une projection **reconstruite**
 * d'une lecture **impossible**, ce que [BlockedPackagesState] ne peut pas exprimer : `Inactive`
 * y signifie « aucune session ne bloque quoi que ce soit », jamais « je ne sais pas ».
 *
 * SPEC_ANDROID §13 interdit toute suppression silencieuse du blocage à cause d'un snapshot
 * illisible. Séparer les deux notions rend cette garantie structurelle plutôt que déclarative :
 * la projection à cache de `:core:system` ne peut mémoriser qu'un [Resolved], donc un snapshot
 * corrompu ne peut pas, même par erreur de programmation, se transformer en `Inactive`.
 *
 * Écart au plan de l'étape 15, qui prévoyait une quatrième variante `Unreadable` dans
 * [BlockedPackagesState] traitée par `BlockingDecision.decide`. Cette forme-là obligeait
 * l'algorithme pur de §12.2 à décider seul du sort d'un état qu'il ne peut pas interpréter — il
 * ne connaît pas la décision précédente — et laissait le type illisible atteignable par le
 * service. Voir `ETAPE-15.md`.
 */
sealed interface BlockedPackagesRead {
    data class Resolved(
        val state: BlockedPackagesState,
    ) : BlockedPackagesRead

    data class Unreadable(
        val reason: String,
    ) : BlockedPackagesRead
}
