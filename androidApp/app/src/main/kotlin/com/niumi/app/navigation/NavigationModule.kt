package com.niumi.app.navigation

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * `@Multibinds` déclare l'ensemble sans forcer au moins un lien : en `release`, où aucun
 * module n'apporte de contributeur, `Set<NavGraphContributor>` est simplement vide plutôt que
 * de faire échouer l'injection.
 */
@Module
@InstallIn(SingletonComponent::class)
interface NavigationModule {
    @Multibinds
    fun bindNavGraphContributors(): Set<NavGraphContributor>
}
