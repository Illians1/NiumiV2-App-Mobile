package com.niumi.feature.setup.pairing

/**
 * Les quatre actions de l'écran 3, regroupées : passées une à une, elles dépassaient la limite de
 * paramètres, et les compter séparément n'apportait rien — elles forment un seul contrat
 * d'interaction entre [PairingScreen] et son appelant.
 */
data class PairingActions(
    val onConfirmReplacement: () -> Unit,
    val onCancelReplacement: () -> Unit,
    val onOpenNfcSettings: () -> Unit,
    val onContinue: () -> Unit,
)
