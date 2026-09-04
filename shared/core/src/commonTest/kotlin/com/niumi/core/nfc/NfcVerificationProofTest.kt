package com.niumi.core.nfc

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `NfcVerificationProof` a un constructeur `internal` (SPEC_CORE_KMP §6). `commonTest` est un
 * *friend module* de `commonMain` en Kotlin : ce test voit donc le constructeur comme tout code
 * de `commonMain`, et ne peut pas prouver son inaccessibilité depuis Android ou iOS par la seule
 * compilation. Ce que ce test vérifie réellement : la seule voie de production légitime observée
 * ici est [BoxVerifier.verify], et `toString()` ne révèle jamais le `boxId` complet.
 * L'inaccessibilité depuis un autre module Gradle (`:core:system`, etc.) est garantie par le
 * mot-clé `internal` lui-même, vérifiable en lisant la signature, pas par un test d'exécution.
 */
class NfcVerificationProofTest {
    @Test
    fun toStringNeverRevealsFullBoxId() {
        val proof =
            BoxVerifier
                .verify(
                    payload =
                        BoxPayload(
                            protocolVersion = 1,
                            boxId = "550e8400-e29b-41d4-a716-446655440000",
                            tokenBytes = ByteArray(16),
                        ),
                    credential =
                        PairedBoxCredential(
                            protocolVersion = 1,
                            boxId = "550e8400-e29b-41d4-a716-446655440000",
                            tokenSha256Hex = Sha256.hexOf(Sha256.hash(ByteArray(16))),
                        ),
                    context =
                        NfcVerificationContext(
                            sessionId = "session-1",
                            eventId = "event-1",
                            expectedRevision = 1L,
                            occurredAtEpochMillis = 1_000L,
                        ),
                ).proof!!

        val text = proof.toString()
        assertFalse(text.contains("550e8400-e29b-41d4-a716-446655440000"))
        assertTrue(text.contains("550e8400"))
    }
}
