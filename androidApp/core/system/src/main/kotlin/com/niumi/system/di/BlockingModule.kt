package com.niumi.system.di

import com.niumi.system.blocking.AccessibilityServiceBinding
import com.niumi.system.blocking.BlockedPackagesProjection
import com.niumi.system.blocking.InMemoryAccessibilityServiceBinding
import com.niumi.system.blocking.PersistedBlockedPackagesProjection
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binding de la projection de blocage (SPEC_ANDROID §12.2). Module dédié plutôt qu'ajouté à
 * [SystemModule] : `TooManyFunctions` de detekt a déjà imposé ce découpage pour l'audio et les
 * notifications (voir `AudioModule`, `SystemNotificationModule`).
 *
 * [com.niumi.system.blocking.AccessibilityServiceStatus] et
 * [com.niumi.system.blocking.BlockingController] ne sont pas liés ici : leurs implémentations
 * ont besoin du nom qualifié de `NiumiBlockingAccessibilityService`, une classe de
 * `:feature:session` que `:core:system` ne peut pas référencer (SPEC_ANDROID §6). Leurs
 * bindings vivent dans `SessionBlockingModule`.
 *
 * [AccessibilityServiceBinding] (2026-10-15), lui, ne connaît pas le nom du service : le service
 * l'injecte pour l'écrire, le statut pour le lire. Il est lié ici, et n'est donc pas remplacé par
 * la doublure de `SessionBlockingModule` sous instrumentation — où il reste faux, ce qui est l'état
 * réel (§19.2).
 */
@Module
@InstallIn(SingletonComponent::class)
interface BlockingModule {
    @Binds
    fun bindBlockedPackagesProjection(impl: PersistedBlockedPackagesProjection): BlockedPackagesProjection

    @Binds
    fun bindAccessibilityServiceBinding(impl: InMemoryAccessibilityServiceBinding): AccessibilityServiceBinding
}
