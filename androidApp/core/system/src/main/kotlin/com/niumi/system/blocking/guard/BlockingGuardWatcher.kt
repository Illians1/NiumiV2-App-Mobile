package com.niumi.system.blocking.guard

import com.niumi.database.logging.TechnicalEventDetails
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.common.DefaultDispatcher
import com.niumi.system.common.OperationResult
import com.niumi.system.session.SessionSnapshotPublisher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Démarre et arrête le service de garde de session au fil des snapshots publiés
 * (SPEC_ANDROID §12.5), et lui transmet chaque changement de phase ou d'heure pour sa
 * notification. Le snapshot est le bon signal : chaque décision du moteur le publie en premier
 * effet, et chaque réconciliation le republie (démarrage du processus, redémarrage, changement
 * d'heure). Aucun effet nouveau n'est donc ajouté au contrat commun.
 *
 * Vit au niveau de l'application, lancé par `NiumiApplication` : une transition peut arriver sans
 * interface (début différé à 22:30 par `BlockingStartReceiver`, réconciliation au démarrage).
 *
 * Un démarrage refusé par Android depuis l'arrière-plan est journalisé et retenu : la décision
 * suivante, ou [retryRefused] au retour de Niumi au premier plan, le rejoue. Le blocage lui-même
 * n'en dépend pas ; seule la protection contre le nettoyeur de HyperOS manque entre-temps.
 */
@Singleton
class BlockingGuardWatcher
    @Inject
    constructor(
        private val publisher: SessionSnapshotPublisher,
        private val controller: BlockingGuardController,
        private val technicalEventLog: TechnicalEventLog,
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + dispatcher)
        private val started = AtomicBoolean(false)
        private val lock = Any()

        /** Dernière décision réellement appliquée ; une décision égale n'est pas rejouée. */
        private var applied: BlockingGuardDecision? = null

        /** Commande refusée par Android, à rejouer dès qu'un démarrage redevient permis. */
        private var refused: BlockingGuardCommand? = null

        fun start() {
            if (started.compareAndSet(false, true)) scope.launch { observe() }
        }

        /**
         * Rejoue un démarrage refusé. Appelé quand Niumi passe au premier plan : Android y autorise
         * toujours le démarrage d'un service de premier plan.
         */
        fun retryRefused() {
            synchronized(lock) {
                val command = refused ?: return
                apply(BlockingGuardDecision.Keep(command))
            }
        }

        internal suspend fun observe() {
            publisher.snapshot.collect { snapshot ->
                synchronized(lock) { apply(BlockingGuardPolicy.decide(snapshot)) }
            }
        }

        /** Appelé sous [lock]. */
        private fun apply(decision: BlockingGuardDecision) {
            if (decision == BlockingGuardDecision.Unknown || decision == applied) return
            when (decision) {
                is BlockingGuardDecision.Keep -> keep(decision)
                BlockingGuardDecision.Release -> release()
                BlockingGuardDecision.Unknown -> Unit
            }
        }

        private fun keep(decision: BlockingGuardDecision.Keep) {
            val sessionId = decision.command.sessionId
            when (val result = controller.keep(decision.command)) {
                is OperationResult.Failure -> {
                    refused = decision.command
                    technicalEventLog.log(
                        TechnicalEventType.BLOCKING_GUARD_START_REFUSED,
                        sessionId,
                        TechnicalEventDetails.errorCode(result.code),
                    )
                }

                OperationResult.Success, OperationResult.AlreadySatisfied -> {
                    // Le passage au coucher, ou une heure changée, remplace la notification d'un
                    // service déjà actif pour la même session : ce n'est pas un nouveau démarrage.
                    val alreadyRunning = (applied as? BlockingGuardDecision.Keep)?.command?.sessionId == sessionId
                    refused = null
                    applied = decision
                    if (!alreadyRunning) technicalEventLog.log(TechnicalEventType.BLOCKING_GUARD_STARTED, sessionId)
                }
            }
        }

        private fun release() {
            refused = null
            val previous = (applied as? BlockingGuardDecision.Keep)?.command?.sessionId
            applied = BlockingGuardDecision.Release
            // `stopService` sur un service arrêté est sans effet : rien à journaliser au démarrage
            // d'un processus sans session.
            if (controller.release() == OperationResult.Success) {
                technicalEventLog.log(TechnicalEventType.BLOCKING_GUARD_STOPPED, previous)
            }
        }
    }
