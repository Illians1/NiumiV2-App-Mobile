package com.niumi.system.blocking.guard

import com.niumi.system.common.OperationResult

/**
 * Démarre ou arrête le service de garde du blocage (SPEC_ANDROID §12.5). Implémenté dans
 * `:feature:session`, qui possède le service : `:core:system` ne peut pas le référencer (§6).
 *
 * [keep] est idempotent : sur un service déjà actif, il remplace seulement sa notification. Il ne
 * lève jamais : un démarrage refusé par Android depuis l'arrière-plan
 * (`ForegroundServiceStartNotAllowedException`) devient un [OperationResult.Failure].
 */
interface BlockingGuardController {
    fun keep(command: BlockingGuardCommand): OperationResult

    fun release(): OperationResult
}
