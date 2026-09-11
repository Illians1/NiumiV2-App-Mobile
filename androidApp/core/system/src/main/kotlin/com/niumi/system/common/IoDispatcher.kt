package com.niumi.system.common

import javax.inject.Qualifier

/**
 * Qualifie le `CoroutineDispatcher` d'entrées-sorties fourni par
 * [com.niumi.system.di.SystemModule]. Distinct de [DefaultDispatcher] : la résolution des
 * applications installées passe par `PackageManager`, une opération bloquante qui monopoliserait
 * le pool de calcul (étape 13). Injecté plutôt qu'appelé directement, pour rester substituable en
 * test.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
