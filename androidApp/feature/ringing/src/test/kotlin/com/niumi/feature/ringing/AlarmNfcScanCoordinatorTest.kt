package com.niumi.feature.ringing

import com.google.common.truth.Truth.assertThat
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.system.audio.VibrationController
import com.niumi.system.nfc.NfcScanHandler
import com.niumi.system.nfc.ScanOutcome
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test

/**
 * SPEC_ANDROID §11.2, §11.3 : garde de réentrance, vibration d'erreur sur boîtier inconnu,
 * journalisation sans jamais exposer l'URI, le token ni son hash (§16 : le journal ne reçoit
 * ici que le type d'événement).
 */
class AlarmNfcScanCoordinatorTest {
    private class FakeVibrationController : VibrationController {
        var errorCallCount = 0

        override fun startRepeating() = Unit

        override fun vibrateError() {
            errorCallCount++
        }

        override fun stop() = Unit
    }

    private class FakeTechnicalEventLog : TechnicalEventLog {
        val loggedTypes = mutableListOf<TechnicalEventType>()

        override fun log(
            type: TechnicalEventType,
            sessionId: String?,
            packageName: String?,
        ) {
            loggedTypes += type
        }

        override fun recent() = emptyList<Nothing>()
    }

    private val vibrationController = FakeVibrationController()
    private val technicalEventLog = FakeTechnicalEventLog()
    private val coordinator = AlarmNfcScanCoordinator(vibrationController, technicalEventLog)

    @Test
    fun nullHandlerReturnsNullWithoutLogging() =
        runTest {
            val outcome = coordinator.handleUri(scanHandler = null, uri = "niumi://box/v1/x?token=y")

            assertThat(outcome).isNull()
            assertThat(technicalEventLog.loggedTypes).isEmpty()
        }

    @Test
    fun acceptedOutcomeLogsValidScan() =
        runTest {
            val handler = NfcScanHandler { ScanOutcome.Accepted }

            val outcome = coordinator.handleUri(handler, uri = "niumi://box/v1/x?token=y")

            assertThat(outcome).isEqualTo(ScanOutcome.Accepted)
            assertThat(technicalEventLog.loggedTypes).containsExactly(TechnicalEventType.NFC_SCAN_VALID)
            assertThat(vibrationController.errorCallCount).isEqualTo(0)
        }

    @Test
    fun unknownBoxOutcomeLogsInvalidScanAndVibrates() =
        runTest {
            val handler = NfcScanHandler { ScanOutcome.UnknownBox }

            val outcome = coordinator.handleUri(handler, uri = "niumi://box/v1/x?token=y")

            assertThat(outcome).isEqualTo(ScanOutcome.UnknownBox)
            assertThat(technicalEventLog.loggedTypes).containsExactly(TechnicalEventType.NFC_SCAN_INVALID)
            assertThat(vibrationController.errorCallCount).isEqualTo(1)
        }

    @Test
    fun aScanAlreadyInProgressMakesTheNextOneReturnNull() =
        runTest {
            var releaseFirstCall: (() -> Unit)? = null
            val slowHandler =
                NfcScanHandler { _ ->
                    // Simule un scan long : releaseFirstCall permet au test de contrôler
                    // explicitement quand le premier appel se termine, sans dépendre du temps réel.
                    suspendCancellableCoroutine { continuation ->
                        releaseFirstCall = { continuation.resumeWith(Result.success(ScanOutcome.Accepted)) }
                    }
                }

            val firstCallDeferred = async { coordinator.handleUri(slowHandler, uri = "first") }
            // Laisse le premier appel s'installer avant de tenter le second.
            yield()
            val secondOutcome = coordinator.handleUri(slowHandler, uri = "second")
            releaseFirstCall?.invoke()
            val firstOutcome = firstCallDeferred.await()

            assertThat(secondOutcome).isNull()
            assertThat(firstOutcome).isEqualTo(ScanOutcome.Accepted)
        }

    @Test
    fun handleUnreadableLogsInvalidScanWithoutVibrating() {
        val outcome = coordinator.handleUnreadable()

        assertThat(outcome).isEqualTo(ScanOutcome.Unreadable)
        assertThat(technicalEventLog.loggedTypes).containsExactly(TechnicalEventType.NFC_SCAN_INVALID)
        assertThat(vibrationController.errorCallCount).isEqualTo(0)
    }
}
