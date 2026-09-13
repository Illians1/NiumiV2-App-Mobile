package com.niumi.database.logging

/** Nombre maximal d'entrées conservées par le journal technique (SPEC_ANDROID §17). */
const val MAX_TECHNICAL_EVENTS = 200

/** Une entrée du journal technique local (SPEC_ANDROID §17). */
data class TechnicalEventEntry(
    val type: TechnicalEventType,
    val sessionId: String?,
    val detailsJson: String?,
    val occurredAtEpochMillis: Long,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
)

/**
 * Journal technique local, borné à [MAX_TECHNICAL_EVENTS] événements (SPEC_ANDROID §17).
 * `detailsJson` est filtré à l'écriture par [TechnicalEventDetails] : `packageName` n'est conservé
 * que pour [TechnicalEventType.BLOCK_APPLIED], un `errorCode` contrôlé pour tout autre type —
 * toute autre clé (texte d'accessibilité, hash de token, identifiant matériel) est supprimée avant
 * d'atteindre la base. Écart à la signature initiale de l'étape 3 (`packageName` direct) : le
 * paramètre `packageName` ne pouvait pas porter le « code d'erreur contrôlé » que §17 autorise
 * aussi pour les autres types (voir `ETAPE-09.md`).
 *
 * Le contexte d'appareil de §17 ([DeviceContext]) est ajouté par l'implémentation et n'apparaît pas
 * dans la signature de [log] : il est identique pour tous les appels d'un même processus, et le
 * faire porter par les seize sites d'appel en ferait une discipline à tenir plutôt qu'une garantie.
 *
 * `log()` reste synchrone et non bloquant : appelé depuis `onReceive`, `onStartCommand` et
 * `onAccessibilityEvent`, tous sur le thread principal, où Room interdit toute requête —
 * l'implémentation poste l'écriture sur un scope injecté. `recent()` est `suspend` : son seul
 * appelant de production est l'écran de diagnostic (écran 12, étape 16), depuis un ViewModel.
 */
interface TechnicalEventLog {
    fun log(
        type: TechnicalEventType,
        sessionId: String? = null,
        detailsJson: String? = null,
    )

    suspend fun recent(): List<TechnicalEventEntry>
}
