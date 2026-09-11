package com.niumi.system.notification

import android.app.NotificationManager
import android.content.Context

/**
 * Filtre d'interruption courant (SPEC_ANDROID §13 : le silence total mute `STREAM_ALARM` et
 * supprime l'écran de réveil, mesure de l'étape 6). Lecture seule : Niumi ne demande jamais
 * `ACCESS_NOTIFICATION_POLICY` et ne modifie jamais le mode Ne pas déranger de l'utilisateur.
 */
fun interface InterruptionFilterSource {
    fun currentInterruptionFilter(): Int
}

class AndroidInterruptionFilterSource(
    private val context: Context,
) : InterruptionFilterSource {
    override fun currentInterruptionFilter(): Int =
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .currentInterruptionFilter
}
