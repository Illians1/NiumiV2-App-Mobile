package com.niumi.system.blocking.fakes

import com.niumi.database.blocking.BlockedPackagesRead
import com.niumi.database.blocking.BlockedPackagesSource
import com.niumi.database.blocking.BlockedPackagesState

/**
 * Source de projection pilotée par le test. [reads] compte les lectures : c'est la seule façon de
 * prouver qu'un abonnement au flux de décisions déclenche bien un réalignement.
 */
class RecordingBlockedPackagesSource(
    var next: BlockedPackagesRead = BlockedPackagesRead.Resolved(BlockedPackagesState.Inactive),
) : BlockedPackagesSource {
    var reads: Int = 0
        private set

    override suspend fun read(): BlockedPackagesRead {
        reads++
        return next
    }
}
