package com.niumi.core.domain

import com.niumi.core.common.CanonicalUuid

private fun violation(
    code: String,
    message: String,
) = DomainViolation(code, message)

/**
 * Contrôles indépendants de l'état source : forme des identifiants, horodatage, révision attendue,
 * présence ou absence de chaque charge selon [SessionEventKind] (SPEC_CORE_KMP §6, §7.3, §7.4). La
 * légalité de la transition elle-même (état source → état cible) reste du ressort des réducteurs
 * par famille. Aucun de ces contrôles ne vit dans le constructeur de [SessionEvent] : §14 interdit
 * qu'une exception traverse la frontière native, voir `ETAPE-07.md`, point 3.
 */
internal object SessionEventValidation {
    internal fun validate(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): List<DomainViolation> =
        identifierViolations(event) +
            timestampViolations(event) +
            sessionViolations(snapshot, event) +
            revisionViolations(snapshot, event) +
            activationRequestViolations(event) +
            nfcProofPresenceViolations(event) +
            failureCodeViolations(event) +
            incidentViolations(event)

    private fun identifierViolations(event: SessionEvent): List<DomainViolation> {
        val invalid = !CanonicalUuid.isCanonical(event.eventId) || !CanonicalUuid.isCanonical(event.sessionId)
        return if (invalid) {
            listOf(
                violation(
                    ViolationCode.INVALID_IDENTIFIER,
                    "eventId et sessionId doivent être des UUID canoniques minuscules.",
                ),
            )
        } else {
            emptyList()
        }
    }

    private fun timestampViolations(event: SessionEvent): List<DomainViolation> =
        if (event.occurredAtEpochMillis <= 0) {
            listOf(violation(ViolationCode.INVALID_TIMESTAMP, "occurredAtEpochMillis doit être strictement positif."))
        } else {
            emptyList()
        }

    // Une nouvelle activation crée toujours un sessionId qui diffère de la session active éventuelle
    // : ce cas relève de l'invariant « une seule session non finale », traité par ActivationReducer
    // (INVALID_STATE_TRANSITION), pas d'une incohérence de sessionId.
    private fun sessionViolations(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): List<DomainViolation> =
        if (snapshot != null && event.kind != SessionEventKind.ACTIVATION_REQUESTED &&
            event.sessionId != snapshot.sessionId
        ) {
            listOf(violation(ViolationCode.UNKNOWN_SESSION, "sessionId ne correspond pas à la session référencée."))
        } else {
            emptyList()
        }

    private fun revisionViolations(
        snapshot: SessionSnapshot?,
        event: SessionEvent,
    ): List<DomainViolation> {
        if (event.kind == SessionEventKind.ACTIVATION_REQUESTED) return emptyList()
        val stale = event.expectedRevision == null || event.expectedRevision != snapshot?.revision
        return if (stale) {
            listOf(violation(ViolationCode.STALE_REVISION, "expectedRevision absent ou périmé."))
        } else {
            emptyList()
        }
    }

    private fun activationRequestViolations(event: SessionEvent): List<DomainViolation> {
        val isActivation = event.kind == SessionEventKind.ACTIVATION_REQUESTED
        val request = event.activationRequest
        return when {
            isActivation && request == null -> {
                listOf(
                    violation(
                        ViolationCode.MISSING_ACTIVATION_REQUEST,
                        "activationRequest requis pour ACTIVATION_REQUESTED.",
                    ),
                )
            }

            !isActivation && request != null -> {
                listOf(
                    violation(
                        ViolationCode.UNEXPECTED_EVENT_PAYLOAD,
                        "activationRequest inattendu hors ACTIVATION_REQUESTED.",
                    ),
                )
            }

            request != null -> {
                appSelectionViolations(request)
            }

            else -> {
                emptyList()
            }
        }
    }

    private fun appSelectionViolations(request: ActivationRequest): List<DomainViolation> {
        val count = request.appSelection.count
        return if (count < AppSelectionSummary.MIN_COUNT || count > AppSelectionSummary.MAX_COUNT) {
            listOf(violation(ViolationCode.INVALID_APP_SELECTION, "count doit être compris entre 1 et 50."))
        } else {
            emptyList()
        }
    }

    private fun nfcProofPresenceViolations(event: SessionEvent): List<DomainViolation> {
        val isNfcScan = event.kind == SessionEventKind.VALID_NFC_SCANNED
        return when {
            isNfcScan && event.nfcProof == null -> {
                listOf(violation(ViolationCode.MISSING_NFC_PROOF, "nfcProof requis pour VALID_NFC_SCANNED."))
            }

            !isNfcScan && event.nfcProof != null -> {
                listOf(violation(ViolationCode.UNEXPECTED_EVENT_PAYLOAD, "nfcProof inattendu hors VALID_NFC_SCANNED."))
            }

            else -> {
                emptyList()
            }
        }
    }

    private fun failureCodeViolations(event: SessionEvent): List<DomainViolation> {
        val isFailure = event.kind == SessionEventKind.ACTIVATION_FAILED
        return when {
            isFailure && event.failureCode == null -> {
                listOf(violation(ViolationCode.MISSING_FAILURE_CODE, "failureCode requis pour ACTIVATION_FAILED."))
            }

            !isFailure && event.failureCode != null -> {
                listOf(
                    violation(ViolationCode.UNEXPECTED_EVENT_PAYLOAD, "failureCode inattendu hors ACTIVATION_FAILED."),
                )
            }

            else -> {
                emptyList()
            }
        }
    }

    private fun incidentViolations(event: SessionEvent): List<DomainViolation> {
        val requiresIncident =
            event.kind == SessionEventKind.RELEASE_FAILED || event.kind == SessionEventKind.INCIDENT_REPORTED
        val allowsOptionalIncident = event.kind == SessionEventKind.TRIGGER_ELAPSED
        return when {
            requiresIncident && event.incident == null -> {
                listOf(
                    violation(
                        ViolationCode.MISSING_INCIDENT,
                        "incident requis pour RELEASE_FAILED et INCIDENT_REPORTED.",
                    ),
                )
            }

            !requiresIncident && !allowsOptionalIncident && event.incident != null -> {
                listOf(
                    violation(ViolationCode.UNEXPECTED_EVENT_PAYLOAD, "incident inattendu pour ce type d'événement."),
                )
            }

            else -> {
                emptyList()
            }
        }
    }
}
