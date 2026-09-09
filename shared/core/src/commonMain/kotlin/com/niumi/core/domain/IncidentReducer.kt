package com.niumi.core.domain

/**
 * Famille `INCIDENT_REPORTED` (SPEC_CORE_KMP §5.1, §7.3). Effets `RECORD_INCIDENT` puis
 * `PUBLISH_PLATFORM_SNAPSHOT` : décision arbitrée avec l'utilisateur le 2026-09-08, la table des
 * effets de §6 ne couvrait pas cet événement à l'origine (même ordre que `RELEASE_FAILED`, pour que
 * l'incident reste rejouable depuis l'outbox après interruption, §17). Voir `ETAPE-07.md`, point 1.
 */
internal object IncidentReducer {
    internal fun onReported(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        if (snapshot == null || snapshot.state in FINAL_STATES) {
            return reject(snapshot, listOf(invalidStateTransition(event)))
        }
        val incident = requireNotNull(event.incident) // garanti par SessionEventValidation
        val newSnapshot =
            snapshot.copy(
                revision = snapshot.revision + 1,
                health = healthAfter(snapshot.health, incident),
            )
        val effects =
            SessionEffectBuilder(newSnapshot.sessionId, newSnapshot.revision)
                .add(SessionEffectKind.RECORD_INCIDENT, IncidentEffectPayload(incident))
                .add(SessionEffectKind.PUBLISH_PLATFORM_SNAPSHOT)
                .build()
        return SessionDecision(newSnapshot, effects, emptyList())
    }
}
