package com.niumi.system.session

import com.niumi.core.interop.DomainViolationDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.EventReceipt

/** Résultat de [SessionCoordinator.dispatch] (« Interfaces transverses » du plan MVP). */
sealed interface DispatchResult {
    data class Applied(
        val snapshot: SessionSnapshotDto?,
        val requiredEffectsSucceeded: Boolean,
    ) : DispatchResult

    data class Duplicate(
        val receipt: EventReceipt,
    ) : DispatchResult

    data class Rejected(
        val violations: List<DomainViolationDto>,
    ) : DispatchResult
}
