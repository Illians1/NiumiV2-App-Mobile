package com.niumi.feature.setup.pairing

import com.niumi.core.diagnostics.PairedBoxLimit
import com.niumi.database.pairing.PairedBoxName
import com.niumi.system.nfc.NfcAvailability

/**
 * Une ligne de la liste « Mes Niumi Spots ». [boxId] sert aux actions de la ligne et n'est jamais
 * affiché en entier : l'écran ne montre que [prefix] (§16). L'empreinte du token n'entre pas
 * dans l'état d'affichage.
 */
data class PairedBoxRow(
    val boxId: String,
    val prefix: String,
    val name: String,
)

/** Renommage en cours : la ligne visée et le texte saisi, pas encore validé. */
data class Renaming(
    val boxId: String,
    val text: String,
)

/**
 * État de l'écran 3 « Mes Niumi Spots » (SPEC_ANDROID §15, Lot 8).
 *
 * [isSessionInProgress] (étape 43) ne bloque plus rien : les Niumi Spots se modifient en session
 * (SPEC_CORE_KMP §2, décisions 11 et 16). Il masque seulement « Continuer », la suite de la mise en
 * route n'ayant rien à préparer pendant une session (décision de Mehdi du 2026-10-24).
 *
 * [isScanning] : la lecture a été demandée par « Ajouter un Niumi Spot », ou ouverte d'office faute
 * de tout Niumi Spot (décision du 2026-10-17). [pendingName] : un Niumi Spot neuf vient d'être lu, son
 * nom attend confirmation ; le credential lu reste dans le ViewModel, hors de l'état affiché.
 */
data class PairingUiState(
    val nfcAvailability: NfcAvailability = NfcAvailability.ENABLED,
    val boxes: List<PairedBoxRow> = emptyList(),
    val isScanning: Boolean = false,
    val pendingName: String? = null,
    val renaming: Renaming? = null,
    val message: String? = null,
    val isSessionInProgress: Boolean = false,
) {
    val isAtLimit: Boolean get() = boxes.size >= PairedBoxLimit.MAX

    val canAdd: Boolean get() = !isAtLimit

    /** Q22 : il reste toujours au moins un Niumi Spot ; « Retirer » disparaît sur le dernier (étape 43). */
    val canRemove: Boolean get() = boxes.size > PairedBoxLimit.MIN

    val isDialogOpen: Boolean get() = pendingName != null || renaming != null

    /**
     * Le Reader Mode n'écoute que lorsqu'un ajout est réellement attendu : jamais pendant un
     * dialogue (un second scan écraserait le premier). Session comprise depuis l'étape 43 (§11.1).
     */
    val isReaderModeExpected: Boolean
        get() = nfcAvailability == NfcAvailability.ENABLED && canAdd && isScanning && !isDialogOpen

    val canContinue: Boolean get() = boxes.isNotEmpty() && !isSessionInProgress

    /** Même règle que le dépôt (`PairedBoxName.sanitize`) : l'écran empêche l'appel, il ne la redéfinit pas. */
    val canConfirmName: Boolean get() = pendingName?.let(PairedBoxName::sanitize) != null

    val canConfirmRename: Boolean get() = renaming?.text?.let(PairedBoxName::sanitize) != null
}
