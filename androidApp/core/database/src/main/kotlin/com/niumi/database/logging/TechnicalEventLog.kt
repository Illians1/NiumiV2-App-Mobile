package com.niumi.database.logging

/** Une entrée du journal technique local (SPEC_ANDROID §17). */
data class TechnicalEventEntry(
    val type: TechnicalEventType,
    val sessionId: String?,
    val packageName: String?,
    val occurredAtEpochMillis: Long,
)

/**
 * Journal technique local, borné à 200 événements (SPEC_ANDROID §17). `packageName` n'est
 * conservé que pour [TechnicalEventType.BLOCK_APPLIED] : ignoré silencieusement pour tout
 * autre type plutôt que de faire échouer l'appelant, un journal ne devant jamais faire
 * planter le parcours qu'il observe.
 *
 * Cette interface est portée par `:core:database` dès l'étape 3 (le receiver d'alarme en a
 * besoin) ; `InMemoryTechnicalEventLog` est une implémentation provisoire, remplacée par une
 * implémentation Room à l'étape 9 sans changer ce contrat.
 */
interface TechnicalEventLog {
    fun log(
        type: TechnicalEventType,
        sessionId: String? = null,
        packageName: String? = null,
    )

    fun recent(): List<TechnicalEventEntry>
}
