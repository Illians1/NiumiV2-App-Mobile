package com.niumi.system.recents.di

import android.content.Context
import com.niumi.system.recents.AndroidRecentsLockStatus
import com.niumi.system.recents.RecentsLockStatus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/**
 * Liaison de [RecentsLockStatus] (étape 25) : le verrou HyperOS dans les récents, seule protection
 * mesurée contre « Tout effacer ». Module dédié plutôt qu'ajouté à `SystemModule`, déjà à son
 * plafond `TooManyFunctions` (11).
 *
 * Il fournissait aussi `RecentsCard`, qui retirait la carte de Niumi des récents pendant une
 * session ; retiré le 2026-10-19 (correctif A8) : le service de garde de session protège du
 * balayage de la carte (SPEC_ANDROID §12.5), et l'exclusion ne couvrait pas les récents ouverts
 * depuis Niumi.
 */
@Module
@InstallIn(SingletonComponent::class)
object RecentsModule {
    @Provides
    fun provideRecentsLockStatus(
        @ApplicationContext context: Context,
    ): RecentsLockStatus = AndroidRecentsLockStatus(context)
}
