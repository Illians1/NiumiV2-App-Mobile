package com.niumi.system.blocking

import com.niumi.database.BlockedPackage
import com.niumi.database.blocking.BlockedPackagesRead
import com.niumi.database.blocking.BlockedPackagesSource
import com.niumi.database.blocking.BlockedPackagesState
import com.niumi.system.common.OperationResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Projection de blocage lue par
 * [NiumiBlockingAccessibilityService][com.niumi.feature.session.blocking.NiumiBlockingAccessibilityService]
 * (SPEC_ANDROID §12.2). Le cache `@Volatile` est une nécessité de forme, pas une source de
 * vérité : `onAccessibilityEvent` doit décider immédiatement sur le thread principal, alors que
 * toute lecture de persistance est `suspend`.
 *
 * L'autorité est [BlockedPackagesSource] — Room, ou le snapshot Direct Boot avant
 * déverrouillage — et [refresh] réaligne le cache sur elle. C'est ce qui distingue cette classe
 * de la projection purement en mémoire de l'étape 5, où un processus tué faisait disparaître le
 * blocage d'une session encore active.
 *
 * [apply] et [remove] subsistent parce que `BlockingController.apply` n'est pas `suspend` et doit
 * rendre un [OperationResult] synchrone à l'exécuteur d'effet. Ce ne sont pas des écritures
 * concurrentes : l'exécuteur ne s'exécute qu'**après** `commitDecision`, donc les paquets qu'il
 * transmet sont déjà ceux de la base. Ils avancent le cache sans attendre le prochain [refresh].
 *
 * Une lecture [BlockedPackagesRead.Unreadable] **ne touche pas** au cache : SPEC_ANDROID §13
 * interdit de lever le blocage à cause d'un snapshot illisible, et `Inactive` signifierait
 * précisément « plus rien n'est bloqué ».
 */
@Singleton
class PersistedBlockedPackagesProjection
    @Inject
    constructor(
        private val source: BlockedPackagesSource,
    ) : BlockedPackagesProjection {
        @Volatile
        private var state: BlockedPackagesState = BlockedPackagesState.Inactive

        override fun current(): BlockedPackagesState = state

        /** Renvoie l'état retenu, qui reste le précédent si la persistance est illisible. */
        suspend fun refresh(): BlockedPackagesState {
            when (val read = source.read()) {
                is BlockedPackagesRead.Resolved -> state = read.state
                is BlockedPackagesRead.Unreadable -> Unit
            }
            return state
        }

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
