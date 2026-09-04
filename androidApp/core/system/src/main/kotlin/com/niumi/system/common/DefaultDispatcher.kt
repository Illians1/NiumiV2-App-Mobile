package com.niumi.system.common

import javax.inject.Qualifier

/**
 * Qualifie le `CoroutineDispatcher` de calcul fourni par [com.niumi.system.di.SystemModule].
 * Un composant qui a besoin d'un dispatcher l'injecte plutôt que d'appeler `Dispatchers.Default`
 * directement, pour rester substituable en test.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
