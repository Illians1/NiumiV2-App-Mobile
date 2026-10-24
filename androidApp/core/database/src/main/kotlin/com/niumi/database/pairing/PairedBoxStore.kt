package com.niumi.database.pairing

import com.niumi.core.interop.PairedBoxCredentialDto
import kotlinx.coroutines.flow.Flow

/**
 * Stockage des boîtiers associés, de un à dix (contrat KMP 1.5), nommés (SPEC_ANDROID §11.1,
 * Lot 8 ; remplace `current`/`replace`/`clear` de l'étape 13). C'est l'ensemble **courant** :
 * modifiable à tout moment, session comprise, il est lu au moment du scan pour le vérifier
 * (SPEC_CORE_KMP §2, décisions 11 et 16). Vit dans `:core:database`, avec les autres interfaces de
 * dépôt (`SessionStore`, `DirectBootStore`, `TechnicalEventLog`) : le plan MVP place son
 * implémentation Room (`RoomPairedBoxStore`) dans ce module, qui ne peut pas dépendre
 * de `:core:system` où l'interface vivait jusqu'à l'étape 13 (règle de dépendance §6).
 */
public interface PairedBoxStore {
    /** Par position, 0 en tête. Liste vide avant déverrouillage, jamais une levée. */
    public suspend fun all(): List<PairedBoxDto>

    /**
     * Même liste que [all], réémise à chaque écriture. Une seule liste vide avant déverrouillage,
     * sans ouvrir la base : un flux collecté verrouillé ne se rouvre pas de lui-même au déverrouillage.
     */
    public val points: Flow<List<PairedBoxDto>>

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

    /** Refuse le dernier boîtier ([PairedBoxRemoveResult.LastBoxKept]) : il en reste toujours au moins un. */
    public suspend fun remove(boxId: String): PairedBoxRemoveResult
}
