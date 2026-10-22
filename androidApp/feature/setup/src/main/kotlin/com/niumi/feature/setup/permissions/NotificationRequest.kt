package com.niumi.feature.setup.permissions

/** Ce qu'ouvre « Autoriser » sur la tuile « Notifications ». */
enum class NotificationRequest {
    /** Fenêtre système `POST_NOTIFICATIONS`. */
    SYSTEM_DIALOG,

    /** Réglages de notification de Niumi (`ACTION_APP_NOTIFICATION_SETTINGS`). */
    APP_SETTINGS,
}

private const val ANDROID_13 = 33

/**
 * SPEC_ANDROID §13, étape 36. Avant Android 13, aucune fenêtre n'existe. Ensuite, Android ne dit
 * pas si la fenêtre peut encore s'ouvrir : après deux refus, ou des notifications coupées dans les
 * réglages, `launch` revient refusé sans rien montrer. Une demande déjà faite ([requestedBefore],
 * persisté) sans justification à afficher ([showRationale] faux) signifie donc que seule la page des
 * réglages peut les rétablir. Prix assumé : une fenêtre fermée d'un geste à côté, sans réponse, mène
 * la fois suivante aux réglages, où les notifications s'activent aussi.
 */
fun notificationRequestFor(
    sdkInt: Int,
    requestedBefore: Boolean,
    showRationale: Boolean,
): NotificationRequest =
    when {
        sdkInt < ANDROID_13 -> NotificationRequest.APP_SETTINGS
        requestedBefore && !showRationale -> NotificationRequest.APP_SETTINGS
        else -> NotificationRequest.SYSTEM_DIALOG
    }
