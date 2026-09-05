package com.niumi.app.poc

import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.nfc.BoxPayloadStatus
import com.niumi.core.nfc.BoxVerificationStatus
import com.niumi.system.nfc.NfcScanHandler
import com.niumi.system.nfc.ScanOutcome
import com.niumi.system.pairing.PairedBoxStore
import com.niumi.system.ringing.RingingController
import javax.inject.Inject

/**
 * Handler NFC de la route POC (debug uniquement, SPEC_ANDROID §22 Lot 0). Ne décide jamais
 * lui-même de la validité d'un payload (SPEC_CORE_KMP §9.3) : délègue entièrement à
 * [NiumiCoreFacade]. Remplacé par `HandleValidNfcUseCase` à l'étape 18 et supprimé à
 * l'étape 21 avec le reste de la route POC.
 *
 * Aucun [com.niumi.core.interop.NfcVerificationContextDto] n'est fourni : avant la Phase C,
 * il n'existe ni session ni révision côté Android (SPEC_CORE_KMP §14, dernier alinéa) — donc
 * jamais de `NfcVerificationProof`, qui n'aurait de toute façon aucun destinataire ici.
 */
class PocNfcScanHandler
    @Inject
    constructor(
        private val facade: NiumiCoreFacade,
        private val pairedBoxStore: PairedBoxStore,
        private val ringingController: RingingController,
    ) : NfcScanHandler {
        override suspend fun onUriRead(uri: String): ScanOutcome {
            val parseResult = facade.parseBoxPayload(uri)
            val payload = parseResult.payload
            val credential = pairedBoxStore.current()
            return when {
                parseResult.status != BoxPayloadStatus.VALID || payload == null -> {
                    ScanOutcome.Unreadable
                }

                credential == null -> {
                    ScanOutcome.UnknownBox
                }

                facade.verifyBox(payload, credential, context = null).status == BoxVerificationStatus.MATCH -> {
                    ringingController.stopRinging(PocSession.ID)
                    ScanOutcome.Accepted
                }

                else -> {
                    ScanOutcome.UnknownBox
                }
            }
        }
    }
