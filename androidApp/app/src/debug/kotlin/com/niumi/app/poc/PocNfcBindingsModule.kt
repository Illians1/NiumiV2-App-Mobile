package com.niumi.app.poc

import com.niumi.database.pairing.PairedBoxStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Bindings de la route POC (debug uniquement, SPEC_ANDROID §22 Lot 0). Supprimé avec le reste de
 * la route POC à l'étape 21.
 *
 * **`NfcScanHandler` n'est plus lié ici depuis l'étape 18** : `HandleValidNfcUseCase`
 * (`:core:system`) occupe la liaison unique dans tous les variants, et le handler POC — qui
 * arrêtait le son d'une session fictive sans toucher à la persistance — a été supprimé. Une
 * alarme lancée depuis `PocScreen` n'est donc plus arrêtable par un scan : elle ne correspond à
 * aucune session en base, et le cas d'usage réel l'ignore. La route POC est redondante dès lors
 * que le parcours réel fonctionne de bout en bout ; la rafistoler par un arrêt direct du service
 * contredirait §11.3 et le critère de clôture de l'étape 17.
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
    @PocPairedBoxStore
    fun bindPairedBoxStore(impl: DebugPairedBoxStore): PairedBoxStore
}
