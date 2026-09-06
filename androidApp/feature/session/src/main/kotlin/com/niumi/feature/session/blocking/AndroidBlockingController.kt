package com.niumi.feature.session.blocking

import com.niumi.database.BlockedPackage
import com.niumi.system.blocking.AccessibilityServiceStatus
import com.niumi.system.blocking.BlockingController
import com.niumi.system.blocking.InMemoryBlockedPackagesProjection
import com.niumi.system.common.OperationResult

/**
 * Traduit [BlockingController] (« Interfaces transverses » du plan MVP, écart de l'étape 5)
 * vers la projection en mémoire et le diagnostic d'activation du service. Vit dans
 * `:feature:session` : [AccessibilityServiceStatus] a besoin du nom qualifié de
 * [NiumiBlockingAccessibilityService], que `:core:system` ne peut pas référencer.
 */
class AndroidBlockingController(
    private val projection: InMemoryBlockedPackagesProjection,
    private val accessibilityServiceStatus: AccessibilityServiceStatus,
) : BlockingController {
    override fun apply(
        sessionId: String,
        packages: Set<BlockedPackage>,
    ): OperationResult = projection.apply(sessionId, packages)

    override fun remove(sessionId: String): OperationResult = projection.remove(sessionId)

    override fun effectivePackages(): Set<String> = projection.effectivePackages()

    override fun isServiceEnabled(): Boolean = accessibilityServiceStatus.isEnabled()
}
