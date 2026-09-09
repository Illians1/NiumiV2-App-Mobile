package com.niumi.core.domain

/** Charge obligatoire de l'événement `ACTIVATION_REQUESTED` (SPEC_CORE_KMP §6). */
public data class ActivationRequest(
    val wakeSchedule: WakeSchedule,
    val appSelection: AppSelectionSummary,
)
