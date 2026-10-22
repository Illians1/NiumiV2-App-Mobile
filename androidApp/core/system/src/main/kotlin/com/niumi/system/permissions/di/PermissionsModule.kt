package com.niumi.system.permissions.di

import com.niumi.system.permissions.AndroidPermissionStatusSource
import com.niumi.system.permissions.PermissionStatusSource
import com.niumi.system.readiness.DeviceReadinessChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings de l'écran 2 (étape 36). Sans état : aucune portée nécessaire. */
@Module
@InstallIn(SingletonComponent::class)
object PermissionsModule {
    @Provides
    fun providePermissionStatusSource(readinessChecker: DeviceReadinessChecker): PermissionStatusSource =
        AndroidPermissionStatusSource(readinessChecker)
}
