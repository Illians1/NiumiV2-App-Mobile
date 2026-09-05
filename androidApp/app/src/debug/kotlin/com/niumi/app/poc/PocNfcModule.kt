package com.niumi.app.poc

import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.system.nfc.NfcScanHandler
import com.niumi.system.pairing.PairedBoxStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings NFC de la route POC (debug uniquement, SPEC_ANDROID §22 Lot 0). Ce module entier vit
 * dans `src/debug` : en release, `Optional<NfcScanHandler>` (`@BindsOptionalOf` dans
 * `:core:system`) reste vide jusqu'à l'étape 18. Supprimé avec le reste de la route POC à
 * l'étape 21.
 */
@Module
@InstallIn(SingletonComponent::class)
interface PocNfcBindingsModule {
    @Binds
    fun bindNfcScanHandler(impl: PocNfcScanHandler): NfcScanHandler

    @Binds
    fun bindPairedBoxStore(impl: DebugPairedBoxStore): PairedBoxStore
}

@Module
@InstallIn(SingletonComponent::class)
object PocFacadeModule {
    // `NiumiCoreFacade` (:shared:core) n'a pas de constructeur @Inject : SPEC_CORE_KMP §3.2
    // garde ce module libre de toute annotation de plateforme, y compris Hilt/javax.inject.
    @Provides
    @Singleton
    fun provideNiumiCoreFacade(): NiumiCoreFacade = NiumiCoreFacade()
}
