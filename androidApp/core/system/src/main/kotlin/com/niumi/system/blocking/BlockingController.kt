package com.niumi.system.blocking

import com.niumi.database.BlockedPackage
import com.niumi.system.common.OperationResult

/**
 * Interface des « Interfaces transverses » du plan MVP, avec un écart validé à l'étape 5 :
 * `apply()` prend `Set<BlockedPackage>` et non `Set<String>`, pour porter le
 * `displayNameSnapshot` exigé par le texte de l'overlay (SPEC_ANDROID §12.2). Voir
 * `ETAPE-05.md`. Idempotente comme tous les adaptateurs système : ne lève jamais.
 */
interface BlockingController {
    fun apply(
        sessionId: String,
        packages: Set<BlockedPackage>,
    ): OperationResult

    fun remove(sessionId: String): OperationResult

    fun effectivePackages(): Set<String>

    fun isServiceEnabled(): Boolean
}
