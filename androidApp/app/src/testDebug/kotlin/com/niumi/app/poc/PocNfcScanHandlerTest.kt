package com.niumi.app.poc

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.system.common.OperationResult
import com.niumi.system.nfc.ScanOutcome
import com.niumi.system.pairing.PairedBoxStore
import com.niumi.system.ringing.RingingController
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * SPEC_ANDROID §11.1, §11.2 : le handler POC ne prend aucune décision de validité lui-même —
 * il délègue entièrement à `NiumiCoreFacade` (`:shared:core`) et se contente d'agir sur le
 * résultat. Aucun contexte de vérification n'est fourni (`context = null`) : avant la Phase C,
 * aucune session ni révision n'existe côté Android (SPEC_CORE_KMP §14, dernier alinéa).
 */
class PocNfcScanHandlerTest {
    // Fixture reprise de shared/core/src/commonTest/resources/fixtures/nfc_payloads.json.
    private val validUri = "niumi://box/v1/550e8400-e29b-41d4-a716-446655440000?token=AAAAAAAAAAAAAAAAAAAAAA"
    private val pairedBoxId = "550e8400-e29b-41d4-a716-446655440000"
    private val otherBoxId = "00000000-0000-0000-0000-000000000000"

    private val facade = NiumiCoreFacade()

    private class FakeRingingController : RingingController {
        var stopRingingCallCount = 0
        var lastSessionId: String? = null

        override fun startRinging(
            sessionId: String,
            revision: Long,
        ): OperationResult = OperationResult.Success

        override fun stopRinging(sessionId: String): OperationResult {
            stopRingingCallCount++
            lastSessionId = sessionId
            return OperationResult.Success
        }
    }

    private class FakePairedBoxStore(
        private var credential: PairedBoxCredentialDto?,
    ) : PairedBoxStore {
        override suspend fun current(): PairedBoxCredentialDto? = credential

        override suspend fun replace(credential: PairedBoxCredentialDto) {
            this.credential = credential
        }

        override suspend fun clear() {
            credential = null
        }
    }

    private fun pairedCredentialFor(boxId: String): PairedBoxCredentialDto {
        val payload = checkNotNull(facade.parseBoxPayload(validUri).payload) { "fixture URI must be VALID" }
        val credential = PairedBoxCredentialDto.fromPayload(payload)
        return credential.copy(boxId = boxId)
    }

    @Test
    fun matchingBoxStopsRingingExactlyOnceAndReturnsAccepted() =
        runTest {
            val ringingController = FakeRingingController()
            val store = FakePairedBoxStore(pairedCredentialFor(pairedBoxId))
            val handler = PocNfcScanHandler(facade, store, ringingController)

            val outcome = handler.onUriRead(validUri)

            assertThat(outcome).isEqualTo(ScanOutcome.Accepted)
            assertThat(ringingController.stopRingingCallCount).isEqualTo(1)
            assertThat(ringingController.lastSessionId).isEqualTo(PocSession.ID)
        }

    @Test
    fun mismatchedBoxIdReturnsUnknownBoxWithoutStoppingRinging() =
        runTest {
            val ringingController = FakeRingingController()
            val store = FakePairedBoxStore(pairedCredentialFor(otherBoxId))
            val handler = PocNfcScanHandler(facade, store, ringingController)

            val outcome = handler.onUriRead(validUri)

            assertThat(outcome).isEqualTo(ScanOutcome.UnknownBox)
            assertThat(ringingController.stopRingingCallCount).isEqualTo(0)
        }

    @Test
    fun malformedUriReturnsUnreadableWithoutStoppingRinging() =
        runTest {
            val ringingController = FakeRingingController()
            val store = FakePairedBoxStore(pairedCredentialFor(pairedBoxId))
            val handler = PocNfcScanHandler(facade, store, ringingController)

            val outcome = handler.onUriRead("not-a-uri")

            assertThat(outcome).isEqualTo(ScanOutcome.Unreadable)
            assertThat(ringingController.stopRingingCallCount).isEqualTo(0)
        }

    @Test
    fun noPairedBoxReturnsUnknownBoxWithoutStoppingRinging() =
        runTest {
            val ringingController = FakeRingingController()
            val store = FakePairedBoxStore(credential = null)
            val handler = PocNfcScanHandler(facade, store, ringingController)

            val outcome = handler.onUriRead(validUri)

            assertThat(outcome).isEqualTo(ScanOutcome.UnknownBox)
            assertThat(ringingController.stopRingingCallCount).isEqualTo(0)
        }
}
