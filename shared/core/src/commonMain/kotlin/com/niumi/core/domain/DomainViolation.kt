package com.niumi.core.domain

/** Refus typé d'un événement par le réducteur (SPEC_CORE_KMP §6, §7.3). Jamais une exception. */
public data class DomainViolation(
    val code: String,
    val message: String,
)
