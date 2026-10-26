package com.niumi.feature.session.home

/** Gestes de la feuille d'annulation. */
data class CancelSessionActions(
    val onDismiss: () -> Unit,
    val onScan: () -> Unit,
    val onCancelScan: () -> Unit,
    val onRetry: () -> Unit,
    val onOpenNfcSettings: () -> Unit,
)
