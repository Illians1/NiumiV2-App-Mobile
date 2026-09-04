package com.niumi.core.interop

import com.niumi.core.nfc.BoxPayloadParser
import com.niumi.core.nfc.BoxVerifier

/**
 * API publique exposée aux plateformes natives (SPEC_CORE_KMP §14). À cette étape, seules les
 * deux méthodes du protocole NFC sont exposées ; `reduce`, `computeWakeSchedule` et
 * `evaluateActivation` arrivent avec le moteur d'états et la politique horaire. Aucune exception
 * ne doit traverser cette frontière.
 */
public class NiumiCoreFacade {
    /** Transforme l'URI brute lue sur le tag NFC en payload typé (SPEC_ANDROID §11.2). */
    public fun parseBoxPayload(uri: String): BoxPayloadResultDto = BoxPayloadParser.parse(uri).toDto()

    /**
     * Compare [payload] au boîtier associé [credential]. [context] est fourni pour un scan de fin
     * de session, `null` lors d'une association (SPEC_CORE_KMP §14, dernier alinéa).
     */
    public fun verifyBox(
        payload: BoxPayloadDto,
        credential: PairedBoxCredentialDto,
        context: NfcVerificationContextDto?,
    ): BoxVerificationResultDto =
        BoxVerifier.verify(payload.toDomain(), credential.toDomain(), context?.toDomain()).toDto()
}
