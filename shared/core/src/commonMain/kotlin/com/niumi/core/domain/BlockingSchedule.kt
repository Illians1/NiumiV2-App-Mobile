package com.niumi.core.domain

/**
 * Début du blocage d'une session (SPEC_CORE_KMP §7.5). Les trois champs sont tous nuls (blocage
 * immédiat, valeur par défaut) ou tous renseignés (blocage différé) ; tout autre mélange est refusé
 * par `INVALID_BLOCKING_SCHEDULE`. Le fuseau est celui de [WakeSchedule.zoneIdAtActivation] : une
 * session n'a qu'un fuseau d'activation. [startsAtEpochMillis] est l'instant contractuel du début,
 * calculé selon §8.3 et strictement antérieur à `triggerAtEpochMillis`.
 */
public data class BlockingSchedule(
    val localDateIso: String?,
    val localTimeIso: String?,
    val startsAtEpochMillis: Long?,
) {
    public val isImmediate: Boolean get() = startsAtEpochMillis == null

    public companion object {
        /** Blocage demandé dès l'activation, comportement du MVP d'origine. */
        public val IMMEDIATE: BlockingSchedule = BlockingSchedule(null, null, null)
    }
}

/**
 * Règle unique « blocage en attente » du domaine (SPEC_CORE_KMP §4, §7.5) : une session différée
 * dont le début n'a pas encore été traité. Un snapshot de `schemaVersion` 1, lu comme un blocage
 * immédiat déjà demandé, n'est donc jamais en attente. Son miroir interop vit dans
 * `interop/BlockingStatus.kt` et doit rester formulé dans les mêmes termes.
 */
public val SessionSnapshot.isBlockingPending: Boolean
    get() = !blockingSchedule.isImmediate && blockingAppliedAtEpochMillis == null
