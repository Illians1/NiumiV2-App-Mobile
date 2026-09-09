package com.niumi.database.converter

import androidx.room.TypeConverter
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.database.EffectStatus

/**
 * Convertisseurs Room des enums de `SessionEffectOutboxEntity` (SPEC_ANDROID §7.2). Même
 * discipline que [SessionEnumConverters] : stockage par `name`, nom inconnu jamais avalé.
 * `EffectStatus` n'a pas d'équivalent dans `:shared:core` (§6.1 laisse sa forme native).
 */
class EffectEnumConverters {
    @TypeConverter
    fun fromSessionEffectKind(value: SessionEffectKindDto): String = value.name

    @TypeConverter
    fun toSessionEffectKind(value: String): SessionEffectKindDto = enumValueOf(value)

    @TypeConverter
    fun fromEffectStatus(value: EffectStatus): String = value.name

    @TypeConverter
    fun toEffectStatus(value: String): EffectStatus = enumValueOf(value)
}
