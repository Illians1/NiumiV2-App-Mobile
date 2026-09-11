package com.niumi.app.poc

import javax.inject.Qualifier

/**
 * Qualifie le [com.niumi.database.pairing.PairedBoxStore] de la route POC (debug uniquement,
 * SPEC_ANDROID §22 Lot 0), lié à [DebugPairedBoxStore]. Depuis l'étape 13, la liaison de
 * production sans qualificatif pointe vers `RoomPairedBoxStore` (`:core:database`,
 * `SingletonComponent`, présente dans tous les variants) : un second `@Binds` non qualifié
 * entrerait en conflit. Le qualificatif garde la route POC sur son propre dépôt tout en
 * conservant l'injection par interface (substituable en test).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PocPairedBoxStore
