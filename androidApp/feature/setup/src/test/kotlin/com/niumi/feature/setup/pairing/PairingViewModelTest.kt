package com.niumi.feature.setup.pairing

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.logging.TechnicalEventType
import com.niumi.database.pairing.PairedBoxDto
import com.niumi.feature.setup.pairing.fakes.FakeNfcReader
import com.niumi.feature.setup.pairing.fakes.FakePairedBoxStore
import com.niumi.feature.setup.pairing.fakes.RecordingTechnicalEventLog
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.session.SessionSnapshotPublisher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

// Fixture reprise de shared/core/src/commonTest/resources/fixtures/nfc_payloads.json.
private const val VALID_URI = "niumi://box/v1/550e8400-e29b-41d4-a716-446655440000?token=AAAAAAAAAAAAAAAAAAAAAA"
private const val VALID_BOX_ID = "550e8400-e29b-41d4-a716-446655440000"

// Même token que la fixture — seul le `boxId` change : un second Niumi Spot, pas un payload invalide.
private const val OTHER_URI = "niumi://box/v1/00000000-0000-0000-0000-000000000000?token=AAAAAAAAAAAAAAAAAAAAAA"
private const val OTHER_BOX_ID = "00000000-0000-0000-0000-000000000000"

private fun box(
    index: Int,
    name: String = "Niumi Spot $index",
): PairedBoxDto =
    PairedBoxDto(
        PairedBoxCredentialDto(1, "${index.toString().repeat(8)}-1111-1111-1111-111111111111", "a".repeat(64)),
        name,
    )

/**
 * Écran 3 « Mes Niumi Spots » (SPEC_ANDROID §11.1, §15 Lot 8). Le ViewModel ne juge jamais lui-même
 * de la validité d'un payload — il délègue à la **vraie** `NiumiCoreFacade` (SPEC_CORE_KMP §9.3),
 * jamais à un parseur simulé — ni de celle d'un nom : `PairedBoxName.sanitize`, celle du dépôt,
 * tranche avant tout appel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {
    private val store = FakePairedBoxStore()
    private val log = RecordingTechnicalEventLog()
    private val reader = FakeNfcReader()

    @Before
    fun setUp() {
        // `viewModelScope` poste sur `Dispatchers.Main` ; `Unconfined` rend l'appel synchrone,
        // l'état est donc lisible dès le retour de `onUriRead`.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        availability: NfcAvailability = NfcAvailability.ENABLED,
        // Par défaut, aucun snapshot publié : aucune session en cours, le parcours est ouvert. La
        // règle elle-même est prouvée sur tous les états par `SetupGateTest`.
        snapshotPublisher: SessionSnapshotPublisher = SessionSnapshotPublisher(),
    ): PairingViewModel {
        reader.availabilityValue = availability
        return PairingViewModel(
            facade = NiumiCoreFacade(),
            pairedBoxStore = store,
            technicalEventLog = log,
            nfcReader = reader,
            snapshotPublisher = snapshotPublisher,
        )
    }

    private fun armedSnapshot() =
        SessionSnapshotDto(
            schemaVersion = 3,
            revision = 2,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule = WakeScheduleDto("2026-10-25", "07:00", "Europe/Paris", 2_000_000_000_000L),
            state = SessionStateDto.ARMED,
            releaseTarget = null,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = 1_000L,
            armedAtEpochMillis = 1_100L,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
        )

    /** Scanne un Niumi Spot neuf et valide le nom proposé. */
    private fun PairingViewModel.addScanned(uri: String) {
        if (!state.isScanning) startAdding()
        onUriRead(uri)
        editor.confirmAdd()
    }

    @Test
    fun onEntryTheListShowsEveryPairedBoxByNameAndPrefixInOrder() {
        store.boxes = listOf(box(1, "Salle de bain"), box(2, "Cuisine"))

        val rows = viewModel().state.boxes

        assertThat(rows.map { it.name }).containsExactly("Salle de bain", "Cuisine").inOrder()
        assertThat(rows.map { it.prefix }).containsExactly("11111111…", "22222222…").inOrder()
        assertThat(viewModel().state.canContinue).isTrue()
    }

    /** Décision du 2026-10-17 : la lecture n'est active d'office qu'à la première association. */
    @Test
    fun theReaderIsOnAtEntryOnlyWithoutAnyBox() {
        assertThat(viewModel().state.isReaderModeExpected).isTrue()

        store.boxes = listOf(box(1))

        assertThat(viewModel().state.isReaderModeExpected).isFalse()
    }

    @Test
    fun addStartsTheReaderAndCancelStopsIt() {
        store.boxes = listOf(box(1))
        val viewModel = viewModel()

        viewModel.startAdding()
        assertThat(viewModel.state.isReaderModeExpected).isTrue()

        viewModel.cancelScan()
        assertThat(viewModel.state.isReaderModeExpected).isFalse()
    }

    @Test
    fun aValidNewTagOpensTheNamePromptPrefilledWithNiumiSpotN() {
        store.boxes = listOf(box(1), box(2))
        val viewModel = viewModel()
        viewModel.startAdding()

        viewModel.onUriRead(VALID_URI)

        assertThat(viewModel.state.pendingName).isEqualTo("Niumi Spot 3")
        assertThat(viewModel.state.isReaderModeExpected).isFalse()
        // Rien n'est écrit avant la confirmation du nom.
        assertThat(store.writeCallCount).isEqualTo(0)
        assertThat(log.types).containsExactly(TechnicalEventType.NFC_SCAN_VALID)
    }

    @Test
    fun confirmingTheNameAddsTheBoxAndSaysNiumiSpotAjoute() {
        val viewModel = viewModel()
        viewModel.onUriRead(VALID_URI)
        viewModel.editor.onNameChanged("  Cuisine ")

        viewModel.editor.confirmAdd()

        assertThat(
            store.boxes
                .single()
                .credential.boxId,
        ).isEqualTo(VALID_BOX_ID)
        assertThat(store.boxes.single().name).isEqualTo("Cuisine")
        assertThat(viewModel.state.pendingName).isNull()
        assertThat(viewModel.state.boxes.map { it.name }).containsExactly("Cuisine")
        assertThat(viewModel.state.message).isEqualTo(PairingTexts.BOX_ADDED)
    }

    @Test
    fun aBlankOrTooLongNameIsRefusedWithoutWriting() {
        val viewModel = viewModel()
        viewModel.onUriRead(VALID_URI)

        viewModel.editor.onNameChanged("   ")
        assertThat(viewModel.state.canConfirmName).isFalse()
        viewModel.editor.confirmAdd()

        viewModel.editor.onNameChanged("a".repeat(31))
        assertThat(viewModel.state.canConfirmName).isFalse()
        viewModel.editor.confirmAdd()

        assertThat(store.writeCallCount).isEqualTo(0)
        assertThat(viewModel.state.pendingName).isEqualTo("a".repeat(31))
    }

    @Test
    fun cancellingTheNamePromptWritesNothing() {
        val viewModel = viewModel()
        viewModel.onUriRead(VALID_URI)

        viewModel.editor.cancelAdd()

        assertThat(store.writeCallCount).isEqualTo(0)
        assertThat(viewModel.state.pendingName).isNull()
        assertThat(viewModel.state.boxes).isEmpty()
    }

    /** Lot 8 : un doublon de `boxId` est signalé, sans dialogue ni écriture. */
    @Test
    fun aTagAlreadyPairedSaysSoAndWritesNothing() {
        store.boxes = listOf(PairedBoxDto(PairedBoxCredentialDto(1, VALID_BOX_ID, "a".repeat(64)), "Niumi Spot 1"))
        val viewModel = viewModel()
        viewModel.startAdding()

        viewModel.onUriRead(VALID_URI)

        assertThat(store.writeCallCount).isEqualTo(0)
        assertThat(viewModel.state.pendingName).isNull()
        assertThat(viewModel.state.message).isEqualTo(PairingTexts.ALREADY_PAIRED)
    }

    @Test
    fun nineBoxesStillAllowAdding() {
        store.boxes = (1..9).map { box(it) }

        assertThat(viewModel().state.canAdd).isTrue()
    }

    @Test
    fun tenBoxesHideAddAndShowTheMaximumMessage() {
        // Contrat KMP 1.5 : dix Niumi Spots au plus (`PairedBoxLimit.MAX`).
        store.boxes = (1..10).map { box(it) }
        val viewModel = viewModel()

        viewModel.startAdding()

        assertThat(viewModel.state.isAtLimit).isTrue()
        assertThat(viewModel.state.canAdd).isFalse()
        assertThat(viewModel.state.isReaderModeExpected).isFalse()
    }

    @Test
    fun renameWritesTheNewNameAndRefreshesTheList() {
        store.boxes = listOf(box(1), box(2))
        val viewModel = viewModel()
        val second = viewModel.state.boxes[1].boxId

        viewModel.editor.startRename(second)
        assertThat(viewModel.state.renaming?.text).isEqualTo("Niumi Spot 2")
        viewModel.editor.onNameChanged(" Cuisine ")
        viewModel.editor.confirmRename()

        assertThat(store.boxes.map { it.name }).containsExactly("Niumi Spot 1", "Cuisine").inOrder()
        assertThat(viewModel.state.boxes.map { it.name }).containsExactly("Niumi Spot 1", "Cuisine").inOrder()
        assertThat(viewModel.state.renaming).isNull()
    }

    @Test
    fun aBlankRenameIsRefusedWithoutWriting() {
        store.boxes = listOf(box(1))
        val viewModel = viewModel()
        viewModel.editor.startRename(
            viewModel.state.boxes
                .single()
                .boxId,
        )

        viewModel.editor.onNameChanged(" ")
        viewModel.editor.confirmRename()

        assertThat(store.writeCallCount).isEqualTo(0)
        assertThat(viewModel.state.renaming).isNotNull()
    }

    @Test
    fun removeDropsTheBoxAndRefreshesTheList() {
        store.boxes = listOf(box(1), box(2), box(3))
        val viewModel = viewModel()

        viewModel.editor.remove(viewModel.state.boxes[1].boxId)

        assertThat(store.boxes.map { it.name }).containsExactly("Niumi Spot 1", "Niumi Spot 3").inOrder()
        assertThat(viewModel.state.boxes.map { it.name }).containsExactly("Niumi Spot 1", "Niumi Spot 3").inOrder()
    }

    /** Décision du 2026-10-17 : le trou laissé par un retrait est comblé en premier. */
    @Test
    fun afterRemovingNiumiSpot2TheNextProposalIsNiumiSpot2() {
        store.boxes = listOf(box(1), box(2), box(3))
        val viewModel = viewModel()
        viewModel.editor.remove(viewModel.state.boxes[1].boxId)
        viewModel.startAdding()

        viewModel.onUriRead(OTHER_URI)

        assertThat(viewModel.state.pendingName).isEqualTo("Niumi Spot 2")
    }

    /**
     * Décision Q22 (étape 43) : il reste toujours au moins un Niumi Spot. Le dépôt refuse le dernier
     * (`LastBoxKept`) et l'écran ne propose pas de le retirer.
     */
    @Test
    fun removingTheLastBoxIsRefusedAndKeepsIt() {
        store.boxes = listOf(box(1))
        val viewModel = viewModel()

        assertThat(viewModel.state.canRemove).isFalse()
        viewModel.editor.remove(
            viewModel.state.boxes
                .single()
                .boxId,
        )

        assertThat(store.boxes).hasSize(1)
        assertThat(viewModel.state.boxes).hasSize(1)
        assertThat(viewModel.state.canContinue).isTrue()
    }

    @Test
    fun withTwoBoxesEitherCanBeRemoved() {
        store.boxes = listOf(box(1), box(2))

        assertThat(viewModel().state.canRemove).isTrue()
    }

    /**
     * Étape 43 (SPEC_CORE_KMP §2, décisions 11 et 16) : en session, les Niumi Spots s'ajoutent, se
     * renomment et se retirent ; seul « Continuer » disparaît, la mise en route n'ayant rien à
     * préparer pendant une session (décision de Mehdi du 2026-10-24 : l'écran sert alors à gérer la liste).
     */
    @Test
    fun aSessionInProgressStillAllowsAddRenameAndRemoveButHidesContinue() {
        store.boxes = listOf(box(1), box(2))
        val viewModel = viewModel()
        viewModel.onSessionStateChanged(isSessionInProgress = true)

        viewModel.startAdding()
        assertThat(viewModel.state.isReaderModeExpected).isTrue()
        viewModel.onUriRead(OTHER_URI)
        viewModel.editor.confirmAdd()
        val first =
            viewModel.state.boxes
                .first()
                .boxId
        viewModel.editor.startRename(first)
        viewModel.editor.onNameChanged("Cuisine")
        viewModel.editor.confirmRename()
        viewModel.editor.remove(viewModel.state.boxes[1].boxId)

        assertThat(store.boxes.map { it.name }).containsExactly("Cuisine", "Niumi Spot 3").inOrder()
        assertThat(viewModel.state.canAdd).isTrue()
        assertThat(viewModel.state.canContinue).isFalse()
    }

    /**
     * Mesuré sur appareil le 2026-10-24 (étape 43) : ouvert pendant une session armée, l'écran
     * montrait « Continuer ». La lecture des Niumi Spots au démarrage suspend ; la session publiée
     * pendant cette suspension était écrasée par la copie de l'état d'avant la lecture.
     */
    @Test
    fun aSessionPublishedWhileTheBoxesAreReadStillHidesContinue() {
        store.boxes = listOf(box(1))
        val gate = CompletableDeferred<Unit>()
        store.allGate = gate
        val publisher = SessionSnapshotPublisher()
        publisher.publish(armedSnapshot())

        val viewModel = viewModel(snapshotPublisher = publisher)
        gate.complete(Unit)

        assertThat(viewModel.state.boxes).hasSize(1)
        assertThat(viewModel.state.isSessionInProgress).isTrue()
        assertThat(viewModel.state.canContinue).isFalse()
    }

    /** Une session qui démarre pendant le dialogue du nom ne l'interrompt plus : l'ajout aboutit. */
    @Test
    fun aSessionStartingWhileANameDialogIsOpenLetsTheAddComplete() {
        val viewModel = viewModel()
        viewModel.onUriRead(VALID_URI)

        viewModel.onSessionStateChanged(isSessionInProgress = true)
        viewModel.editor.confirmAdd()

        assertThat(store.boxes.map { it.credential.boxId }).containsExactly(VALID_BOX_ID)
        assertThat(viewModel.state.pendingName).isNull()
    }

    @Test
    fun aScanWhileANameDialogIsOpenIsIgnored() {
        val viewModel = viewModel()
        viewModel.onUriRead(VALID_URI)

        viewModel.onUriRead(OTHER_URI)
        viewModel.editor.confirmAdd()

        assertThat(
            store.boxes
                .single()
                .credential.boxId,
        ).isEqualTo(VALID_BOX_ID)
    }

    /**
     * Un tag tenu au bord de la zone émet plusieurs lectures (même constat qu'`ScanToModifyViewModel`).
     * Les deux passent la garde avant que la lecture du dépôt rende la main : seule la première doit
     * ouvrir le dialogue et journaliser le scan.
     */
    @Test
    fun twoReadsOfTheSameTagWhileTheStoreIsReadOpenOneDialogAndLogOnce() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        store.allGate = gate

        viewModel.onUriRead(VALID_URI)
        viewModel.onUriRead(OTHER_URI)
        gate.complete(Unit)
        viewModel.editor.confirmAdd()

        assertThat(log.types).containsExactly(TechnicalEventType.NFC_SCAN_VALID)
        assertThat(
            store.boxes
                .single()
                .credential.boxId,
        ).isEqualTo(VALID_BOX_ID)
    }

    /** Un renommage ouvert pendant la lecture du dépôt l'emporte : jamais deux dialogues superposés. */
    @Test
    fun aRenameOpenedWhileAScanReadsTheStoreIsNotCoveredByANameDialog() {
        store.boxes = listOf(box(1))
        val viewModel = viewModel()
        viewModel.startAdding()
        val gate = CompletableDeferred<Unit>()
        store.allGate = gate

        viewModel.onUriRead(VALID_URI)
        viewModel.editor.startRename(
            viewModel.state.boxes
                .single()
                .boxId,
        )
        gate.complete(Unit)

        assertThat(viewModel.state.renaming).isNotNull()
        assertThat(viewModel.state.pendingName).isNull()
    }

    @Test
    fun anInvalidPayloadStoresNothingAndExplainsWhy() {
        val viewModel = viewModel()

        viewModel.onUriRead("https://example.com/not-a-niumi-box")

        assertThat(store.writeCallCount).isEqualTo(0)
        assertThat(viewModel.state.pendingName).isNull()
        assertThat(viewModel.state.message).isEqualTo(PairingTexts.UNKNOWN_PAYLOAD)
        assertThat(log.types).containsExactly(TechnicalEventType.NFC_SCAN_INVALID)
    }

    @Test
    fun anUnreadableTagStoresNothingAndExplainsWhy() {
        val viewModel = viewModel()

        viewModel.onUnreadableTag()

        assertThat(store.writeCallCount).isEqualTo(0)
        assertThat(viewModel.state.message).isEqualTo(PairingTexts.UNREADABLE)
        assertThat(log.types).containsExactly(TechnicalEventType.NFC_SCAN_INVALID)
    }

    @Test
    fun aSecondDistinctBoxIsAddedAfterTheFirst() {
        val viewModel = viewModel()

        viewModel.addScanned(VALID_URI)
        viewModel.addScanned(OTHER_URI)

        assertThat(store.boxes.map { it.credential.boxId }).containsExactly(VALID_BOX_ID, OTHER_BOX_ID).inOrder()
        assertThat(store.boxes.map { it.name }).containsExactly("Niumi Spot 1", "Niumi Spot 2").inOrder()
    }

    /**
     * SPEC_ANDROID §16, SPEC_CORE_KMP §9.2 : ni le token, ni sa forme encodée, ni son empreinte ne
     * doivent apparaître dans le journal technique. L'assertion porte sur le contenu écrit, pas
     * seulement sur le type d'événement.
     */
    @Test
    fun noTokenOrFingerprintEverReachesTheTechnicalLog() {
        val viewModel = viewModel()

        viewModel.addScanned(VALID_URI)
        viewModel.startAdding()
        viewModel.onUriRead("niumi://box/v1/bad-uuid?token=AAAAAAAAAAAAAAAAAAAAAA")
        viewModel.onUnreadableTag()

        val storedFingerprint =
            store.boxes
                .single()
                .credential.tokenSha256Hex
        assertThat(log.types)
            .containsExactly(
                TechnicalEventType.NFC_SCAN_VALID,
                TechnicalEventType.NFC_SCAN_INVALID,
                TechnicalEventType.NFC_SCAN_INVALID,
            )
        log.entries.forEach { entry ->
            val details = entry.detailsJson.orEmpty()
            assertThat(details).doesNotContain(storedFingerprint)
            assertThat(details).doesNotContain("AAAAAAAAAAAAAAAAAAAAAA")
            assertThat(details).doesNotContain("token")
        }
    }

    /** L'état d'affichage ne porte ni empreinte ni token : l'écran ne doit rien pouvoir en montrer (§16). */
    @Test
    fun theUiStateNeverCarriesAFingerprint() {
        val viewModel = viewModel()
        viewModel.addScanned(VALID_URI)

        assertThat(viewModel.state.toString()).doesNotContain(
            store.boxes
                .single()
                .credential.tokenSha256Hex,
        )
    }

    /**
     * Mesuré sur appareil à l'étape 13 : après un tag illisible, couper le NFC laissait afficher
     * « Réessaie en approchant le Niumi Spot plus lentement » alors que plus aucun scan n'est
     * possible — un conseil inapplicable, donc un faux état (§15). Le retour d'un scan précédent
     * ne survit pas à la perte du lecteur.
     */
    @Test
    fun losingTheReaderClearsTheFeedbackOfAPreviousScan() {
        val viewModel = viewModel()
        viewModel.onUnreadableTag()
        assertThat(viewModel.state.message).isEqualTo(PairingTexts.UNREADABLE)

        reader.availabilityValue = NfcAvailability.DISABLED
        viewModel.refreshAvailability()

        assertThat(viewModel.state.message).isNull()
        assertThat(viewModel.state.nfcAvailability).isEqualTo(NfcAvailability.DISABLED)
    }

    /** Le retour du lecteur ne réaffiche pas le message effacé : il n'a plus de raison d'être. */
    @Test
    fun recoveringTheReaderDoesNotBringBackTheClearedFeedback() {
        val viewModel = viewModel()
        viewModel.onUnreadableTag()
        reader.availabilityValue = NfcAvailability.DISABLED
        viewModel.refreshAvailability()

        reader.availabilityValue = NfcAvailability.ENABLED
        viewModel.refreshAvailability()

        assertThat(viewModel.state.message).isNull()
        assertThat(viewModel.state.isReaderModeExpected).isTrue()
    }

    /** Un ajout réussi reste affiché : il décrit un fait, pas une action à réessayer. */
    @Test
    fun aSuccessfulAdditionSurvivesTheReaderGoingAway() {
        val viewModel = viewModel()
        viewModel.addScanned(VALID_URI)

        reader.availabilityValue = NfcAvailability.DISABLED
        viewModel.refreshAvailability()

        assertThat(viewModel.state.message).isEqualTo(PairingTexts.BOX_ADDED)
    }

    @Test
    fun aDisabledNfcAdapterIsReportedWithoutStartingAnyScan() {
        val viewModel = viewModel(availability = NfcAvailability.DISABLED)

        assertThat(viewModel.state.nfcAvailability).isEqualTo(NfcAvailability.DISABLED)
        assertThat(viewModel.state.isReaderModeExpected).isFalse()
    }
}
