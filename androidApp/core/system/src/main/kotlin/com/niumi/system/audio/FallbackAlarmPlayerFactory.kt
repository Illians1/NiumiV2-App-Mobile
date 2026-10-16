package com.niumi.system.audio

/**
 * Lecteur de réveil sans blanc de boucle d'abord, `MediaPlayer` ensuite (SPEC_ANDROID §10.2,
 * décision du 2026-10-15). Un réveil muet n'est jamais acceptable : si [primary] ne peut pas créer
 * son lecteur — tampon statique refusé par un appareil, fichier hors format —, [fallback] sonne,
 * avec son blanc à chaque tour. Le lecteur rendu dit lequel des deux joue
 * ([AlarmPlayer.gaplessLoop]) ; le service journalise `AUDIO_LOOP_FALLBACK` quand c'est le repli.
 *
 * La capture large est voulue, comme dans `DefaultAlarmAudioEngine` : la fabrique Android peut
 * lancer `IllegalStateException`, `IllegalArgumentException` ou `UnsupportedOperationException`
 * selon l'appareil, et chacune doit laisser une chance au repli.
 */
class FallbackAlarmPlayerFactory(
    private val primary: AlarmPlayerFactory,
    private val fallback: AlarmPlayerFactory,
) : AlarmPlayerFactory {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun create(
        ringtoneKey: String,
        configuration: AlarmAudioConfiguration,
    ): AlarmPlayer =
        try {
            primary.create(ringtoneKey, configuration)
        } catch (error: RuntimeException) {
            fallback.create(ringtoneKey, configuration)
        }
}
