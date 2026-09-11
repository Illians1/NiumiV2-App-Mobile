package com.niumi.feature.setup.pairing

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.core.nfc.BoxPayloadStatus
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.feature.setup.isSetupEditable
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Association du boîtier (écran 3, SPEC_ANDROID §11.1). Ne juge jamais lui-même de la validité
 * d'un payload : `NiumiCoreFacade.parseBoxPayload` tranche (SPEC_CORE_KMP §9.3), et seul
 * `PairedBoxCredentialDto.fromPayload` calcule l'empreinte — le token en clair ne quitte jamais
 * `:shared:core`.
 *
 * Le ViewModel ne lit de [NfcReader] que sa disponibilité : le Reader Mode lui-même se branche
 * sur l'activité hôte depuis [PairingRoute], parce que `enableReaderMode` exige une `Activity`
 * au premier plan (§11.1) qu'un ViewModel n'a pas à connaître.
 */
@HiltViewModel
class PairingViewModel
    @Inject
    constructor(
        private val facade: NiumiCoreFacade,
        private val pairedBoxStore: PairedBoxStore,
        private val technicalEventLog: TechnicalEventLog,
        private val nfcReader: NfcReader,
        snapshotPublisher: SessionSnapshotPublisher,
    ) : ViewModel() {
        var state by mutableStateOf(PairingUiState(nfcAvailability = nfcReader.availability))
            private set

        init {
            viewModelScope.launch { refreshPairedBox() }
            // Garde de `SetupGate` : le boîtier figé dans une session en cours ne peut pas être
            // remplacé (SPEC_CORE_KMP §2 point 11).
            viewModelScope.launch {
                snapshotPublisher.snapshot.collect { snapshot ->
                    onSessionStateChanged(!isSetupEditable(snapshot?.state))
                }
            }
        }

        /**
         * Recalcule la disponibilité du NFC au retour des réglages (SPEC_ANDROID §13). Perdre le
         * lecteur efface le retour du scan précédent : « Réessaie en approchant le boîtier » sous
         * un NFC coupé conseille une action impossible, donc un faux état (§15). Une association
         * réussie, elle, énonce un fait et survit — c'est `pairedBoxIdPrefix` qui la porte.
         */
        fun refreshAvailability() {
            val availability = nfcReader.availability
            val losesReader = availability != NfcAvailability.ENABLED
            state =
                state.copy(
                    nfcAvailability = availability,
                    message = if (losesReader && state.message != PairingTexts.PAIRED) null else state.message,
                )
        }

        /**
         * [activity] traverse le ViewModel sans jamais y être conservée : `enableReaderMode` en
         * exige une, et la garder survivrait à sa destruction (fuite). Elle n'est utilisée que le
         * temps de l'appel, exactement comme dans `AlarmActivity`.
         */
        fun startReaderMode(activity: Activity) {
            if (!state.isReaderModeExpected) return
            nfcReader.start(activity, onUri = ::onUriRead, onUnreadable = ::onUnreadableTag)
        }

        fun stopReaderMode(activity: Activity) {
            nfcReader.stop(activity)
        }

        fun onSessionStateChanged(isSessionInProgress: Boolean) {
            state =
                state.copy(
                    isSessionInProgress = isSessionInProgress,
                    message = if (isSessionInProgress) PairingTexts.SESSION_IN_PROGRESS else state.message,
                )
        }

        /** Appelé depuis le callback du Reader Mode (thread binder) : lance sa propre coroutine. */
        fun onUriRead(uri: String) {
            viewModelScope.launch {
                if (state.isSessionInProgress) return@launch
                val result = facade.parseBoxPayload(uri)
                val payload = result.payload
                if (result.status != BoxPayloadStatus.VALID || payload == null) {
                    technicalEventLog.log(TechnicalEventType.NFC_SCAN_INVALID)
                    state = state.copy(message = PairingTexts.UNKNOWN_PAYLOAD)
                    return@launch
                }
                val candidate = PairedBoxCredentialDto.fromPayload(payload)
                if (pairedBoxStore.current() == null) {
                    store(candidate)
                } else {
                    // §11.1 : « toute nouvelle association remplace l'ancienne après
                    // confirmation » — rien n'est écrit avant la réponse de l'utilisateur.
                    state = state.copy(pendingReplacement = candidate, message = null)
                }
            }
        }

        /** Tag physiquement illisible : §11.2 impose un texte distinct d'un payload non reconnu. */
        fun onUnreadableTag() {
            technicalEventLog.log(TechnicalEventType.NFC_SCAN_INVALID)
            state = state.copy(message = PairingTexts.UNREADABLE)
        }

        fun confirmReplacement() {
            val candidate = state.pendingReplacement ?: return
            viewModelScope.launch {
                state = state.copy(pendingReplacement = null)
                store(candidate)
            }
        }

        fun cancelReplacement() {
            state = state.copy(pendingReplacement = null)
        }

        private suspend fun store(credential: PairedBoxCredentialDto) {
            pairedBoxStore.replace(credential)
            technicalEventLog.log(TechnicalEventType.NFC_SCAN_VALID)
            state =
                state.copy(
                    pairedBoxIdPrefix = credential.boxId.take(PairingTexts.BOX_ID_PREFIX_LENGTH),
                    message = PairingTexts.PAIRED,
                )
        }

        private suspend fun refreshPairedBox() {
            state =
                state.copy(
                    pairedBoxIdPrefix =
                        pairedBoxStore.current()?.boxId?.take(PairingTexts.BOX_ID_PREFIX_LENGTH),
                )
        }
    }
