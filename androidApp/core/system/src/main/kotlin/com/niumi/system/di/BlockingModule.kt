package com.niumi.system.di

import com.niumi.system.blocking.BlockedPackagesProjection
import com.niumi.system.blocking.InMemoryBlockedPackagesProjection
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
 */
@Module
@InstallIn(SingletonComponent::class)
interface BlockingModule {
    @Binds
    fun bindBlockedPackagesProjection(impl: InMemoryBlockedPackagesProjection): BlockedPackagesProjection
}
