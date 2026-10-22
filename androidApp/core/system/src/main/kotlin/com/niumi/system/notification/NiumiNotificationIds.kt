package com.niumi.system.notification

/**
 * Identifiants des notifications que plusieurs composants postent sous le même numéro
 * (SPEC_ANDROID §10.3, §10.5, §12.5).
 *
 * Depuis le 2026-10-22, le service de garde reprend la notification de l'alarme, de l'alarme au
 * scan : il appelle `startForeground` avec **le même identifiant et le même contenu** que la
 * sonnerie ou la demande de scan, pour qu'une seule notification reste affichée. Android garde une
 * notification tant qu'un service de premier plan du paquet l'utilise
 * (`ActiveServices.cancelForegroundNotificationLocked`). L'identifiant est donc un contrat entre
 * `:feature:ringing`, `:feature:session` et ce module.
 *
 * Les avertissements de §13.1 (3, 5 à 9) gardent leurs identifiants dans
 * `SessionWarningNotificationSpecs` ; 4 est retiré et jamais réattribué.
 */
object NiumiNotificationIds {
    const val RINGING = 1
    const val SCAN_REQUEST = 2
    const val BLOCKING_GUARD = 10
}
