package com.niumi.system.nfc.di

import android.content.Context
import com.niumi.system.nfc.NfcReader
import com.niumi.system.nfc.NfcScanHandler
import com.niumi.system.nfc.PendingNfcScanHandler
import com.niumi.system.nfc.ReaderModeNfcReader
import com.niumi.system.nfc.SessionNfcScanHandler
import dagger.Binds
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
 * [NfcScanHandler] non qualifié n'a aucune implémentation en `main` avant l'étape 18 : la seule
 * (`PocNfcScanHandler`) vit dans `src/debug` de `:app`. `@BindsOptionalOf` permet à
 * `AlarmActivity` d'injecter `Optional<NfcScanHandler>` — présent en debug, absent en release —
 * sans qu'aucun binding no-op ne vive dans `main` (CLAUDE.md : pas de faux comportement de
 * production).
 *
 * Le scan de sortie de session (écran 9) emploie au contraire une liaison **qualifiée** et
 * toujours présente, [SessionNfcScanHandler] : un `Optional` vide y aurait laissé l'écran hériter
 * du handler POC en debug. Voir [PendingNfcScanHandler].
 */
@Module
@InstallIn(SingletonComponent::class)
interface NfcHandlerModule {
    @BindsOptionalOf
    fun optionalNfcScanHandler(): NfcScanHandler

    /**
     * Liaison qualifiée du scan de sortie de session (écran 9, étape 15). Non optionnelle et
     * présente dans tous les variants, contrairement à celle ci-dessus : l'écran 9 doit se
     * comporter de la même façon en debug et en release, et surtout ne jamais capter
     * `PocNfcScanHandler` — voir [com.niumi.system.nfc.SessionNfcScanHandler].
     */
    @Binds
    @SessionNfcScanHandler
    fun bindSessionNfcScanHandler(impl: PendingNfcScanHandler): NfcScanHandler
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
