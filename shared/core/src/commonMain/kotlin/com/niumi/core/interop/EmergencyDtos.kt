package com.niumi.core.interop

import com.niumi.core.emergency.EmergencyExitProof
import com.niumi.core.emergency.TranscriptionResult
import com.niumi.core.emergency.TranscriptionStatus

/**
 * DTO de la sortie de secours (contrat 1.5, SPEC_CORE_KMP §12, §14). Non sérialisables : la preuve
 * reste le type opaque du domaine, transmise en mémoire comme `NfcVerificationProof`.
 */
public data class EmergencyExitTextDto(
    val id: String,
    val text: String,
    val charCount: Int,
)

public data class EmergencyTranscriptionInputDto(
    val textId: String,
    val typed: String,
)

public typealias TranscriptionStatusDto = TranscriptionStatus

public data class EmergencyTranscriptionResultDto(
    val status: TranscriptionStatusDto,
    val matchedWordCount: Int,
    val mismatchWordIndex: Int?,
    val expectedCharCount: Int,
    val typedCharCount: Int,
    val proof: EmergencyExitProof?,
)

internal fun TranscriptionResult.toDto(): EmergencyTranscriptionResultDto =
    EmergencyTranscriptionResultDto(
        status,
        matchedWordCount,
        mismatchWordIndex,
        expectedCharCount,
        typedCharCount,
        proof,
    )
