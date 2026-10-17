package com.niumi.feature.setup.pairing

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.database.logging.TechnicalEventType
import com.niumi.database.pairing.PairedBoxDto
import com.niumi.feature.setup.pairing.fakes.FakeNfcReader
import com.niumi.feature.setup.pairing.fakes.FakePairedBoxStore
import com.niumi.feature.setup.pairing.fakes.RecordingTechnicalEventLog
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.session.SessionSnapshotPublisher
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

// Même token que la fixture — seul le `boxId` change : un second boîtier, pas un payload invalide.
private const val OTHER_URI = "niumi://box/v1/00000000-0000-0000-0000-000000000000?token=AAAAAAAAAAAAAAAAAAAAAA"

/**
 * SPEC_ANDROID §11.1 (Lot 8) : jusqu'à cinq boîtiers associés, un doublon est signalé sans
 * écriture, jamais le token en clair. Le ViewModel ne juge jamais lui-même de la validité d'un
 * payload — il délègue à la **vraie** `NiumiCoreFacade` (SPEC_CORE_KMP §9.3), jamais à un parseur
 * simulé. L'écran « Mes boîtiers » complet (renommer, retirer) arrive à l'étape 31 : ici, le
 * dépôt n'ajoute qu'un boîtier à la fois, nommé « Boîtier {n} ».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {
    private val store = FakePairedBoxStore()
    private val log = RecordingTechnicalEventLog()

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

    private fun viewModel(availability: NfcAvailability = NfcAvailability.ENABLED): PairingViewModel =
        PairingViewModel(
            facade = NiumiCoreFacade(),
            pairedBoxStore = store,
            technicalEventLog = log,
            nfcReader = FakeNfcReader(availability),
            // Aucun snapshot publié : aucune session en cours, le parcours est ouvert. La règle
            // elle-même est prouvée sur tous les états par `SetupGateTest`.
            snapshotPublisher = SessionSnapshotPublisher(),
        )

    @Test
    fun aValidTagWithNoExistingBoxIsStoredImmediately() {
        val viewModel = viewModel()

        viewModel.onUriRead(VALID_URI)

        assertThat(store.addCallCount).isEqualTo(1)
        assertThat(
            store.boxes
                .single()
                .credential.boxId,
        ).isEqualTo(VALID_BOX_ID)
        assertThat(store.boxes.single().name).isEqualTo("Boîtier 1")
        assertThat(viewModel.state.pairedBoxIdPrefix).isEqualTo(VALID_BOX_ID.take(PairingTexts.BOX_ID_PREFIX_LENGTH))
        assertThat(viewModel.state.message).isEqualTo(PairingTexts.PAIRED)
        assertThat(log.types).containsExactly(TechnicalEventType.NFC_SCAN_VALID)
    }

    @Test
    fun anInvalidPayloadStoresNothingAndExplainsWhy() {
        val viewModel = viewModel()

        viewModel.onUriRead("https://example.com/not-a-niumi-box")

        assertThat(store.addCallCount).isEqualTo(0)
        assertThat(store.boxes).isEmpty()
        assertThat(viewModel.state.pairedBoxIdPrefix).isNull()
        assertThat(viewModel.state.message).isEqualTo(PairingTexts.UNKNOWN_PAYLOAD)
        assertThat(log.types).containsExactly(TechnicalEventType.NFC_SCAN_INVALID)
    }

    @Test
    fun anUnreadableTagStoresNothingAndExplainsWhy() {
        val viewModel = viewModel()

        viewModel.onUnreadableTag()

        assertThat(store.addCallCount).isEqualTo(0)
        assertThat(viewModel.state.message).isEqualTo(PairingTexts.UNREADABLE)
        assertThat(log.types).containsExactly(TechnicalEventType.NFC_SCAN_INVALID)
    }

    /** Lot 8 : un doublon de `boxId` est signalé, jamais réécrit — plus de dialogue de remplacement. */
    @Test
    fun aValidTagForAnAlreadyPairedBoxShowsAlreadyPairedWithoutWriting() {
        store.boxes = listOf(PairedBoxDto(PairedBoxCredentialDto(1, VALID_BOX_ID, "a".repeat(64)), "Boîtier 1"))
        val viewModel = viewModel()

        viewModel.onUriRead(VALID_URI)

        assertThat(store.addCallCount).isEqualTo(1)
        assertThat(store.boxes).hasSize(1)
        assertThat(viewModel.state.message).isEqualTo(PairingTexts.ALREADY_PAIRED)
    }

    /** Lot 8 : un deuxième boîtier distinct s'ajoute à la liste, nommé par sa position. */
    @Test
    fun aSecondDistinctBoxIsAddedAndNamedByItsPosition() {
        store.boxes =
            listOf(
                PairedBoxDto(
                    PairedBoxCredentialDto(1, "11111111-1111-1111-1111-111111111111", "a".repeat(64)),
                    "Boîtier 1",
                ),
            )
        val viewModel = viewModel()

        viewModel.onUriRead(OTHER_URI)

        assertThat(store.boxes.map { it.name }).containsExactly("Boîtier 1", "Boîtier 2").inOrder()
    }

    /**
     * SPEC_ANDROID §16, SPEC_CORE_KMP §9.2 : ni le token, ni sa forme encodée, ni son empreinte ne
     * doivent apparaître dans le journal technique. L'assertion porte sur le contenu écrit, pas
     * seulement sur le type d'événement.
     */
    @Test
    fun noTokenOrFingerprintEverReachesTheTechnicalLog() {
        val viewModel = viewModel()

        viewModel.onUriRead(VALID_URI)
        viewModel.onUriRead("niumi://box/v1/bad-uuid?token=AAAAAAAAAAAAAAAAAAAAAA")
        viewModel.onUnreadableTag()

        val storedFingerprint =
            store.boxes
                .single()
                .credential.tokenSha256Hex
        assertThat(log.entries).isNotEmpty()
        log.entries.forEach { entry ->
            val details = entry.detailsJson.orEmpty()
            assertThat(details).doesNotContain(storedFingerprint)
            assertThat(details).doesNotContain("AAAAAAAAAAAAAAAAAAAAAA")
            assertThat(details).doesNotContain("token")
        }
    }

    /**
     * Mesuré sur appareil à l'étape 13 : après un tag illisible, couper le NFC laissait afficher
     * « Réessaie en approchant le boîtier plus lentement » alors que plus aucun scan n'est
     * possible — un conseil inapplicable, donc un faux état (§15). Le retour d'un scan précédent
     * ne survit pas à la perte du lecteur.
     */
    @Test
    fun losingTheReaderClearsTheFeedbackOfAPreviousScan() {
        val reader = FakeNfcReader(NfcAvailability.ENABLED)
        val viewModel =
            PairingViewModel(NiumiCoreFacade(), store, log, reader, SessionSnapshotPublisher())
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
        val reader = FakeNfcReader(NfcAvailability.ENABLED)
        val viewModel =
            PairingViewModel(NiumiCoreFacade(), store, log, reader, SessionSnapshotPublisher())
        viewModel.onUnreadableTag()
        reader.availabilityValue = NfcAvailability.DISABLED
        viewModel.refreshAvailability()

        reader.availabilityValue = NfcAvailability.ENABLED
        viewModel.refreshAvailability()

        assertThat(viewModel.state.message).isNull()
        assertThat(viewModel.state.isReaderModeExpected).isTrue()
    }

    /** Une association réussie reste affichée : elle décrit un fait, pas une action à réessayer. */
    @Test
    fun aSuccessfulPairingSurvivesTheReaderComingBack() {
        val reader = FakeNfcReader(NfcAvailability.ENABLED)
        val viewModel =
            PairingViewModel(NiumiCoreFacade(), store, log, reader, SessionSnapshotPublisher())
        viewModel.onUriRead(VALID_URI)

        viewModel.refreshAvailability()

        assertThat(viewModel.state.message).isEqualTo(PairingTexts.PAIRED)
    }

    @Test
    fun aDisabledNfcAdapterIsReportedWithoutStartingAnyScan() {
        val viewModel = viewModel(availability = NfcAvailability.DISABLED)

        assertThat(viewModel.state.nfcAvailability).isEqualTo(NfcAvailability.DISABLED)
        assertThat(viewModel.state.isReaderModeExpected).isFalse()
    }

    @Test
    fun anExistingBoxIsShownOnEntryAsATruncatedIdentifier() {
        store.boxes = listOf(PairedBoxDto(PairedBoxCredentialDto(1, VALID_BOX_ID, "a".repeat(64)), "Boîtier 1"))

        val state = viewModel().state

        assertThat(state.pairedBoxIdPrefix).isEqualTo(VALID_BOX_ID.take(PairingTexts.BOX_ID_PREFIX_LENGTH))
        assertThat(state.canContinue).isTrue()
    }
}
