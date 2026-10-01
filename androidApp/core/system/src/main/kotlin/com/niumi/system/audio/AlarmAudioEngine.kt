package com.niumi.system.audio

import com.niumi.system.common.OperationResult

/**
 * Contrat du moteur audio d'alarme (« Interfaces transverses » du plan MVP, Lot 7). `start` est
 * idempotent (`AlreadySatisfied` si un son joue déjà) et pilote lui-même la montée progressive ;
 * `stop` l'annule. Ce qui sonne est décidé en amont par `RingingSoundResolver`.
 */
interface AlarmAudioEngine {
    fun start(sound: AlarmSound): OperationResult

    fun stop(): OperationResult

    val isPlaying: Boolean
}
