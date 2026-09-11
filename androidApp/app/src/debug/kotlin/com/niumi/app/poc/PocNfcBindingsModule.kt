package com.niumi.app.poc

import com.niumi.database.pairing.PairedBoxStore
import com.niumi.system.nfc.NfcScanHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Bindings NFC de la route POC (debug uniquement, SPEC_ANDROID §22 Lot 0). Ce module entier vit
 * dans `src/debug` : en release, `Optional<NfcScanHandler>` (`@BindsOptionalOf` dans
 * `:core:system`) reste vide jusqu'à l'étape 18. Supprimé avec le reste de la route POC à
 * l'étape 21.
 *
 * `NiumiCoreFacade` n'est plus fournie ici depuis l'étape 11 : `SessionModule`
 * (`:core:system`, présent dans tous les variants) la fournit désormais en production, la route
 * POC la consomme telle quelle.
 *
 * `PairedBoxStore` est lié sous le qualificatif [PocPairedBoxStore] depuis l'étape 13 :
 * `RoomPairedBoxStore` (`:core:database`) occupe désormais la liaison non qualifiée dans
 * `SingletonComponent` pour tous les variants.
 */
@Module
@InstallIn(SingletonComponent::class)
interface PocNfcBindingsModule {
    @Binds
    fun bindNfcScanHandler(impl: PocNfcScanHandler): NfcScanHandler

    @Binds
    @PocPairedBoxStore
    fun bindPairedBoxStore(impl: DebugPairedBoxStore): PairedBoxStore
}
