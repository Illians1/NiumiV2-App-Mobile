package com.niumi.feature.session.di

import android.content.Context
import com.niumi.feature.session.blocking.AndroidBlockingController
import com.niumi.feature.session.blocking.BlockOverlayController
import com.niumi.feature.session.blocking.NiumiBlockingAccessibilityService
import com.niumi.feature.session.blocking.WindowManagerBlockOverlayController
import com.niumi.system.blocking.AccessibilityServiceStatus
import com.niumi.system.blocking.AndroidAccessibilityServiceStatus
import com.niumi.system.blocking.BlockingController
import com.niumi.system.blocking.InMemoryBlockedPackagesProjection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * `:core:system` ne peut pas référencer [NiumiBlockingAccessibilityService] ni le `R` de
 * `:feature:session` : ces bindings vivent ici, module downstream (SPEC_ANDROID §6), même motif
 * que `RingingModule` dans `:feature:ringing`.
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionBlockingModule {
    @Provides
    @Singleton
    fun provideAccessibilityServiceStatus(
        @ApplicationContext context: Context,
    ): AccessibilityServiceStatus =
        AndroidAccessibilityServiceStatus(
            contentResolver = context.contentResolver,
            expectedComponent = "${context.packageName}/${NiumiBlockingAccessibilityService::class.java.name}",
        )

    @Provides
    @Singleton
    fun provideBlockingController(
        projection: InMemoryBlockedPackagesProjection,
        accessibilityServiceStatus: AccessibilityServiceStatus,
    ): BlockingController = AndroidBlockingController(projection, accessibilityServiceStatus)

    // Pas de binding pour BlockOverlayController : `TYPE_ACCESSIBILITY_OVERLAY` exige le
    // contexte du service d'accessibilité lui-même, que Hilt ne peut pas fournir. Le service
    // construit son overlay dans `onServiceConnected()` (voir NiumiBlockingAccessibilityService).
}
