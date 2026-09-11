package com.niumi.system.session

import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.PendingEffect
import com.niumi.system.common.OperationResult

/**
 * [incident] laisse un exécuteur signaler un incident constaté en marge de son propre résultat
 * (SPEC_ANDROID §11.3 : `REMOVE_BLOCKING` en `AlreadySatisfied` alors que le service
 * d'accessibilité est désactivé collecte `BLOCKING_PERMISSION_REVOKED`/`CRITICAL`) — distinct
 * de [result], qui ne porte que le succès/échec de l'effet lui-même.
 */
data class ExecutionOutcome(
    val result: OperationResult,
    val incident: SessionIncidentDto? = null,
)

/** Un exécuteur par `SessionEffectKind` (SPEC_CORE_KMP §6). Opère sur [PendingEffect], la forme
 * commune à une décision fraîche et à un rejeu d'outbox (les deux passent par le même
 * [EffectDispatcher]). */
fun interface EffectExecutor {
    suspend fun execute(
        effect: PendingEffect,
        snapshot: SessionSnapshotDto,
        extras: AndroidSessionExtras,
    ): ExecutionOutcome
}
