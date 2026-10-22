package com.niumi.system.permissions

/**
 * Les tuiles de l'écran 2 (`design-plan/SPECIFICATION.md` §5), dans l'ordre d'affichage. Le NFC
 * n'en fait pas partie (D10, tranché le 2026-10-20) : il est signalé à l'association, au scan et
 * par le bandeau de l'accueil.
 *
 * [BATTERY] et [RECENTS_LOCK] sont propres à Android (décision de Mehdi du 2026-10-21) : sur
 * HyperOS, sans exemption de batterie ni verrou dans les récents, le système arrête Niumi et le
 * blocage cesse (SPEC_ANDROID §13). [RECENTS_LOCK] n'est affiché que là où le verrou existe.
 */
enum class NiumiPermission {
    ALARM,
    APP_BLOCKING,
    BATTERY,
    RECENTS_LOCK,
    NOTIFICATIONS,
}
