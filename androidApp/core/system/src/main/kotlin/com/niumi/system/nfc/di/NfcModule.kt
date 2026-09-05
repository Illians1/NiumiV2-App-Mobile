package com.niumi.system.nfc.di

import android.content.Context
import com.niumi.system.nfc.NfcReader
import com.niumi.system.nfc.NfcScanHandler
import com.niumi.system.nfc.ReaderModeNfcReader
import dagger.BindsOptionalOf
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
 * [NfcScanHandler] n'a aucune implémentation en `main` avant l'étape 18 : la seule
 * implémentation avant cette étape (`PocNfcScanHandler`) vit dans `src/debug` de `:app`.
 * `@BindsOptionalOf` permet à `AlarmActivity` d'injecter `Optional<NfcScanHandler>` — présent
 * en debug, absent en release — sans qu'aucun binding no-op ne vive dans `main` (CLAUDE.md :
 * pas de faux comportement de production).
 */
@Module
@InstallIn(SingletonComponent::class)
interface NfcHandlerModule {
    @BindsOptionalOf
    fun optionalNfcScanHandler(): NfcScanHandler
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
