package com.niumi.database.directboot

/**
 * Résultat d'une écriture Direct Boot (SPEC_ANDROID §7.3, SPEC_CORE_KMP §13). Remplace
 * `OperationResult` des « Interfaces transverses » du plan MVP : `OperationResult` vit dans
 * `:core:system`, `DirectBootStore` dans `:core:database`, et `:core:system → :core:database`
 * jamais l'inverse (règle de dépendance SPEC_ANDROID §6). Voir `ETAPE-10.md`, écart 4.
 */
sealed interface DirectBootWriteResult {
    data object Written : DirectBootWriteResult

    data object StaleRevision : DirectBootWriteResult

    data class Failed(
        val reason: String,
    ) : DirectBootWriteResult
}

/**
 * Projection partielle de Room dans le stockage protégé de l'appareil (SPEC_ANDROID §7.3).
 * Non-`suspend` (« Interfaces transverses » du plan MVP) : appelée depuis un composant
 * `directBootAware` (`AlarmReceiver`) sans contexte de coroutine garanti.
 */
interface DirectBootStore {
    /** `null` si aucun snapshot n'a jamais été écrit. */
    fun read(): DirectBootSnapshot?

    /**
     * Refuse une révision strictement inférieure à celle déjà écrite **pour la même session**
     * (SPEC_CORE_KMP §13). Une nouvelle session (`sessionId` différent) repart légitimement à une
     * révision inférieure : `ActivationReducer` réinitialise `revision = 1` par session
     * (`ETAPE-10.md`, écart 6).
     */
    fun write(snapshot: DirectBootSnapshot.Active): DirectBootWriteResult

    fun clear()
}

/**
 * Politique de révision, extraite de [FileDirectBootStore] pour être testée en JVM sans dépendance
 * Android (même motif que `AlarmPendingIntentSpecs`/`RingingNotificationSpecs`) : [FileDirectBootStore]
 * ne fait qu'orchestrer la lecture, cette décision et l'écriture atomique.
 */
internal fun decideWrite(
    existing: DirectBootSnapshot?,
    candidate: DirectBootSnapshot.Active,
): DirectBootWriteResult =
    if (existing is DirectBootSnapshot.Active &&
        existing.sessionId == candidate.sessionId &&
        candidate.domainRevision < existing.domainRevision
    ) {
        DirectBootWriteResult.StaleRevision
    } else {
        DirectBootWriteResult.Written
    }
