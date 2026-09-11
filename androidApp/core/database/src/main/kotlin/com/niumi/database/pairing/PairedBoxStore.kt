package com.niumi.database.pairing

import com.niumi.core.interop.PairedBoxCredentialDto

/**
 * Stockage du boîtier associé (« Interfaces transverses » du plan MVP, SPEC_ANDROID §11.1).
 * Un seul boîtier est associé dans le MVP ; toute nouvelle association remplace l'ancienne.
 *
 * Vit dans `:core:database`, avec les autres interfaces de dépôt (`SessionStore`,
 * `DirectBootStore`, `TechnicalEventLog`) : le plan MVP place son implémentation Room
 * (`RoomPairedBoxStore`) dans ce module, qui ne peut pas dépendre de `:core:system` où
 * l'interface vivait jusqu'à l'étape 13 (règle de dépendance §6). Implémentation Room à
 * l'étape 13 ; implémentation debug sur DataStore Preferences depuis l'étape 4
 * (`DebugPairedBoxStore`, `src/debug` de `:app`).
 */
public interface PairedBoxStore {
    public suspend fun current(): PairedBoxCredentialDto?

    public suspend fun replace(credential: PairedBoxCredentialDto)

    public suspend fun clear()
}
