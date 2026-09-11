package com.niumi.system.session

/** Déclencheurs de réconciliation (« Interfaces transverses » du plan MVP, SPEC_ANDROID §9.3, §13.1). */
enum class ReconcileReason {
    PROCESS_START,
    USER_UNLOCKED,
    LOCKED_BOOT,
    BOOT,
    PACKAGE_REPLACED,
    TIME_CHANGED,
    TIMEZONE_CHANGED,
    BEFORE_SCAN,
    SERVICE_RECREATED,
}
