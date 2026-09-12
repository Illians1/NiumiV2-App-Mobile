package com.niumi.database.blocking

/**
 * Lecture de la projection de blocage depuis la persistance. Interface plutôt que classe
 * concrète pour la même raison que [com.niumi.database.SessionStore] et
 * [com.niumi.database.directboot.DirectBootStore] : le cache de `:core:system` doit se prouver
 * sans base Room, notamment sur la règle « une persistance illisible ne lève pas le blocage »
 * (SPEC_ANDROID §13), qu'aucune base réelle ne sait produire à la demande.
 */
interface BlockedPackagesSource {
    suspend fun read(): BlockedPackagesRead
}
