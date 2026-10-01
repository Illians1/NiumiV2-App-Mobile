package com.niumi.system.session

/** Issue de [SessionCoordinator.updateAlarmSound] (Lot 7, SPEC_ANDROID §3, §10.2). */
sealed interface AlarmSoundUpdateResult {
    /** Écrit dans Room et recopié dans la projection Direct Boot. */
    data object Updated : AlarmSoundUpdateResult

    /** Aucune session lisible à modifier. */
    data object NoActiveSession : AlarmSoundUpdateResult

    /** La session a quitté `ARMED` : sa sonnerie est figée. */
    data object NotArmed : AlarmSoundUpdateResult

    /** Clé absente de `NiumiRingtones`. */
    data object UnknownRingtone : AlarmSoundUpdateResult

    /** Durée absente de `VolumeRampDurations.SECONDS`. */
    data object InvalidRampDuration : AlarmSoundUpdateResult

    /** Avant déverrouillage : Room fait foi et n'est pas lisible, rien n'est écrit. */
    data object DeferredUntilUnlock : AlarmSoundUpdateResult
}
