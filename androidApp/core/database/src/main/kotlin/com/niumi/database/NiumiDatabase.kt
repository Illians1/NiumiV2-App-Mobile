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
 * chaque schéma est committé dans `androidApp/core/database/schemas/` (bloc `room {}` du
 * build.gradle.kts), lu par `NiumiDatabaseSchemaTest` et `MigrationTestHelper`.
 *
 * v2 (étape 16) ajoute à `technical_event` les trois champs de contexte de SPEC_ANDROID §17
 * (modèle de l'appareil, version Android, version de l'application). La migration est additive et
 * ne touche aucune autre table : voir [com.niumi.database.migration.MIGRATION_1_2].
 *
 * v3 (Lot 6, blocage différé) ajoute à `alarm_session` les quatre colonnes `blocking*` de
 * SPEC_ANDROID §7.2, toutes nullables. Migration additive elle aussi :
 * voir [com.niumi.database.migration.MIGRATION_2_3].
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
    version = 3,
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
