package com.niumi.core.emergency

import com.niumi.core.nfc.NfcVerificationContext

/** Résultat d'une recopie comparée au texte modèle (`SPECIFICATION.md` §13). */
public enum class TranscriptionStatus {
    COMPLETE,
    INCOMPLETE,
    MISMATCH,
    UNKNOWN_TEXT,
}

/**
 * [mismatchWordIndex] désigne, dans le texte normalisé, le premier mot qui diffère ; il vaut le
 * nombre de mots du modèle quand la saisie le dépasse. [typedCharCount] est la longueur brute de la
 * saisie, pour le compteur « 84 sur 190 caractères ». [proof] n'existe que sur `COMPLETE` avec un
 * contexte.
 */
public data class TranscriptionResult(
    val status: TranscriptionStatus,
    val matchedWordCount: Int,
    val mismatchWordIndex: Int?,
    val expectedCharCount: Int,
    val typedCharCount: Int,
    val proof: EmergencyExitProof?,
)

/**
 * Vérifie la recopie d'un texte de sortie de secours (`SPECIFICATION.md` §13, SPEC_CORE_KMP §12).
 * Les majuscules, les accents, la ponctuation et les espaces en double sont ignorés : « J’avais »,
 * « J'avais » et « Javais » se valent, comme la majuscule automatique du clavier. La comparaison se
 * fait mot à mot ; seul le dernier mot tapé peut être le début du mot attendu (frappe en cours).
 *
 * `commonMain` n'a pas de normalisation Unicode (NFD) : les lettres accentuées du français sont
 * repliées par une table, et les diacritiques combinants disparaissent avec la ponctuation, car ils
 * ne sont ni lettres ni chiffres (écart documenté dans `ETAPE-42.md`).
 */
public object EmergencyTranscription {
    private val FOLDED: Map<Char, String> =
        buildMap {
            "àâä".forEach { put(it, "a") }
            "éèêë".forEach { put(it, "e") }
            "îï".forEach { put(it, "i") }
            "ôö".forEach { put(it, "o") }
            "ùûü".forEach { put(it, "u") }
            put('ÿ', "y")
            put('ç', "c")
            put('œ', "oe")
            put('æ', "ae")
        }

    public fun verify(
        textId: String,
        typed: String,
        context: NfcVerificationContext?,
    ): TranscriptionResult {
        val model =
            EmergencyExitTexts.text(textId)
                ?: return TranscriptionResult(
                    status = TranscriptionStatus.UNKNOWN_TEXT,
                    matchedWordCount = 0,
                    mismatchWordIndex = null,
                    expectedCharCount = 0,
                    typedCharCount = typed.length,
                    proof = null,
                )
        val expected = normalizedWords(model)
        val words = normalizedWords(typed)
        val matched = expected.zip(words).takeWhile { (want, got) -> want == got }.size
        val status =
            when {
                words.size > expected.size && matched == expected.size -> {
                    TranscriptionStatus.MISMATCH
                }

                matched == words.size -> {
                    completeOrIncomplete(matched, expected.size)
                }

                matched == words.lastIndex && expected[matched].startsWith(words[matched]) -> {
                    TranscriptionStatus.INCOMPLETE
                }

                else -> {
                    TranscriptionStatus.MISMATCH
                }
            }
        val proof =
            if (status == TranscriptionStatus.COMPLETE && context != null) {
                EmergencyExitProof(
                    textId = textId,
                    sessionId = context.sessionId,
                    eventId = context.eventId,
                    expectedRevision = context.expectedRevision,
                    verifiedAtEpochMillis = context.occurredAtEpochMillis,
                )
            } else {
                null
            }
        return TranscriptionResult(
            status = status,
            matchedWordCount = matched,
            mismatchWordIndex = matched.takeIf { status == TranscriptionStatus.MISMATCH },
            expectedCharCount = model.length,
            typedCharCount = typed.length,
            proof = proof,
        )
    }

    private fun completeOrIncomplete(
        matched: Int,
        expectedCount: Int,
    ): TranscriptionStatus =
        if (matched == expectedCount) TranscriptionStatus.COMPLETE else TranscriptionStatus.INCOMPLETE

    /** Minuscules, accents repliés, ponctuation retirée, découpage sur les espaces. */
    internal fun normalizedWords(text: String): List<String> {
        val folded =
            buildString {
                for (char in text.lowercase()) {
                    val replacement = FOLDED[char]
                    when {
                        replacement != null -> append(replacement)
                        char.isLetterOrDigit() -> append(char)
                        char.isWhitespace() -> append(' ')
                    }
                }
            }
        return folded.split(' ').filter { it.isNotEmpty() }
    }
}
