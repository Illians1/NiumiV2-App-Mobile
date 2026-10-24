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
import com.niumi.database.pairing.PairedBoxAddResult
import com.niumi.database.pairing.PairedBoxName
import com.niumi.database.pairing.PairedBoxStore
import com.niumi.feature.setup.isSetupEditable
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Écran 3 « Mes boîtiers » (SPEC_ANDROID §11.1, §15 Lot 8). Ne juge jamais lui-même de la
 * validité d'un payload : `NiumiCoreFacade.parseBoxPayload` tranche (SPEC_CORE_KMP §9.3), et seul
 * `PairedBoxCredentialDto.fromPayload` calcule l'empreinte — le token en clair ne quitte jamais
 * `:shared:core`. Les refus d'un doublon et d'un onzième boîtier viennent du dépôt, en résultat
 * typé ; un nom invalide est arrêté ici par la fonction même du dépôt, `PairedBoxName.sanitize`.
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

        /** Boîtier lu en attente de son nom. Hors de [state] : il porte l'empreinte du token (§16). */
        private var pendingCandidate: PairedBoxCredentialDto? = null

        init {
            viewModelScope.launch {
                refreshBoxes()
                // Décision du 2026-10-17 : sans aucun boîtier, la lecture est ouverte d'office,
                // comme à la première configuration ; sinon elle attend « Ajouter un boîtier ».
                if (state.boxes.isEmpty() && state.canAdd) state = state.copy(isScanning = true)
            }
            // Garde de `SetupGate` : la liste figée dans une session en cours ne se modifie pas
            // (SPEC_CORE_KMP §2 point 11).
            viewModelScope.launch {
                snapshotPublisher.snapshot.collect { snapshot ->
                    onSessionStateChanged(!isSetupEditable(snapshot?.state))
                }
            }
        }

        /**
         * Recalcule la disponibilité du NFC au retour des réglages (SPEC_ANDROID §13). Perdre le
         * lecteur efface le retour du scan précédent : « Réessaie en approchant le boîtier » sous
         * un NFC coupé conseille une action impossible, donc un faux état (§15). Un ajout réussi
         * et la garde de session, eux, énoncent un fait et survivent.
         */
        fun refreshAvailability() {
            val availability = nfcReader.availability
            val losesReader = availability != NfcAvailability.ENABLED
            val keepsMessage = state.message in DURABLE_MESSAGES
            state =
                state.copy(
                    nfcAvailability = availability,
                    message = if (losesReader && !keepsMessage) null else state.message,
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

        /** Une session qui démarre ferme tout : lecture, dialogues, candidat non confirmé (§11.1). */
        fun onSessionStateChanged(isSessionInProgress: Boolean) {
            if (isSessionInProgress) pendingCandidate = null
            state =
                if (isSessionInProgress) {
                    state.copy(
                        isSessionInProgress = true,
                        isScanning = false,
                        pendingName = null,
                        renaming = null,
                        message = PairingTexts.SESSION_IN_PROGRESS,
                    )
                } else {
                    state.copy(
                        isSessionInProgress = false,
                        message = state.message.takeUnless { it == PairingTexts.SESSION_IN_PROGRESS },
                    )
                }
        }

        fun startAdding() {
            if (!state.canAdd || state.isDialogOpen) return
            state = state.copy(isScanning = true, message = null)
        }

        fun cancelScan() {
            state = state.copy(isScanning = false)
        }

        /**
         * Appelé depuis le callback du Reader Mode (thread binder) : lance sa propre coroutine.
         * Un scan arrivé hors d'une lecture attendue — dialogue ouvert, session démarrée, lecture
         * annulée entre-temps — est ignoré : il écraserait un candidat ou contournerait §11.1.
         */
        fun onUriRead(uri: String) {
            viewModelScope.launch {
                if (!state.isReaderModeExpected) return@launch
                val result = facade.parseBoxPayload(uri)
                val payload = result.payload
                if (result.status != BoxPayloadStatus.VALID || payload == null) {
                    technicalEventLog.log(TechnicalEventType.NFC_SCAN_INVALID)
                    state = state.copy(message = PairingTexts.UNKNOWN_PAYLOAD)
                    return@launch
                }
                val candidate = PairedBoxCredentialDto.fromPayload(payload)
                val existing = pairedBoxStore.all()
                // Revérifiée après la suspension : une seconde lecture du même tag, un renommage
                // ouvert ou une session démarrée pendant la lecture du dépôt l'emportent.
                if (!state.isReaderModeExpected) return@launch
                when {
                    // Relu au dépôt et non dans `state` : la liste affichée peut dater.
                    existing.any { it.credential.boxId == candidate.boxId } -> {
                        state = state.copy(message = PairingTexts.ALREADY_PAIRED)
                    }

                    else -> {
                        technicalEventLog.log(TechnicalEventType.NFC_SCAN_VALID)
                        pendingCandidate = candidate
                        state =
                            state.copy(
                                isScanning = false,
                                pendingName = DefaultBoxName.next(existing.map { it.name }),
                                message = null,
                            )
                    }
                }
            }
        }

        /** Tag physiquement illisible : §11.2 impose un texte distinct d'un payload non reconnu. */
        fun onUnreadableTag() {
            viewModelScope.launch {
                if (!state.isReaderModeExpected) return@launch
                technicalEventLog.log(TechnicalEventType.NFC_SCAN_INVALID)
                state = state.copy(message = PairingTexts.UNREADABLE)
            }
        }

        /**
         * Édition de la liste : nom d'un boîtier lu, renommage, retrait (§15, Lot 8). Regroupée à
         * part de la lecture NFC, qui reste dans le ViewModel : les deux tiennent ainsi chacune sous
         * le plafond `TooManyFunctions` de detekt (11), et l'écran les reçoit par [PairingActions].
         */
        val editor = Editor()

        inner class Editor {
            /** Le texte saisi va au dialogue ouvert, ajout ou renommage. */
            fun onNameChanged(text: String) {
                state =
                    when {
                        state.pendingName != null -> state.copy(pendingName = text)
                        state.renaming != null -> state.copy(renaming = state.renaming?.copy(text = text))
                        else -> state
                    }
            }

            /** Un nom refusé par `PairedBoxName.sanitize`, ou une session démarrée entre-temps, n'écrit rien. */
            fun confirmAdd() {
                val candidate = pendingCandidate
                val name = state.pendingName?.let(PairedBoxName::sanitize)
                if (candidate == null || name == null || state.isSessionInProgress) return
                pendingCandidate = null
                viewModelScope.launch {
                    val message =
                        when (pairedBoxStore.add(candidate, name)) {
                            PairedBoxAddResult.Added -> PairingTexts.BOX_ADDED
                            PairedBoxAddResult.AlreadyPaired -> PairingTexts.ALREADY_PAIRED
                            PairedBoxAddResult.LimitReached -> PairingTexts.LIMIT_REACHED
                        }
                    refreshBoxes()
                    state = state.copy(pendingName = null, message = message)
                }
            }

            fun cancelAdd() {
                pendingCandidate = null
                state = state.copy(pendingName = null)
            }

            fun startRename(boxId: String) {
                if (state.isSessionInProgress || state.isDialogOpen) return
                val row = state.boxes.firstOrNull { it.boxId == boxId } ?: return
                state = state.copy(isScanning = false, renaming = Renaming(boxId, row.name), message = null)
            }

            fun confirmRename() {
                val renaming = state.renaming
                val name = renaming?.text?.let(PairedBoxName::sanitize)
                if (renaming == null || name == null || state.isSessionInProgress) return
                viewModelScope.launch {
                    pairedBoxStore.rename(renaming.boxId, name)
                    refreshBoxes()
                    state = state.copy(renaming = null)
                }
            }

            fun cancelRename() {
                state = state.copy(renaming = null)
            }

            /** Sans confirmation (§15) : retirer le dernier est permis, le diagnostic refusera l'activation. */
            fun remove(boxId: String) {
                if (state.isSessionInProgress || state.isDialogOpen) return
                viewModelScope.launch {
                    pairedBoxStore.remove(boxId)
                    refreshBoxes()
                    state = state.copy(message = null)
                }
            }
        }

        private suspend fun refreshBoxes() {
            state =
                state.copy(
                    boxes =
                        pairedBoxStore.all().map { box ->
                            PairedBoxRow(
                                boxId = box.credential.boxId,
                                prefix = PairingTexts.prefixOf(box.credential.boxId),
                                name = box.name,
                            )
                        },
                )
        }

        private companion object {
            val DURABLE_MESSAGES = setOf(PairingTexts.BOX_ADDED, PairingTexts.SESSION_IN_PROGRESS)
        }
    }
