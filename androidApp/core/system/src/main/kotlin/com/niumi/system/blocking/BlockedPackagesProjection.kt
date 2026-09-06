package com.niumi.system.blocking

/**
 * Lecture de la projection de blocage courante. Le service d'accessibilité ne consomme que
 * cette interface, jamais `BlockingController` : il n'a besoin d'écrire nulle part
 * (SPEC_ANDROID §12.2).
 */
interface BlockedPackagesProjection {
    fun current(): BlockedPackagesState
}
