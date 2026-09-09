package com.niumi.core.domain

/** État final atteint après une phase `RELEASING` réussie (SPEC_CORE_KMP §5, §12). */
public enum class ReleaseTarget {
    COMPLETED,
    CANCELLED,
}
