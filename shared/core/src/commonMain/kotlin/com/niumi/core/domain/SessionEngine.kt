package com.niumi.core.domain

/**
 * Réducteur pur des transitions de session (SPEC_CORE_KMP §5, §6). `reduce()` ne dépend que de ses
 * deux paramètres : sans horloge, sans aléa, sans état interne. La validation de forme précède
 * l'aiguillage direct vers la fonction `internal` responsable de l'événement ; ce découpage par
 * famille (un objet par groupe de la table SPEC_CORE_KMP §5.1) reste sous les seuils de complexité
 * de detekt — voir `ETAPE-07.md`, point 5.
 */
public class SessionEngine {
    public fun reduce(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): SessionDecision {
        val shapeViolations = SessionEventValidation.validate(snapshot, event)
        if (shapeViolations.isNotEmpty()) return reject(snapshot, shapeViolations)

        return when (event.kind) {
            SessionEventKind.ACTIVATION_REQUESTED -> ActivationReducer.onRequested(snapshot, event)
            SessionEventKind.ACTIVATION_SUCCEEDED -> ActivationReducer.onSucceeded(snapshot, event)
            SessionEventKind.ACTIVATION_FAILED -> ActivationReducer.onFailed(snapshot, event)
            SessionEventKind.ALARM_FIRED -> TriggerReducer.onAlarmFired(snapshot, event)
            SessionEventKind.ALARM_SOUND_STOPPED -> TriggerReducer.onAlarmSoundStopped(snapshot, event)
            SessionEventKind.TRIGGER_ELAPSED -> TriggerReducer.onTriggerElapsed(snapshot, event)
            SessionEventKind.VALID_NFC_SCANNED -> NfcReducer.onValidScan(snapshot, event)
            SessionEventKind.INVALID_NFC_SCANNED -> NfcReducer.onInvalidScan(snapshot, event)
            SessionEventKind.RELEASE_SUCCEEDED -> ReleaseReducer.onSucceeded(snapshot, event)
            SessionEventKind.RELEASE_FAILED -> ReleaseReducer.onFailed(snapshot, event)
            SessionEventKind.INCIDENT_REPORTED -> IncidentReducer.onReported(snapshot, event)
        }
    }
}
