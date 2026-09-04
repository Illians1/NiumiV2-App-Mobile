package com.niumi.core.nfc

/**
 * Preuve opaque qu'un scan NFC correspond au boîtier associé (SPEC_CORE_KMP §6). Créée
 * uniquement par [BoxVerifier.verify], en mémoire, jamais sérialisée ni journalisée. Le
 * constructeur `internal` empêche toute construction ou réutilisation par un adaptateur natif
 * hors de `:shared:core` ; `commonTest` reste un module ami de `commonMain` et voit donc ce
 * constructeur, mais aucun module Android ou iOS ne l'appelle jamais directement.
 *
 * Volontairement pas une `data class` : ni `equals`/`hashCode` structurels ni `copy()` ne
 * doivent permettre de fabriquer ou de comparer une preuve en dehors de son usage prévu.
 */
public class NfcVerificationProof internal constructor(
    public val boxId: String,
    public val sessionId: String,
    public val eventId: String,
    public val expectedRevision: Long,
    public val verifiedAtEpochMillis: Long,
) {
    override fun toString(): String {
        val boxIdPrefix = boxId.take(8)
        return "NfcVerificationProof(boxId=$boxIdPrefix…, sessionId=$sessionId, eventId=$eventId, " +
            "expectedRevision=$expectedRevision, verifiedAtEpochMillis=$verifiedAtEpochMillis)"
    }
}
