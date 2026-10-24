package com.niumi.core.emergency

/**
 * Preuve opaque qu'un texte de sortie de secours a été recopié en entier (SPEC_CORE_KMP §6, §12,
 * contrat 1.5), sur le modèle de [com.niumi.core.nfc.NfcVerificationProof]. Créée uniquement par
 * [EmergencyTranscription.verify] sur une recopie `COMPLETE`, liée à un unique événement
 * `EMERGENCY_EXIT_CONFIRMED`, transmise en mémoire, jamais sérialisée ni journalisée. Le
 * constructeur `internal` empêche toute construction par un adaptateur natif.
 *
 * Volontairement pas une `data class` : ni `equals`/`hashCode` structurels ni `copy()`.
 */
public class EmergencyExitProof internal constructor(
    public val textId: String,
    public val sessionId: String,
    public val eventId: String,
    public val expectedRevision: Long,
    public val verifiedAtEpochMillis: Long,
) {
    override fun toString(): String =
        "EmergencyExitProof(textId=$textId, sessionId=$sessionId, eventId=$eventId, " +
            "expectedRevision=$expectedRevision, verifiedAtEpochMillis=$verifiedAtEpochMillis)"
}
