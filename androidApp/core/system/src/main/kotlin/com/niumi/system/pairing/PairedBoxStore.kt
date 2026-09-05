package com.niumi.system.pairing

import com.niumi.core.interop.PairedBoxCredentialDto

/**
 * Stockage du boîtier associé (« Interfaces transverses » du plan MVP, SPEC_ANDROID §11.1).
 * Un seul boîtier est associé dans le MVP ; toute nouvelle association remplace l'ancienne.
 * Implémentation Room à l'étape 13 ; implémentation debug sur DataStore Preferences à
 * l'étape 4 (`DebugPairedBoxStore`, `src/debug` de `:app`).
 */
public interface PairedBoxStore {
    public suspend fun current(): PairedBoxCredentialDto?

    public suspend fun replace(credential: PairedBoxCredentialDto)

    public suspend fun clear()
}
