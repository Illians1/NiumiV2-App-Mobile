package com.niumi.system.readiness

import com.niumi.core.domain.IncidentCodes

/**
 * Le contrôle de §13 qui dit si la cause d'un incident est **encore** présente (SPEC_ANDROID §15,
 * étape 25). L'écran 7 le rejoue à chaque affichage : un incident `CRITICAL` dont le contrôle est
 * repassé vert n'est plus « à vérifier maintenant », il est rétabli.
 *
 * Inverse de [MonitoredReadinessChecks.incidentCodes], plus `NFC_DISABLED`, produit par
 * `SessionRuntimeReconciler` et non par la surveillance de §13.1 — même périmètre que
 * [IncidentRemediation], et pour la même raison : ces sept codes décrivent un réglage qui peut
 * revenir. `null` pour tout autre code : un fait passé (`TIME_CHANGED`, `MISSED_TRIGGER_WINDOW`) ou
 * un défaut interne (`SNAPSHOT_CORRUPTED`) n'a pas de contrôle à rejouer, et n'est **jamais** présumé
 * rétabli (§15, « ne jamais afficher un faux état de fiabilité »).
 */
object IncidentReadinessChecks {
    private val checks: Map<String, ReadinessCheckId> =
        MonitoredReadinessChecks.incidentCodes.entries.associate { (checkId, code) -> code to checkId } +
            (IncidentCodes.NFC_DISABLED to ReadinessCheckId.NFC_ENABLED)

    fun checkFor(code: String): ReadinessCheckId? = checks[code]
}
