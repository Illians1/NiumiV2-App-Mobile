package com.niumi.system.nfc.di

import android.content.Context
import com.niumi.system.nfc.HandleValidNfcUseCase
import com.niumi.system.nfc.NfcReader
import com.niumi.system.nfc.NfcScanHandler
import com.niumi.system.nfc.ReaderModeNfcReader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings NFC (SPEC_ANDROID §11). Module séparé de [com.niumi.system.di.SystemModule], déjà à
 * son plafond `TooManyFunctions` de detekt (ETAPE-03.md, décision 6).
 *
 * **Une seule liaison depuis l'étape 18.** Jusque-là, deux implémentations concurrentes de
 * [NfcScanHandler] coexistaient et imposaient deux indirections : un `@BindsOptionalOf` pour
 * `AlarmActivity`, parce qu'aucune implémentation ne vivait dans `main` en release (la seule,
 * `PocNfcScanHandler`, était dans `src/debug` de `:app`) ; et un qualificatif
 * `@SessionNfcScanHandler` pour l'écran 9, afin qu'il ne capte pas le handler POC en debug et
 * n'affiche pas un « Session annulée » mensonger.
 *
 * `HandleValidNfcUseCase` supprime les deux raisons d'un coup : il est la seule implémentation,
 * présente dans tous les variants, et les deux écrans doivent désormais s'en servir. Conserver un
 * `Optional` qui ne peut plus être vide et un qualificatif à candidat unique aurait laissé deux
 * branches mortes jusqu'à l'étape 21.
 */
@Module
@InstallIn(SingletonComponent::class)
interface NfcHandlerModule {
    @Binds
    fun bindNfcScanHandler(impl: HandleValidNfcUseCase): NfcScanHandler
}

@Module
@InstallIn(SingletonComponent::class)
object NfcReaderModule {
    @Provides
    @Singleton
    fun provideNfcReader(
        @ApplicationContext context: Context,
    ): NfcReader = ReaderModeNfcReader(context)
}
