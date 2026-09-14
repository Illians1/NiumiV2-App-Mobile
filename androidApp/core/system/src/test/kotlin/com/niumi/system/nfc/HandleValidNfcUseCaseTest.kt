package com.niumi.system.nfc

import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.IncidentCodes
import com.niumi.core.interop.SessionEventDto
import com.niumi.core.interop.SessionEventKindDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.EventReceipt
import com.niumi.system.common.IdGenerator
import com.niumi.system.common.OperationResult
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.LoadResult
import com.niumi.system.session.ReconcileReason
import com.niumi.system.session.ReconcileResult
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.fakes.NfcScanFixtures
import com.niumi.system.session.fakes.SequentialIdGenerator
import com.niumi.system.session.fakes.SessionDtoFixtures
import com.niumi.system.session.fakes.TestCoordinatorHarness
import com.niumi.system.session.fakes.persistSession
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * SPEC_ANDROID §11.3, points 1 à 4 : éligibilité de l'état, réconciliation avant scan, vérification
 * du boîtier **figé dans la session**, construction de l'événement porteur de la preuve. Les points
 * 5 à 14 appartiennent au coordinateur et à ses exécuteurs, déjà couverts depuis l'étape 11 ; ce
 * test vérifie ce que le cas d'usage ajoute et comment il interprète le [DispatchResult].
 */
class HandleValidNfcUseCaseTest {
    /**
     * [IdGenerator] partagé avec le harnais, comme en production où Hilt n'en fournit qu'un :
     * deux générateurs séquentiels indépendants produiraient le même premier identifiant, et
     * l'événement de suivi `RELEASE_SUCCEEDED` entrerait en `EVENT_ID_CONFLICT` avec le scan.
     */
    private fun TestCoordinatorHarness.useCase() =
        HandleValidNfcUseCase(
            gateway = gateway,
            coordinator = coordinator,
            facade = facade,
            eventFactory = NfcScanEventFactory(idGenerator, clock),
        )

    private fun TestCoordinatorHarness.sessionExtras(uri: String = NfcScanFixtures.SESSION_BOX_URI) =
        NfcScanFixtures.extrasFor(facade, uri)

    /**
     * État tel que persisté. `null` une fois la session terminée : `CLEAR_ACTIVE_SESSION` efface
     * le pointeur actif (§11.3 point 14) — pour un état final, lire [finalState].
     */
    private suspend fun TestCoordinatorHarness.storedState(): SessionStateDto? =
        (gateway.load() as? LoadResult.Present)?.snapshot?.state

    /** Dernier état publié à l'interface (`PUBLISH_PLATFORM_SNAPSHOT`), pointeur effacé ou non. */
    private fun TestCoordinatorHarness.finalState(): SessionStateDto? = publisher.snapshot.value?.state

    // --- 1. États non éligibles : aucun dispatch, aucun effet ---------------------------------

    /**
     * §5.2 : `VALID_NFC_SCANNED` est refusé dans `PREPARING` et dans tout état final. La garde
     * évite un aller-retour inutile jusqu'au moteur ; elle ne duplique pas la règle, qui reste
     * dans `NfcReducer`.
     */
    @Test
    fun anIneligibleStateIsIgnoredWithoutDispatching() =
        runTest {
            listOf(
                SessionStateDto.PREPARING,
                SessionStateDto.COMPLETED,
                SessionStateDto.CANCELLED,
                SessionStateDto.FAILED,
            ).forEach { state ->
                val harness = TestCoordinatorHarness()
                harness.persistSession(state, extras = harness.sessionExtras())
                val before = harness.journal.calls.size

                val outcome = harness.useCase().onUriRead(NfcScanFixtures.SESSION_BOX_URI)

                assertThat(outcome).isEqualTo(ScanOutcome.Ignored)
                assertThat(harness.journal.calls.drop(before)).isEmpty()
                assertThat(harness.storedState()).isEqualTo(state)
            }
        }

    @Test
    fun noSessionAtAllIsIgnored() =
        runTest {
            val harness = TestCoordinatorHarness()

            val outcome = harness.useCase().onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.Ignored)
            assertThat(harness.journal.calls).isEmpty()
        }

    /**
     * SPEC_CORE_KMP §13 : un snapshot illisible ne se lit jamais « pas de session ». Sans révision
     * fiable, aucun événement ne peut être construit — le scan est ignoré, jamais accepté.
     */
    @Test
    fun anUnreadableSnapshotIsIgnored() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.RINGING, extras = harness.sessionExtras())
            harness.gateway.forceUnreadable = "snapshot corrompu"

            val outcome = harness.useCase().onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.Ignored)
        }

    // --- 2. Réconciliation avant le scan (§11.3 point 2) ---------------------------------------

    /** `ARMED` avant l'heure : annulation, donc `RELEASING`/`CANCELLED` puis `CANCELLED`. */
    @Test
    fun armedBeforeTheHourCancelsTheSession() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.ARMED, extras = harness.sessionExtras())

            val outcome = harness.useCase().onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.Accepted)
            assertThat(harness.finalState()).isEqualTo(SessionStateDto.CANCELLED)
        }

    /**
     * `ARMED` après l'heure sans alarme observée : `TRIGGER_ELAPSED` d'abord (§5.1), sinon le
     * moteur refuserait le scan par `TRIGGER_ALREADY_ELAPSED` et la session finirait `CANCELLED`
     * alors que le réveil a bel et bien eu lieu.
     */
    @Test
    fun armedAfterTheHourCompletesRatherThanCancels() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.ARMED, extras = harness.sessionExtras())
            harness.clock.now = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS + 1_000L

            val outcome = harness.useCase().onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.Accepted)
            assertThat(harness.finalState()).isEqualTo(SessionStateDto.COMPLETED)
        }

    /**
     * **Défaut mesuré sur appareil le 2026-09-14, corrigé dans `SessionReconciler`.** Un service
     * d'accessibilité coupé pendant une session armée faisait interrompre la passe `BEFORE_SCAN`
     * par la garde de permission de l'étape 12 : plus de `TRIGGER_ELAPSED`, donc un `ARMED` après
     * l'heure, que le moteur refuse (`TRIGGER_ALREADY_ELAPSED`). Le scan était alors traduit en
     * `Ignored` **sans rien afficher**, et l'utilisateur ne pouvait plus terminer sa session — le
     * seul chemin de sortie du produit (§11.2). Le scan doit aboutir quelles que soient les
     * permissions perdues.
     */
    @Test
    fun aLostAccessibilityPermissionStillLetsTheScanEndTheSession() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.ARMED, extras = harness.sessionExtras())
            harness.clock.now = SessionDtoFixtures.TRIGGER_AT_EPOCH_MILLIS + 60_000L
            harness.accessibilityServiceStatus.enabled = false
            harness.blockingController.serviceEnabled = false
            harness.blockingController.removeResult = OperationResult.AlreadySatisfied

            val outcome = harness.useCase().onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.Accepted)
            assertThat(harness.finalState()).isEqualTo(SessionStateDto.COMPLETED)
        }

    @Test
    fun ringingAndScanAwaitingStatesComplete() =
        runTest {
            listOf(
                SessionStateDto.RINGING,
                SessionStateDto.AWAITING_NFC,
                SessionStateDto.TRIGGERED_AWAITING_NFC,
            ).forEach { state ->
                val harness = TestCoordinatorHarness()
                harness.persistSession(state, extras = harness.sessionExtras())

                val outcome = harness.useCase().onUriRead(NfcScanFixtures.SESSION_BOX_URI)

                assertThat(outcome).isEqualTo(ScanOutcome.Accepted)
                assertThat(harness.finalState()).isEqualTo(SessionStateDto.COMPLETED)
            }
        }

    /** La réconciliation d'avant-scan ne concerne que `ARMED` (§11.3 point 2). */
    @Test
    fun onlyArmedReconcilesBeforeScanning() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.RINGING, extras = harness.sessionExtras())
            val coordinator = ProgrammableCoordinator()

            harness.useCaseWith(coordinator).onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            assertThat(coordinator.reconcileReasons).isEmpty()
        }

    @Test
    fun armedReconcilesWithBeforeScanReason() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.ARMED, extras = harness.sessionExtras())
            val coordinator = ProgrammableCoordinator()

            harness.useCaseWith(coordinator).onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            assertThat(coordinator.reconcileReasons).containsExactly(ReconcileReason.BEFORE_SCAN)
        }

    // --- 3. Vérification contre le boîtier figé dans la session (§11.3 point 3) -----------------

    /**
     * Le boîtier de référence est celui de la session, jamais `PairedBoxEntity` : une nouvelle
     * association pendant la session ne doit pas ouvrir une sortie que l'utilisateur n'avait pas
     * au moment de s'engager. Le cas d'usage n'injecte pas `PairedBoxStore` du tout ; ce test
     * l'observe de l'extérieur.
     */
    @Test
    fun aTagMatchingANewlyPairedBoxIsRefused() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.RINGING, extras = harness.sessionExtras())

            val outcome = harness.useCase().onUriRead(NfcScanFixtures.OTHER_BOX_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.UnknownBox)
            assertThat(harness.storedState()).isEqualTo(SessionStateDto.RINGING)
        }

    @Test
    fun theSameBoxWithAnotherTokenIsRefused() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.RINGING, extras = harness.sessionExtras())

            val outcome = harness.useCase().onUriRead(NfcScanFixtures.SESSION_BOX_WRONG_TOKEN_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.UnknownBox)
            assertThat(harness.storedState()).isEqualTo(SessionStateDto.RINGING)
        }

    /** Payload rejeté par le parseur commun : ni état, ni révision, ni effet ne bougent (§4). */
    @Test
    fun aMalformedPayloadLeavesEverythingUntouched() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.RINGING, revision = 3, extras = harness.sessionExtras())
            val before = harness.journal.calls.size

            val outcome = harness.useCase().onUriRead(NfcScanFixtures.MALFORMED_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.Unreadable)
            assertThat(harness.journal.calls.drop(before)).isEmpty()
            val present = harness.gateway.load() as LoadResult.Present
            assertThat(present.snapshot.state).isEqualTo(SessionStateDto.RINGING)
            assertThat(present.snapshot.revision).isEqualTo(3)
        }

    // --- 4. Événement construit (§11.3 point 4) ------------------------------------------------

    /** La preuve lie l'événement : même `eventId`, même révision attendue, même horodatage. */
    @Test
    fun theProofIsBoundToTheDispatchedEvent() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.RINGING, revision = 4, extras = harness.sessionExtras())
            harness.clock.now = 1_700_000_000_000L
            val coordinator = ProgrammableCoordinator()

            harness.useCaseWith(coordinator).onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            val event = coordinator.dispatched.single()
            assertThat(event.kind).isEqualTo(SessionEventKindDto.VALID_NFC_SCANNED)
            assertThat(event.expectedRevision).isEqualTo(4)
            assertThat(event.occurredAtEpochMillis).isEqualTo(1_700_000_000_000L)
            val proof = checkNotNull(event.nfcProof)
            assertThat(proof.eventId).isEqualTo(event.eventId)
            assertThat(proof.sessionId).isEqualTo(SessionDtoFixtures.SESSION_ID)
            assertThat(proof.expectedRevision).isEqualTo(4)
            assertThat(proof.verifiedAtEpochMillis).isEqualTo(1_700_000_000_000L)
            assertThat(proof.boxId).isEqualTo(SessionDtoFixtures.BOX_ID)
        }

    /**
     * §11.3, dernier alinéa : « le registre retourne le reçu sans rappeler le moteur ». Du point de
     * vue du scan, l'événement a bien été pris en compte — répondre `Ignored` ferait vibrer
     * l'appareil comme pour un boîtier étranger.
     */
    @Test
    fun aDuplicateDispatchStillCountsAsAccepted() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.RINGING, extras = harness.sessionExtras())
            val coordinator =
                ProgrammableCoordinator(
                    DispatchResult.Duplicate(
                        EventReceipt(
                            eventId = "00000000-0000-0000-0000-00000000000a",
                            sessionId = SessionDtoFixtures.SESSION_ID,
                            payloadSha256Hex = "d".repeat(64),
                            appliedRevision = 3,
                            receivedAtEpochMillis = 1_000L,
                        ),
                    ),
                )

            val outcome = harness.useCaseWith(coordinator).onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.Accepted)
        }

    /** Un refus du moteur (révision périmée, transition illégale) ne fait rien avancer. */
    @Test
    fun aRejectedDispatchIsIgnored() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.RINGING, extras = harness.sessionExtras())
            val coordinator = ProgrammableCoordinator(DispatchResult.Rejected(emptyList()))

            val outcome = harness.useCaseWith(coordinator).onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.Ignored)
        }

    // --- Nettoyage partiel (§11.3, effets requis) ----------------------------------------------

    /**
     * « Si un effet requis échoue alors que sa précondition tient toujours, envoyer
     * `RELEASE_FAILED` [...] et conserver `RELEASING` » — mais le scan, lui, est accepté : il a eu
     * lieu, et le nettoyage continue par la réconciliation.
     */
    @Test
    fun aFailedRemoveBlockingKeepsReleasingButAcceptsTheScan() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.RINGING, extras = harness.sessionExtras())
            harness.blockingController.removeResult = OperationResult.Failure("REMOVE_FAILED")

            val outcome = harness.useCase().onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.Accepted)
            assertThat(harness.storedState()).isEqualTo(SessionStateDto.RELEASING)
        }

    /**
     * Règle d'échappement (§11.3) : service d'accessibilité déjà coupé par l'utilisateur → le
     * retrait est satisfait, un incident `BLOCKING_PERMISSION_REVOKED` est consigné, et
     * `RELEASING` ne reste pas bloqué.
     */
    @Test
    fun anAlreadyDisabledAccessibilityServiceStillReleases() =
        runTest {
            val harness = TestCoordinatorHarness()
            harness.persistSession(SessionStateDto.RINGING, extras = harness.sessionExtras())
            harness.blockingController.removeResult = OperationResult.AlreadySatisfied
            harness.blockingController.serviceEnabled = false

            val outcome = harness.useCase().onUriRead(NfcScanFixtures.SESSION_BOX_URI)

            assertThat(outcome).isEqualTo(ScanOutcome.Accepted)
            assertThat(harness.finalState()).isEqualTo(SessionStateDto.COMPLETED)
            assertThat(harness.gateway.incidentsRecorded.map { it.second.code })
                .contains(IncidentCodes.BLOCKING_PERMISSION_REVOKED)
        }

    private fun TestCoordinatorHarness.useCaseWith(coordinator: SessionCoordinator) =
        HandleValidNfcUseCase(
            gateway = gateway,
            coordinator = coordinator,
            facade = facade,
            eventFactory = NfcScanEventFactory(SequentialIdGenerator(), clock),
        )
}

/**
 * Coordinateur programmable : isole ce que le cas d'usage **envoie** et comment il **interprète**
 * la réponse, sans faire tourner le moteur. Les scénarios d'état réels utilisent au contraire le
 * vrai coordinateur du harnais.
 */
private class ProgrammableCoordinator(
    private val result: DispatchResult = DispatchResult.Applied(snapshot = null, requiredEffectsSucceeded = true),
) : SessionCoordinator {
    val dispatched = mutableListOf<SessionEventDto>()
    val reconcileReasons = mutableListOf<ReconcileReason>()

    override suspend fun dispatch(
        event: SessionEventDto,
        extras: AndroidSessionExtras?,
    ): DispatchResult {
        dispatched += event
        return result
    }

    override suspend fun reconcile(reason: ReconcileReason): ReconcileResult {
        reconcileReasons += reason
        return ReconcileResult(sessionId = null, actions = emptyList())
    }
}
