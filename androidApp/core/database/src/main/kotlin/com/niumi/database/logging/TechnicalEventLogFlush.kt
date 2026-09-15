package com.niumi.database.logging

/**
 * Verse dans Room ce que le journal technique a écrit en mémoire avant le premier déverrouillage
 * (SPEC_ANDROID §17, §9.3 ; étape 20). Interface séparée de [TechnicalEventLog] : le vidage n'a de
 * sens pour aucune de ses deux implémentations prises isolément ([InMemoryTechnicalEventLog] n'a
 * personne vers qui verser, [RoomTechnicalEventLog] n'a rien à vider) — l'ajouter à [TechnicalEventLog]
 * aurait posé une méthode sans réponse sensée sur seize sites d'appel.
 */
fun interface TechnicalEventLogFlush {
    suspend fun flush()
}
