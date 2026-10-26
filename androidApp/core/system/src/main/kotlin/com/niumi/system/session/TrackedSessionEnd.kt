package com.niumi.system.session

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto

/**
 * Fin de la session suivie par l'écran de réveil ou la feuille d'annulation (étape 46), lue sur la
 * session relue par [SessionHistoryReader], jamais sur le seul résultat du scan : `Accepted` ne
 * dit pas que la libération a réussi (SPEC_ANDROID §11.3).
 */
sealed interface TrackedSessionEnd {
    /** Scan ou recopie du matin : écran 11. */
    data class Completed(
        val sessionId: String,
    ) : TrackedSessionEnd

    /** Annulation par scan (ou recopie du soir) : accueil clair et message. */
    data object Cancelled : TrackedSessionEnd

    /** `DISARMED` ou `FAILED` : accueil, sans message. */
    data object Other : TrackedSessionEnd
}

/**
 * La session suivie doit être relue quand le snapshot publié ne la montre plus en cours : une
 * autre session (le crochet du planning a armé la suivante), plus rien, ou elle-même finie.
 */
fun needsTrackedSessionRead(
    trackedSessionId: String,
    published: SessionSnapshotDto?,
): Boolean = published == null || published.sessionId != trackedSessionId || published.state in SESSION_FINAL_STATES

/** `null` tant que la session relue n'est pas finie, ou introuvable. */
fun trackedSessionEndOf(session: SessionSnapshotDto?): TrackedSessionEnd? =
    when (session?.state) {
        SessionStateDto.COMPLETED -> TrackedSessionEnd.Completed(session.sessionId)
        SessionStateDto.CANCELLED -> TrackedSessionEnd.Cancelled
        SessionStateDto.DISARMED, SessionStateDto.FAILED -> TrackedSessionEnd.Other
        else -> null
    }
