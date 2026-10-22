package com.niumi.system.permissions

import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.readiness.ReadinessInput

/**
 * Les trois tuiles de l'écran 2, lues à chaque appel (SPEC_ANDROID §13 : recalculer après chaque
 * retour des réglages). Aucun état n'est gardé : `DeviceReadinessChecker` relit ses sources.
 */
fun interface PermissionStatusSource {
    suspend fun status(): List<PermissionStatus>
}

/** Agrège le diagnostic existant : aucune détection n'est dupliquée. */
class AndroidPermissionStatusSource(
    private val readinessChecker: DeviceReadinessChecker,
) : PermissionStatusSource {
    override suspend fun status(): List<PermissionStatus> =
        permissionStatusesOf(readinessChecker.check(ReadinessInput()))
}
