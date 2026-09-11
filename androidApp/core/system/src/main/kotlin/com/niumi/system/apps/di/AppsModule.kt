package com.niumi.system.apps.di

import android.content.Context
import com.niumi.system.apps.AppSelectionSource
import com.niumi.system.apps.AppSelectionStore
import com.niumi.system.apps.DataStoreAppSelectionStore
import com.niumi.system.apps.InstalledAppsSource
import com.niumi.system.apps.PackageManagerInstalledAppsSource
import com.niumi.system.apps.PackageManagerPackageQuery
import com.niumi.system.apps.PackageQuery
import com.niumi.system.common.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Sélecteur d'applications et sélection courante (SPEC_ANDROID §12.1, étape 13). Module dédié
 * plutôt qu'un ajout à `ReadinessModule`, qui approche le plafond `TooManyFunctions` de detekt —
 * même motif que `NfcModule`.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppsModule {
    @Provides
    @Singleton
    fun providePackageQuery(
        @ApplicationContext context: Context,
    ): PackageQuery = PackageManagerPackageQuery(context)

    @Provides
    @Singleton
    fun provideInstalledAppsSource(
        packageQuery: PackageQuery,
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): InstalledAppsSource = PackageManagerInstalledAppsSource(packageQuery, context.packageName, ioDispatcher)

    @Provides
    @Singleton
    fun provideAppSelectionStore(
        @ApplicationContext context: Context,
    ): AppSelectionStore = DataStoreAppSelectionStore(context)

    /**
     * Le diagnostic de §13 ne compte que la sélection : il reçoit la vue étroite, servie par le
     * même dépôt — deux sources distinctes divergeraient.
     */
    @Provides
    @Singleton
    fun provideAppSelectionSource(store: AppSelectionStore): AppSelectionSource = store
}
