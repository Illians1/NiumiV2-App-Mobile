package com.niumi.database.pairing

import com.niumi.core.interop.PairedBoxCredentialDto

/**
 * Stockage des boîtiers associés, jusqu'à dix (contrat KMP 1.5), nommés (SPEC_ANDROID §11.1, Lot 8 ; remplace
 * `current`/`replace`/`clear` de l'étape 13). Vit dans `:core:database`, avec les autres
 * interfaces de dépôt (`SessionStore`, `DirectBootStore`, `TechnicalEventLog`) : le plan MVP
 * place son implémentation Room (`RoomPairedBoxStore`) dans ce module, qui ne peut pas dépendre
 * de `:core:system` où l'interface vivait jusqu'à l'étape 13 (règle de dépendance §6).
 */
public interface PairedBoxStore {
    /** Par position, 0 en tête. Liste vide avant déverrouillage, jamais une levée. */
    public suspend fun all(): List<PairedBoxDto>

    /**
     * [name] passe par [PairedBoxName.sanitize] : la valeur nettoyée est écrite, un nom refusé lève
     * `IllegalArgumentException("PAIRED_BOX_NAME_INVALID")` avant toute écriture (§7.2, §11.1).
     */
    public suspend fun add(
        credential: PairedBoxCredentialDto,
        name: String,
    ): PairedBoxAddResult

    /** Même règle que [add] pour [name]. */
    public suspend fun rename(
        boxId: String,
        name: String,
    )

    public suspend fun remove(boxId: String)
}
