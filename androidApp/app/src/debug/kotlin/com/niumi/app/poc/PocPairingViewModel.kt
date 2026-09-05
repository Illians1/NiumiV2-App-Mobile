package com.niumi.app.poc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.core.nfc.BoxPayloadStatus
import com.niumi.system.pairing.PairedBoxStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// SPEC_ANDROID §16 : jamais le token ni son hash complet, seulement un préfixe de boxId.
private const val BOX_ID_PREFIX_LENGTH = 8

data class PocPairingUiState(
    val pairedBoxIdPrefix: String? = null,
    val lastResultText: String? = null,
)

/**
 * Association d'un tag NFC pour la route POC (debug uniquement, SPEC_ANDROID §22 Lot 0,
 * §11.1). Ne décide jamais de la validité d'un payload elle-même : délègue à
 * [NiumiCoreFacade]. Remplacée à l'étape 13 par le parcours d'association réel adossé à Room.
 */
@HiltViewModel
class PocPairingViewModel
    @Inject
    constructor(
        private val facade: NiumiCoreFacade,
        private val pairedBoxStore: PairedBoxStore,
    ) : ViewModel() {
        var state by mutableStateOf(PocPairingUiState())
            private set

        init {
            viewModelScope.launch {
                state = state.copy(pairedBoxIdPrefix = pairedBoxStore.current()?.boxId?.take(BOX_ID_PREFIX_LENGTH))
            }
        }

        /** Appelé depuis le callback du Reader Mode (thread binder) : lance sa propre coroutine. */
        fun onUriRead(uri: String) {
            viewModelScope.launch {
                val result = facade.parseBoxPayload(uri)
                val payload = result.payload
                if (result.status != BoxPayloadStatus.VALID || payload == null) {
                    state = state.copy(lastResultText = "Tag illisible ou non reconnu.")
                    return@launch
                }
                val credential = PairedBoxCredentialDto.fromPayload(payload)
                pairedBoxStore.replace(credential)
                state =
                    state.copy(
                        pairedBoxIdPrefix = credential.boxId.take(BOX_ID_PREFIX_LENGTH),
                        lastResultText = "Boîtier associé.",
                    )
            }
        }
    }
