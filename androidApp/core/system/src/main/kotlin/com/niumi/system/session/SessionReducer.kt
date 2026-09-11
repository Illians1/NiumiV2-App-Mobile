package com.niumi.system.session

import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionDecisionDto
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionSnapshotDto

/**
 * Abstraction fine de `NiumiCoreFacade.reduce` (classe finale sans interface côté `:shared:core`) :
 * permet à `SessionCoordinatorIdempotenceTest` de prouver qu'un doublon strict n'appelle jamais le
 * moteur (SPEC_CORE_KMP §5.2, §6.1) sans instancier de vraie façade.
 */
fun interface SessionReducer {
    fun reduce(
        snapshot: SessionSnapshotDto?,
        event: SessionEventDto,
    ): SessionDecisionDto
}

class FacadeSessionReducer(
    private val facade: NiumiCoreFacade,
) : SessionReducer {
    override fun reduce(
        snapshot: SessionSnapshotDto?,
        event: SessionEventDto,
    ): SessionDecisionDto = facade.reduce(snapshot, event)
}
