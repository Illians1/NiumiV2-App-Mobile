package com.niumi.system.common

/**
 * Horloge injectable : aucun composant système ne lit `System.currentTimeMillis()`
 * directement, pour que les tests n'attendent jamais une vraie heure (SPEC_ANDROID §19.2).
 */
interface Clock {
    fun nowEpochMillis(): Long
}

class SystemClock : Clock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
