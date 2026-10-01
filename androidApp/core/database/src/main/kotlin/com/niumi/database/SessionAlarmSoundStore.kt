package com.niumi.database

import androidx.room.withTransaction
import com.niumi.core.interop.SessionStateDto
import com.niumi.database.directboot.UnlockState
import javax.inject.Provider

private const val ROOM_BEFORE_UNLOCK_MESSAGE = "ROOM_BEFORE_UNLOCK"

/** Issue de [SessionAlarmSoundStore.update]. */
sealed interface AlarmSoundStoreResult {
    data object Updated : AlarmSoundStoreResult

    /** Aucune session active, ou une autre que celle demandée. */
    data object NoActiveSession : AlarmSoundStoreResult

    /** La session active n'est plus `ARMED` : sa sonnerie est figée (SPEC_ANDROID §3). */
    data object NotArmed : AlarmSoundStoreResult
}

/**
 * Modifie sonnerie et montée d'une session `ARMED` (Lot 7, SPEC_ANDROID §3, §7.2, §10.2). Aucune
 * validation du catalogue ici : `:core:database` ne connaît pas `NiumiRingtones`, et c'est
 * `SessionCoordinator.updateAlarmSound` qui valide avant d'appeler.
 */
fun interface SessionAlarmSoundStore {
    suspend fun update(
        sessionId: String,
        ringtoneKey: String,
        volumeRampSeconds: Int?,
    ): AlarmSoundStoreResult
}

/**
 * Implémentation Room de [SessionAlarmSoundStore]. Classe distincte de [RoomSessionStore], au
 * plafond detekt `TooManyFunctions`. Même garde de déverrouillage : la base n'est jamais construite
 * avant `isUserUnlocked` (SPEC_ANDROID §7.3).
 *
 * L'état est relu **dans la transaction** qui écrit : une décision du moteur (`ALARM_FIRED`) est
 * elle aussi une transaction Room, donc l'une des deux voit l'autre en entier — jamais une
 * sonnerie modifiée sur une session déjà passée en `RINGING`. Le coordinateur sérialise en plus les
 * deux sous son verrou.
 */
class RoomSessionAlarmSoundStore(
    private val databaseProvider: Provider<NiumiDatabase>,
    private val unlockState: UnlockState,
) : SessionAlarmSoundStore {
    override suspend fun update(
        sessionId: String,
        ringtoneKey: String,
        volumeRampSeconds: Int?,
    ): AlarmSoundStoreResult {
        check(unlockState.isUserUnlocked) { ROOM_BEFORE_UNLOCK_MESSAGE }
        val database = databaseProvider.get()
        return database.withTransaction {
            val activeId = database.activeSessionPointerDao().current()?.sessionId
            val session = activeId?.takeIf { it == sessionId }?.let { database.sessionDao().findById(it) }
            when {
                session == null -> {
                    AlarmSoundStoreResult.NoActiveSession
                }

                session.state != SessionStateDto.ARMED -> {
                    AlarmSoundStoreResult.NotArmed
                }

                else -> {
                    database.sessionDao().updateAlarmSound(sessionId, ringtoneKey, volumeRampSeconds)
                    AlarmSoundStoreResult.Updated
                }
            }
        }
    }
}
