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

    /**
     * Vrai si le son en cours boucle sans blanc (lecteur `AudioTrack`), faux s'il joue par le repli
     * `MediaPlayer` ou si rien ne joue (SPEC_ANDROID §10.2, 2026-10-15). Lu par le service pour
     * journaliser `AUDIO_LOOP_FALLBACK`.
     */
    val isGaplessLoop: Boolean
}
