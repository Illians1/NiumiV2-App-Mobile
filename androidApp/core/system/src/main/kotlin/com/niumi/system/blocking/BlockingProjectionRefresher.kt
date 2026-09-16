package com.niumi.system.blocking

import com.niumi.system.session.SessionSnapshotPublisher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Réaligne [PersistedBlockedPackagesProjection] sur la persistance aux deux moments prévus par
 * SPEC_ANDROID §12.2 : à la (re)connexion du service d'accessibilité, et à chaque décision de
 * session publiée.
 *
 * Classe distincte du service pour être prouvable : un `AccessibilityService` ne s'instancie pas
 * en test JVM et le dépôt n'utilise pas Robolectric. Le service ne garde donc que le câblage —
 * un scope qu'il annule lui-même — et la logique d'abonnement est testée ici.
 *
 * [observeDecisions] ne rend jamais la main : `SessionSnapshotPublisher.snapshot` est un
 * `StateFlow`, sa collecte émet d'abord la valeur courante, puis chaque décision suivante.
 * [refreshNow] reste nécessaire malgré cela : un `StateFlow` conflue les valeurs égales, alors
 * qu'un statut d'effet peut changer dans l'outbox sans que le snapshot bouge (`RELEASING`
 * partiel, SPEC_CORE_KMP §4).
 */
@Singleton
class BlockingProjectionRefresher
    @Inject
    constructor(
        private val projection: PersistedBlockedPackagesProjection,
        private val publisher: SessionSnapshotPublisher,
    ) {
        suspend fun refreshNow() {
            projection.refresh()
        }

        /**
         * [activationListener] est prévenu quand la projection passe d'inactive à active, pour que le
         * service rejoue sa décision sur la dernière application vue (SPEC_ANDROID §12.4, Lot 6). Il
         * est posé sur la projection elle-même, seul point de passage de tout changement d'état :
         * la transition d'un début différé peut venir du rafraîchissement déclenché par la
         * publication du snapshot **ou** de l'exécution d'`APPLY_BLOCKING`, et les deux arrivent dans
         * la même décision.
         */
        suspend fun observeDecisions(activationListener: BlockingActivationListener? = null): Nothing {
            projection.observeActivation(activationListener)
            publisher.snapshot.collect { projection.refresh() }
        }
    }
