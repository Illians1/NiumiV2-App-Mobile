package com.niumi.system.nfc

import com.niumi.core.interop.BoxPayloadDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.nfc.BoxPayloadStatus
import com.niumi.core.nfc.BoxVerificationStatus
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.LoadResult
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionPersistenceGateway
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fin ou annulation de session par scan du Niumi Spot (SPEC_ANDROID §11.3 ; SPEC_CORE_KMP §4, §6,
 * §9.2, §10). **Réalise les points 1 à 4 de §11.3** — éligibilité de l'état, réconciliation avant
 * scan, vérification du Niumi Spot, envoi de `VALID_NFC_SCANNED` — et interprète le résultat. Les
 * points 5 à 14 (persistance atomique, exécution des effets, `RELEASE_SUCCEEDED`/`FAILED`, état
 * final, effacement du pointeur) appartiennent au coordinateur et à ses exécuteurs depuis
 * l'étape 11 : rien n'en est redit ici.
 *
 * **Ne décide jamais de la validité d'un payload** (SPEC_CORE_KMP §9.3) : le parseur et le
 * vérificateur communs tranchent, ce cas d'usage ne fait que leur fournir le Niumi Spot de référence
 * et traduire leur verdict en [ScanOutcome].
 *
 * **La liste de référence est l'ensemble courant de [PairedBoxStore], lu au moment du scan**
 * (§11.3 point 3 ; SPEC_CORE_KMP §2, décisions 11 et 16 ; décision Q11 du 2026-10-19) : un Niumi Spot
 * ajouté pendant la session la termine, un Niumi Spot retiré n'arrête plus rien. La lecture a lieu après
 * l'éventuelle réconciliation, au plus près de la vérification. Le scan exige un appareil
 * déverrouillé (SPEC_ANDROID §4.4) ; avant déverrouillage, `RoomPairedBoxStore.all()` répondrait une
 * liste vide, donc un refus, jamais une acceptation à tort.
 *
 * **S'exécute hors du `Mutex` du coordinateur**, comme `AlarmTriggerHandler` (étape 17) : ce mutex
 * est privé et non réentrant, et lire puis dispatcher est le seul motif possible. Si la révision
 * bougeait entre la lecture et le dispatch, le moteur répondrait `STALE_REVISION` — un scan refusé
 * sans effet de bord, jamais une libération à moitié faite.
 */
@Singleton
class HandleValidNfcUseCase
    @Inject
    constructor(
        private val gateway: SessionPersistenceGateway,
        private val coordinator: SessionCoordinator,
        private val facade: NiumiCoreFacade,
        private val eventFactory: NfcScanEventFactory,
        private val pairedBoxStore: PairedBoxStore,
    ) : NfcScanHandler {
        /**
         * Clauses de garde séquentielles — session illisible ou absente, état non éligible, payload
         * rejeté, Niumi Spot étranger — avant le dispatch. Même motif que
         * `DefaultSessionCoordinator.dispatchLocked` et `AlarmTriggerHandler.handle` : imbriquer
         * ces refus produirait cinq niveaux d'indentation pour une suite de conditions
         * indépendantes.
         */
        @Suppress("ReturnCount")
        override suspend fun onUriRead(uri: String): ScanOutcome {
            val initial = eligibleSessionOrNull() ?: return ScanOutcome.Ignored

            val parsed = facade.parseBoxPayload(uri)
            val payload = parsed.payload
            if (parsed.status != BoxPayloadStatus.VALID || payload == null) return refusalFor(parsed.status)

            // §11.3 point 2 : un `ARMED` dont l'heure contractuelle est atteinte doit passer par
            // `TRIGGER_ELAPSED` avant le scan, sinon le moteur le refuse
            // (`TRIGGER_ALREADY_ELAPSED`) et un réveil bel et bien survenu finirait `CANCELLED`.
            // La passe pouvant dispatcher, le snapshot est relu et l'éligibilité réévaluée.
            val session =
                if (initial.snapshot.state == SessionStateDto.ARMED) {
                    coordinator.reconcile(ReconcileReason.BEFORE_SCAN)
                    eligibleSessionOrNull() ?: return ScanOutcome.Ignored
                } else {
                    initial
                }

            val context = eventFactory.verificationContext(session.snapshot)
            // Contrat 1.5 : `verifyBox` compare à l'ensemble enregistré au moment du scan — vide
            // compris, auquel cas `NO_PAIRED_BOX_MATCH` revient sans qu'aucun credential n'ait
            // correspondu.
            val credentials = pairedBoxStore.all().map { it.credential }
            val verification = facade.verifyBox(payload, credentials, context)
            val proof = verification.proof
            if (verification.status != BoxVerificationStatus.MATCH || proof == null) {
                return refusalFor(verification.status)
            }

            return when (coordinator.dispatch(eventFactory.validNfcScanned(context, proof))) {
                // Le scan a produit son effet, ou l'avait déjà produit (§11.3, dernier alinéa : le
                // registre retourne le reçu sans rappeler le moteur). Dans les deux cas il est
                // accepté, même si le nettoyage reste partiel : « le scan est accepté, le nettoyage
                // continue ».
                is DispatchResult.Applied, is DispatchResult.Duplicate -> ScanOutcome.Accepted

                // Révision périmée ou transition refusée : rien n'a bougé, et il n'y a rien à
                // annoncer à l'utilisateur — ce n'est pas un mauvais Niumi Spot.
                is DispatchResult.Rejected -> ScanOutcome.Ignored
            }
        }

        /**
         * `null` quand aucun scan ne peut être traité : pas de session, snapshot illisible
         * (SPEC_CORE_KMP §13 — jamais lu comme « pas de session »), ou état hors des quatre
         * sources de §5.1. La garde évite un aller-retour inutile jusqu'au moteur ; l'autorité
         * reste `NfcReducer`, qui refuserait de toute façon.
         */
        private suspend fun eligibleSessionOrNull(): LoadResult.Present? =
            (gateway.load() as? LoadResult.Present)?.takeIf { it.snapshot.state in SCAN_SOURCE_STATES }

        /**
         * Étape 38 : une URI que le parseur refuse n'est pas un Niumi Spot, sauf une version
         * inconnue — c'est un Niumi Spot, que cette version de l'app n'a pas pu enregistrer.
         */
        private fun refusalFor(status: BoxPayloadStatus): ScanOutcome =
            if (status == BoxPayloadStatus.UNSUPPORTED_VERSION) ScanOutcome.UnknownBox else ScanOutcome.NotANiumiSpot

        private fun refusalFor(status: BoxVerificationStatus): ScanOutcome =
            when (status) {
                // §11.2 : « tag Niumi non enregistré → vibration courte d'erreur, alarme
                // maintenue » (contrat 1.4 : un seul statut de refus, `NO_PAIRED_BOX_MATCH`).
                BoxVerificationStatus.NO_PAIRED_BOX_MATCH -> ScanOutcome.UnknownBox

                // Inatteignable derrière un payload `VALID` : `BoxPayloadParser` rejette déjà les
                // versions non prises en charge (§11.2). Même réponse que le parseur, par défense.
                BoxVerificationStatus.UNSUPPORTED_VERSION -> ScanOutcome.UnknownBox

                // `MATCH` sans preuve : impossible, un contexte est toujours fourni ci-dessus.
                BoxVerificationStatus.MATCH -> ScanOutcome.Ignored
            }

        private companion object {
            /** Miroir de `NfcReducer.VALID_SCAN_SOURCE_STATES` (SPEC_CORE_KMP §5.1). */
            val SCAN_SOURCE_STATES: Set<SessionStateDto> =
                setOf(
                    SessionStateDto.ARMED,
                    SessionStateDto.RINGING,
                    SessionStateDto.AWAITING_NFC,
                    SessionStateDto.TRIGGERED_AWAITING_NFC,
                )
        }
    }
