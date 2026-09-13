package com.niumi.feature.ringing

import com.niumi.database.BlockedPackage
import com.niumi.system.blocking.AccessibilityServiceStatus
import com.niumi.system.blocking.BlockingController
import com.niumi.system.common.OperationResult
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Même trou de graphe que [TestComponentResolverModule], élargi par l'étape 17 : `AlarmReceiver`
 * injecte désormais `AlarmTriggerHandler`, donc `SessionCoordinator`, donc les exécuteurs de
 * blocage et la surveillance de §13.1 — dont les liaisons réelles vivent dans `SessionBlockingModule`
 * (`:feature:session`), absent de l'APK de test de ce module.
 *
 * Les doublures sont inertes : aucun test de `:feature:ringing` n'exerce le blocage. La chaîne
 * complète, avec les vraies liaisons, est vérifiée par `AlarmChainInstrumentedTest` dans `:app`,
 * seul module dont le graphe est complet.
 */
@Module
@InstallIn(SingletonComponent::class)
object TestSessionBindingsModule {
    @Provides
    @Singleton
    fun provideAccessibilityServiceStatus(): AccessibilityServiceStatus =
        object : AccessibilityServiceStatus {
            override fun isEnabled(): Boolean = false
        }

    @Provides
    @Singleton
    fun provideBlockingController(): BlockingController =
        object : BlockingController {
            override fun apply(
                sessionId: String,
                packages: Set<BlockedPackage>,
            ): OperationResult = OperationResult.Success

            override fun remove(sessionId: String): OperationResult = OperationResult.Success

            override fun effectivePackages(): Set<String> = emptySet()

            override fun isServiceEnabled(): Boolean = false
        }
}
