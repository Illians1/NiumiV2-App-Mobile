package com.niumi.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.niumi.database.converter.EffectEnumConverters
import com.niumi.database.converter.IncidentEnumConverters
import com.niumi.database.converter.SessionEnumConverters
import com.niumi.database.dao.ActiveSessionPointerDao
import com.niumi.database.dao.BlockedAppDao
import com.niumi.database.dao.IncidentDao
import com.niumi.database.dao.OutboxDao
import com.niumi.database.dao.PairedBoxDao
import com.niumi.database.dao.ReceiptDao
import com.niumi.database.dao.SessionDao
import com.niumi.database.dao.TechnicalEventDao
import com.niumi.database.entity.ActiveSessionPointerEntity
import com.niumi.database.entity.AlarmSessionEntity
import com.niumi.database.entity.BlockedAppEntity
import com.niumi.database.entity.PairedBoxEntity
import com.niumi.database.entity.SessionEffectOutboxEntity
import com.niumi.database.entity.SessionEventReceiptEntity
import com.niumi.database.entity.SessionIncidentEntity
import com.niumi.database.entity.TechnicalEventEntity

/**
 * Source persistante canonique après déverrouillage (SPEC_CORE_KMP §13). `exportSchema = true` :
 * le schéma v1 est committé dans `androidApp/core/database/schemas/` (bloc `room {}` du
 * build.gradle.kts), lu par `ExportedSchemaTest` et `MigrationTestHelper`.
 */
@Database(
    entities = [
        AlarmSessionEntity::class,
        BlockedAppEntity::class,
        PairedBoxEntity::class,
        TechnicalEventEntity::class,
        SessionIncidentEntity::class,
        SessionEventReceiptEntity::class,
        SessionEffectOutboxEntity::class,
        ActiveSessionPointerEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(SessionEnumConverters::class, EffectEnumConverters::class, IncidentEnumConverters::class)
abstract class NiumiDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun blockedAppDao(): BlockedAppDao

    abstract fun activeSessionPointerDao(): ActiveSessionPointerDao

    abstract fun receiptDao(): ReceiptDao

    abstract fun outboxDao(): OutboxDao

    abstract fun incidentDao(): IncidentDao

    abstract fun pairedBoxDao(): PairedBoxDao

    abstract fun technicalEventDao(): TechnicalEventDao
}
