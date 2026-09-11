package com.niumi.feature.setup.pairing

import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.system.nfc.NfcAvailability

/**
 * État de l'écran 3. [pendingReplacement] non nul signifie qu'un boîtier est déjà associé et
 * qu'un nouveau tag valide attend la confirmation de SPEC_ANDROID §11.1 (« toute nouvelle
 * association remplace l'ancienne après confirmation ») : rien n'est écrit tant qu'elle n'est pas
 * donnée.
 */
data class PairingUiState(
    val nfcAvailability: NfcAvailability = NfcAvailability.ENABLED,
    val pairedBoxIdPrefix: String? = null,
    val pendingReplacement: PairedBoxCredentialDto? = null,
    val message: String? = null,
    val isSessionInProgress: Boolean = false,
) {
    val isReaderModeExpected: Boolean
        get() =
            nfcAvailability == NfcAvailability.ENABLED &&
                pendingReplacement == null &&
                !isSessionInProgress

    val canContinue: Boolean get() = pairedBoxIdPrefix != null
}
