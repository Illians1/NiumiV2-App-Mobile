package com.niumi.database.logging

/** Une entrée du journal technique local (SPEC_ANDROID §17). */
data class TechnicalEventEntry(
    val type: TechnicalEventType,
    val sessionId: String?,
    val detailsJson: String?,
    val occurredAtEpochMillis: Long,
)

/**
 * Journal technique local, borné à 200 événements (SPEC_ANDROID §17). `detailsJson` est filtré à
 * l'écriture par [TechnicalEventDetails] : `packageName` n'est conservé que pour
 * [TechnicalEventType.BLOCK_APPLIED], un `errorCode` contrôlé pour tout autre type — toute autre
 * clé (texte d'accessibilité, hash de token, identifiant matériel) est supprimée avant d'atteindre
 * la base. Écart à la signature initiale de l'étape 3 (`packageName` direct) : le paramètre
 * `packageName` ne pouvait pas porter le « code d'erreur contrôlé » que §17 autorise aussi pour
 * les autres types (voir `ETAPE-09.md`).
 *
 * `log()` reste synchrone et non bloquant : appelé depuis `onReceive`, `onStartCommand` et
 * `onAccessibilityEvent`, tous sur le thread principal, où Room interdit toute requête —
 * l'implémentation poste l'écriture sur un scope injecté. `recent()` est `suspend` : aucun
 * appelant de production, seul l'écran de diagnostic (étape 16) l'utilisera depuis un ViewModel.
 */
interface TechnicalEventLog {
    fun log(
        type: TechnicalEventType,
        sessionId: String? = null,
        detailsJson: String? = null,
    )

    suspend fun recent(): List<TechnicalEventEntry>
}
