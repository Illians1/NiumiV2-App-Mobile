package com.niumi.feature.session.active

import com.niumi.database.incident.SessionIncidentsReader
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.session.SessionPersistenceGateway
import javax.inject.Inject

/**
 * Les trois lectures dont l'écran 7 a besoin en plus du snapshot publié (SPEC_ANDROID §15),
 * groupées : ce sont toutes des **sources**, et les énumérer à plat faisait dépasser
 * `LongParameterList` de detekt sur le constructeur du ViewModel à l'arrivée de la troisième.
 * Même motif que `DiagnosticSources` (écran 12) et `ReadinessSources` (`:core:system`).
 *
 * [readinessChecker] rejoint l'écran 7 à l'étape 25 : un incident `CRITICAL` n'est plus « à
 * vérifier maintenant » quand son contrôle est repassé vert, ce que seul un diagnostic rejoué à
 * l'affichage peut dire — le checker est sans état (§13.1), comme sur l'écran 12.
 *
 * Aucune n'écrit : l'écran 7 constate et propose des recours, il ne décide de rien (§15).
 */
class ActiveSessionSources
    @Inject
    constructor(
        val gateway: SessionPersistenceGateway,
        val incidentsReader: SessionIncidentsReader,
        val readinessChecker: DeviceReadinessChecker,
    )
