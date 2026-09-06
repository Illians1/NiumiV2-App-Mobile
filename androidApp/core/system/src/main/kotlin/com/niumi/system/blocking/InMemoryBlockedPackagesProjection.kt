package com.niumi.system.blocking

import com.niumi.database.BlockedPackage
import com.niumi.system.common.OperationResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Projection de blocage en mémoire, partagée par [AndroidBlockingController]
 * [com.niumi.feature.session.blocking.AndroidBlockingController] (écriture) et
 * [NiumiBlockingAccessibilityService][com.niumi.feature.session.blocking.NiumiBlockingAccessibilityService]
 * (lecture), tous deux dans le même process. Implémentation provisoire, remplacée par une
 * lecture Room à l'étape 15 sans changer [BlockedPackagesProjection] — même motif que
 * `InMemoryTechnicalEventLog` (`:core:database`).
 */
@Singleton
class InMemoryBlockedPackagesProjection
    @Inject
    constructor() : BlockedPackagesProjection {
        @Volatile
        private var state: BlockedPackagesState = BlockedPackagesState.Inactive

        override fun current(): BlockedPackagesState = state

        fun apply(
            sessionId: String,
            packages: Set<BlockedPackage>,
        ): OperationResult {
            val next = BlockedPackagesState.Active(sessionId, packages)
            if (state == next) return OperationResult.AlreadySatisfied
            state = next
            return OperationResult.Success
        }

        fun remove(sessionId: String): OperationResult {
            val current = state
            val matchesSession =
                when (current) {
                    BlockedPackagesState.Inactive -> false
                    is BlockedPackagesState.Active -> current.sessionId == sessionId
                    is BlockedPackagesState.Releasing -> current.sessionId == sessionId
                }
            if (!matchesSession) return OperationResult.AlreadySatisfied
            state = BlockedPackagesState.Inactive
            return OperationResult.Success
        }

        fun effectivePackages(): Set<String> =
            when (val current = state) {
                BlockedPackagesState.Inactive -> emptySet()
                is BlockedPackagesState.Active -> current.packages.map { it.packageName }.toSet()
                is BlockedPackagesState.Releasing -> current.effectivePackages.map { it.packageName }.toSet()
            }
    }
