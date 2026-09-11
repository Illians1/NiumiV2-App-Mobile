package com.niumi.database.directboot

import kotlinx.serialization.json.Json

/**
 * Configuration `Json` unique du format persisté (SPEC_ANDROID §7.3), partagée par
 * [FileDirectBootStore] et ses tests. `encodeDefaults = true` est impératif :
 * [DirectBootSnapshot.Active.projectionSchemaVersion] a une valeur par défaut, et le comportement
 * par défaut de kotlinx-serialization omet silencieusement un champ égal à sa valeur par défaut —
 * un futur changement de format ne pourrait alors plus distinguer un ancien fichier (champ absent)
 * d'un fichier écrit à la version actuelle avec cette valeur par coïncidence. `ignoreUnknownKeys`
 * permet à une version plus récente d'ajouter un champ sans casser une version antérieure qui le
 * relirait (rollback). Même distinction de responsabilité que `SessionEffectMapper.persistenceJson`
 * et `EventFingerprint` (étape 9) : un format persisté a ses propres contraintes, jamais partagées
 * avec un autre usage de `Json`.
 */
internal val directBootJson =
    Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }
