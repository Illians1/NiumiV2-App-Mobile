package com.niumi.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionStateDto

/**
 * Session persistée, colonnes exactes de SPEC_ANDROID §7.2. `boxId` et `boxTokenSha256Hex` sont
 * copiés depuis `PairedBoxEntity` à `ACTIVATION_REQUESTED` et figés pour la durée de la session
 * (§7.2, dernier alinéa) : `RoomSessionStore.commitDecision` les reprend de la ligne existante
 * plutôt que de l'appelant. `localDate`/`localTime` reprennent le nom de colonne de §7.2, qui
 * diffère de `localDateIso`/`localTimeIso` du DTO (SPEC_CORE_KMP §7.2) : divergence absorbée par
 * `SessionSnapshotMapper`.
 */
@Entity(tableName = "alarm_session")
data class AlarmSessionEntity(
    @PrimaryKey val id: String,
    val schemaVersion: Int,
    val revision: Long,
    val localDate: String,
    val localTime: String,
    val zoneIdAtActivation: String,
    val triggerAtEpochMillis: Long,
    val state: SessionStateDto,
    val releaseTarget: ReleaseTargetDto?,
    val health: SessionHealthDto,
    val boxId: String,
    val boxTokenSha256Hex: String,
    val ringtoneKey: String,
    val vibrationEnabled: Boolean,
    val createdAtEpochMillis: Long,
    val armedAtEpochMillis: Long?,
    val ringingAtEpochMillis: Long?,
    val alarmSoundStoppedAtEpochMillis: Long?,
    val triggerElapsedAtEpochMillis: Long?,
    val nfcVerifiedAtEpochMillis: Long?,
    val releasingAtEpochMillis: Long?,
    val completedAtEpochMillis: Long?,
    val cancelledAtEpochMillis: Long?,
    val failureCode: String?,
)
