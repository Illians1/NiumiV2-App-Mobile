package com.niumi.core.interop

/**
 * Miroir interop de la règle unique « blocage en attente » du domaine
 * (`com.niumi.core.domain.isBlockingPending`, SPEC_CORE_KMP §4, §7.5) : une session différée dont le
 * début n'a pas encore été traité. Un snapshot de `schemaVersion` 1, lu comme un blocage immédiat
 * déjà demandé, n'est donc jamais en attente. Les deux définitions doivent rester formulées dans les
 * mêmes termes ; aucune troisième copie n'est admise, côté commun comme côté natif.
 */
public val SessionSnapshotDto.isBlockingPending: Boolean
    get() = blockingSchedule.startsAtEpochMillis != null && blockingAppliedAtEpochMillis == null

/**
 * Miroir interop de `com.niumi.core.domain.BlockingSchedule.isImmediate` (SPEC_CORE_KMP §7.5), au
 * même titre que [isBlockingPending] l'est de son homologue du domaine. Distincte de « en attente » :
 * un blocage différé **déjà appliqué** n'est plus en attente mais reste différé, ce que le journal
 * technique Android doit savoir pour distinguer `BLOCKING_STARTED` d'un simple `BLOCK_APPLIED`
 * d'activation (SPEC_ANDROID §17).
 */
public val BlockingScheduleDto.isImmediate: Boolean
    get() = startsAtEpochMillis == null
