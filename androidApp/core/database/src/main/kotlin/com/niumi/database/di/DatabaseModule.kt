package com.niumi.database.di

import android.content.Context
import androidx.room.Room
import com.niumi.database.NiumiDatabase
import com.niumi.database.migration.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "niumi.db"

/**
 * Fournit l'instance unique de [NiumiDatabase]. `Room.databaseBuilder(...).build()` est paresseux
 * (aucune connexion SQLite ouverte avant la première requête) : la simple résolution de ce binding
 * par Hilt n'ouvre pas la base. Aucun composant `directBootAware` (`AlarmReceiver`,
 * `AlarmRingingService`, `AlarmActivity`) ne dépend encore de ce module à cette étape — voir
 * `ETAPE-09.md` : la garde `ROOM_BEFORE_UNLOCK` de SPEC_ANDROID §7.3 arrive à l'étape 10.
 *
 * Aucun `fallbackToDestructiveMigration` : une migration manquante doit faire échouer l'ouverture
 * plutôt que d'effacer silencieusement une session active et son journal (§13, §18).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideNiumiDatabase(
        @ApplicationContext context: Context,
    ): NiumiDatabase =
        Room
            .databaseBuilder(context, NiumiDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()
}
