package com.niumi.core.emergency

import com.niumi.core.nfc.NfcVerificationContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * `SPECIFICATION.md` §13 : les majuscules, les accents, la ponctuation et les espaces en double
 * sont ignorés ; le premier mot qui diffère est signalé. Cas limite 3 du plan de refonte :
 * apostrophes et majuscules automatiques du clavier ne sont jamais des fautes.
 */
class EmergencyTranscriptionTest {
    private val textId = "v1-01"
    private val model = requireNotNull(EmergencyExitTexts.text(textId))

    private val context =
        NfcVerificationContext(
            sessionId = "11111111-1111-1111-1111-111111111111",
            eventId = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
            expectedRevision = 4L,
            occurredAtEpochMillis = 1_800_000_002_000L,
        )

    @Test
    fun theExactModelIsComplete() {
        val result = EmergencyTranscription.verify(textId, model, context = null)

        assertEquals(TranscriptionStatus.COMPLETE, result.status)
        assertEquals(32, result.matchedWordCount)
        assertNull(result.mismatchWordIndex)
        assertEquals(190, result.expectedCharCount)
        assertEquals(190, result.typedCharCount)
    }

    @Test
    fun caseAccentsPunctuationAndDoubleSpacesAreIgnored() {
        val typed =
            "JAVAIS  decide de bloquer mes applications jusquau scan de mon niumi spot " +
                "je choisis darreter mon reveil sans le scanner   " +
                "JE RECOPIE CE TEXTE EN ENTIER POUR CONFIRMER QUE CEST MON CHOIX"

        assertEquals(TranscriptionStatus.COMPLETE, EmergencyTranscription.verify(textId, typed, null).status)
    }

    @Test
    fun aStraightApostropheTypedOnTheKeyboardMatchesTheTypographicOne() {
        val typed = model.replace('’', '\'')

        assertEquals(TranscriptionStatus.COMPLETE, EmergencyTranscription.verify(textId, typed, null).status)
    }

    @Test
    fun anOmittedApostropheIsIgnoredToo() {
        val typed = model.replace("’", "")

        assertEquals(TranscriptionStatus.COMPLETE, EmergencyTranscription.verify(textId, typed, null).status)
    }

    @Test
    fun theKeyboardsAutomaticCapitalIsIgnored() {
        // Le clavier met une majuscule en début de phrase, ou après un point qui n'en appelait pas.
        val typed = "J'avais Décidé De bloquer"

        val result = EmergencyTranscription.verify(textId, typed, null)

        assertEquals(TranscriptionStatus.INCOMPLETE, result.status)
        assertEquals(4, result.matchedWordCount)
    }

    @Test
    fun aMissingFinalPeriodIsStillComplete() {
        assertEquals(
            TranscriptionStatus.COMPLETE,
            EmergencyTranscription.verify(textId, model.removeSuffix("."), null).status,
        )
    }

    @Test
    fun combiningAccentsAreIgnoredLikePrecomposedOnes() {
        // « é » saisi comme « e » suivi de l'accent aigu combinant (U+0301).
        val typed = "J’avais décidé de"

        val result = EmergencyTranscription.verify(textId, typed, null)

        assertEquals(TranscriptionStatus.INCOMPLETE, result.status)
        assertEquals(3, result.matchedWordCount)
    }

    @Test
    fun aDivergentFirstWordIsAMismatchAtIndexZero() {
        val result = EmergencyTranscription.verify(textId, "Javis décidé", null)

        assertEquals(TranscriptionStatus.MISMATCH, result.status)
        assertEquals(0, result.mismatchWordIndex)
        assertEquals(0, result.matchedWordCount)
    }

    @Test
    fun theMockupsMisspelledWordIsSignalledAtItsIndex() {
        // Maquette 29 : « aplications » au lieu de « applications », sixième mot (index 5).
        val typed = "J'avais décidé de bloquer mes aplications"

        val result = EmergencyTranscription.verify(textId, typed, null)

        assertEquals(TranscriptionStatus.MISMATCH, result.status)
        assertEquals(5, result.mismatchWordIndex)
        assertEquals(5, result.matchedWordCount)
        assertEquals(41, result.typedCharCount)
    }

    @Test
    fun aPrefixOfTheLastWordIsIncomplete() {
        // Maquette 28 : la saisie s'arrête au milieu de « choisis ».
        val typed = "J'avais décidé de bloquer mes applications jusqu'au scan de mon Niumi Spot. Je choi"

        val result = EmergencyTranscription.verify(textId, typed, null)

        assertEquals(TranscriptionStatus.INCOMPLETE, result.status)
        assertEquals(13, result.matchedWordCount)
        assertNull(result.mismatchWordIndex)
        assertEquals(83, result.typedCharCount)
    }

    @Test
    fun aPrefixBeforeTheLastWordIsAMismatch() {
        // « J avais » : le préfixe n'est toléré que pour le mot en cours de frappe.
        val result = EmergencyTranscription.verify(textId, "J avais", null)

        assertEquals(TranscriptionStatus.MISMATCH, result.status)
        assertEquals(0, result.mismatchWordIndex)
    }

    @Test
    fun anEmptyEntryIsIncomplete() {
        val result = EmergencyTranscription.verify(textId, "  ", null)

        assertEquals(TranscriptionStatus.INCOMPLETE, result.status)
        assertEquals(0, result.matchedWordCount)
        assertEquals(2, result.typedCharCount)
    }

    @Test
    fun aWordBeyondTheModelIsAMismatchAfterItsLastWord() {
        val result = EmergencyTranscription.verify(textId, "$model encore", null)

        assertEquals(TranscriptionStatus.MISMATCH, result.status)
        assertEquals(32, result.mismatchWordIndex)
        assertEquals(32, result.matchedWordCount)
    }

    @Test
    fun aCompleteTranscriptionWithAContextCarriesAProofBoundToIt() {
        val result = EmergencyTranscription.verify(textId, model, context)

        val proof = assertNotNull(result.proof)
        assertEquals(textId, proof.textId)
        assertEquals(context.sessionId, proof.sessionId)
        assertEquals(context.eventId, proof.eventId)
        assertEquals(context.expectedRevision, proof.expectedRevision)
        assertEquals(context.occurredAtEpochMillis, proof.verifiedAtEpochMillis)
    }

    @Test
    fun noProofWithoutAContext() {
        assertNull(EmergencyTranscription.verify(textId, model, context = null).proof)
    }

    @Test
    fun noProofForAnIncompleteOrMismatchingEntry() {
        assertNull(EmergencyTranscription.verify(textId, "J'avais", context).proof)
        assertNull(EmergencyTranscription.verify(textId, "Javis", context).proof)
    }

    @Test
    fun anUnknownTextIdIsRefusedWithoutProof() {
        val result = EmergencyTranscription.verify("v9-99", model, context)

        assertEquals(TranscriptionStatus.UNKNOWN_TEXT, result.status)
        assertNull(result.proof)
        assertEquals(0, result.expectedCharCount)
        assertEquals(190, result.typedCharCount)
    }

    @Test
    fun normalizationFoldsFrenchLigaturesAndAccents() {
        assertEquals(listOf("oeuvre", "ca", "aeiouyc"), EmergencyTranscription.normalizedWords("Œuvre, ça ! ÂÉÎÔÛŸÇ"))
    }
}
