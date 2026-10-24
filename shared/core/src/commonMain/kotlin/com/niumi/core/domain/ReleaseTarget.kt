package com.niumi.core.domain

/**
 * État final atteint après une phase `RELEASING` réussie (SPEC_CORE_KMP §5, §12). `DISARMED`
 * (contrat 1.5) est distinct de `CANCELLED` : le second éteint le planning, le premier précède un
 * ré-armement.
 */
public enum class ReleaseTarget {
    COMPLETED,
    CANCELLED,
    DISARMED,
}
