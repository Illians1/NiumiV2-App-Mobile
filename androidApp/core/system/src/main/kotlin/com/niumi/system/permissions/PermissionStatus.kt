package com.niumi.system.permissions

import com.niumi.system.readiness.ReadinessCheck
import com.niumi.system.readiness.ReadinessCheckId
import com.niumi.system.readiness.ReadinessOutcome
import com.niumi.system.readiness.ReadinessReport

/**
 * État d'une tuile de l'écran 2. [pending] est le premier contrôle de §13 encore en échec parmi
 * ceux que la tuile agrège : c'est lui que traite « Autoriser », un seul à la fois (§13, une
 * action principale à la fois). `null` quand la tuile est accordée.
 */
data class PermissionStatus(
    val permission: NiumiPermission,
    val granted: Boolean,
    val pending: ReadinessCheck?,
) {
    /**
     * Le contrôle en attente ne peut pas encore être jugé (`NOT_APPLICABLE` d'un contrôle qui a un
     * objet sur cet appareil) : l'écran doit relire un peu plus tard plutôt que conclure.
     */
    val isAwaitingJudgement: Boolean get() = pending?.outcome == ReadinessOutcome.NOT_APPLICABLE
}

/**
 * Contrôles agrégés par chaque tuile, **dans l'ordre de SPEC_ANDROID §13** : c'est l'ordre dans
 * lequel « Autoriser » les traite.
 *
 * - Alarme : l'accès aux alarmes exactes seul. Niumi déclare `USE_EXACT_ALARM` : rien n'est à
 *   demander en principe, la tuile est donc cochée dès l'arrivée (§13, §14).
 * - Blocage des applications : le service d'accessibilité.
 * - Batterie et Verrouiller Niumi : l'exemption de batterie et le verrou des récents, chacun sa
 *   tuile (décision de Mehdi du 2026-10-21). Sans eux, HyperOS coupe le blocage sans prévenir (§13).
 * - Notifications : la permission, puis le canal du réveil.
 *
 * Ne pas déranger, le volume, le NFC et les trois étapes de parcours n'ont pas de tuile (Q1, D10).
 */
private val CHECKS_BY_PERMISSION: Map<NiumiPermission, List<ReadinessCheckId>> =
    mapOf(
        NiumiPermission.ALARM to listOf(ReadinessCheckId.EXACT_ALARM),
        NiumiPermission.APP_BLOCKING to listOf(ReadinessCheckId.ACCESSIBILITY_SERVICE),
        NiumiPermission.BATTERY to listOf(ReadinessCheckId.BATTERY_OPTIMIZATION),
        NiumiPermission.RECENTS_LOCK to listOf(ReadinessCheckId.RECENTS_LOCK),
        NiumiPermission.NOTIFICATIONS to listOf(ReadinessCheckId.NOTIFICATIONS, ReadinessCheckId.ALARM_CHANNEL),
    )

/**
 * Contrôles dont `NOT_APPLICABLE` signifie « pas encore jugeable » et non « sans objet » : le
 * service d'accessibilité inscrit mais pas encore relié pendant la fenêtre de liaison
 * (`UnlockSettling`, SPEC_ANDROID §13.1). Après une mort du processus sur HyperOS, il ne reviendra
 * pas : dire la tuile accordée serait un faux état de fiabilité (§15). Défaut relevé à la relecture
 * de l'étape 36.
 */
private val UNDECIDED_WHEN_NOT_APPLICABLE = setOf(ReadinessCheckId.ACCESSIBILITY_SERVICE)

/**
 * Fonction pure, prouvée en JVM sans `Context`. Une tuile dont tous les contrôles sont sans objet
 * sur cet appareil n'est pas affichée : il n'y a rien à demander (verrou des récents hors
 * HyperOS). Pour [UNDECIDED_WHEN_NOT_APPLICABLE], `NOT_APPLICABLE` laisse la tuile à faire
 * ([PermissionStatus.isAwaitingJudgement]).
 */
fun permissionStatusesOf(report: ReadinessReport): List<PermissionStatus> =
    NiumiPermission.entries.mapNotNull { permission ->
        val checks = CHECKS_BY_PERMISSION.getValue(permission).map(report::check)
        val withoutObject = checks.all { it.outcome == ReadinessOutcome.NOT_APPLICABLE && !it.isUndecided() }
        if (withoutObject) return@mapNotNull null
        val pending = checks.firstOrNull { it.outcome == ReadinessOutcome.FAILED || it.isUndecided() }
        PermissionStatus(permission = permission, granted = pending == null, pending = pending)
    }

private fun ReadinessCheck.isUndecided(): Boolean =
    outcome == ReadinessOutcome.NOT_APPLICABLE && id in UNDECIDED_WHEN_NOT_APPLICABLE
