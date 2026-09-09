package com.niumi.database.converter

import androidx.room.TypeConverter
import com.niumi.core.interop.ReleaseTargetDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionStateDto

/**
 * Convertisseurs Room des enums de `AlarmSessionEntity` (SPEC_ANDROID §7.2). Stockage par `name`,
 * jamais par `ordinal` : un enum ajouté dans `:shared:core` ne doit pas décaler les valeurs déjà
 * écrites. Un nom inconnu en base laisse `enumValueOf` lever : c'est une corruption, traitée
 * explicitement plutôt que masquée (SPEC_CORE_KMP §13). `SessionStateDto`, `ReleaseTargetDto` et
 * `SessionHealthDto` sont des `typealias` vers `com.niumi.core.domain.*` (ETAPE-08.md) : ce fichier
 * n'importe que `com.niumi.core.interop`.
 */
class SessionEnumConverters {
    @TypeConverter
    fun fromSessionState(value: SessionStateDto): String = value.name

    @TypeConverter
    fun toSessionState(value: String): SessionStateDto = enumValueOf(value)

    @TypeConverter
    fun fromReleaseTarget(value: ReleaseTargetDto?): String? = value?.name

    @TypeConverter
    fun toReleaseTarget(value: String?): ReleaseTargetDto? = value?.let { enumValueOf(it) }

    @TypeConverter
    fun fromSessionHealth(value: SessionHealthDto): String = value.name

    @TypeConverter
    fun toSessionHealth(value: String): SessionHealthDto = enumValueOf(value)
}
