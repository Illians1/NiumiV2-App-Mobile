package com.niumi.database.converter

import androidx.room.TypeConverter
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto

/**
 * Convertisseurs Room des enums de `SessionIncidentEntity` (SPEC_ANDROID §7.2). Même discipline
 * que [SessionEnumConverters] : stockage par `name`, nom inconnu jamais avalé.
 */
class IncidentEnumConverters {
    @TypeConverter
    fun fromIncidentSeverity(value: IncidentSeverityDto): String = value.name

    @TypeConverter
    fun toIncidentSeverity(value: String): IncidentSeverityDto = enumValueOf(value)

    @TypeConverter
    fun fromPlatform(value: PlatformDto): String = value.name

    @TypeConverter
    fun toPlatform(value: String): PlatformDto = enumValueOf(value)
}
