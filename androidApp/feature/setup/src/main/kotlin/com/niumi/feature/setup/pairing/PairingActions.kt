package com.niumi.feature.setup.pairing

/**
 * Les actions de l'écran 3, regroupées : passées une à une, elles dépassaient la limite de
 * paramètres, et les compter séparément n'apportait rien — elles forment un seul contrat
 * d'interaction entre [PairingScreen] et son appelant.
 */
data class PairingActions(
    val onOpenNfcSettings: () -> Unit,
    val onContinue: () -> Unit,
    val onStartAdding: () -> Unit = {},
    val onCancelScan: () -> Unit = {},
    val onNameChanged: (String) -> Unit = {},
    val onConfirmAdd: () -> Unit = {},
    val onCancelAdd: () -> Unit = {},
    val onStartRename: (boxId: String) -> Unit = {},
    val onConfirmRename: () -> Unit = {},
    val onCancelRename: () -> Unit = {},
    val onRemove: (boxId: String) -> Unit = {},
)
