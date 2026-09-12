package com.niumi.system.nfc

import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Handler du scan de sortie de session (écran 9), en attendant `HandleValidNfcUseCase` livré à
 * l'étape 18. Retourne [ScanOutcome.Ignored] : aucun état n'est modifié, ce qui est exactement le
 * comportement qu'exige SPEC_CORE_KMP §4 d'un scan qui ne peut pas être validé — « un NFC invalide
 * ne modifie ni l'état, ni le blocage, ni le son ». Ce n'est donc pas un faux comportement de
 * production (CLAUDE.md) : rien n'est simulé, la libération n'est simplement pas encore
 * implémentée, et l'écran 9 ne prétend à aucun succès.
 *
 * Le scan est tout de même journalisé : `NFC_SCAN_IGNORED` n'existe pas dans SPEC_ANDROID §17, et
 * aucun type n'y sera ajouté pour un état transitoire du développement — d'où
 * [TechnicalEventType.NFC_SCAN_INVALID], qui décrit fidèlement le fait qu'aucune validation n'a
 * eu lieu.
 */
@Singleton
class PendingNfcScanHandler
    @Inject
    constructor(
        private val technicalEventLog: TechnicalEventLog,
    ) : NfcScanHandler {
        override suspend fun onUriRead(uri: String): ScanOutcome {
            technicalEventLog.log(TechnicalEventType.NFC_SCAN_INVALID)
            return ScanOutcome.Ignored
        }
    }

/**
 * Qualifie le handler du scan de sortie de session, distinct du [NfcScanHandler] non qualifié que
 * la route POC lie en debug (`PocNfcScanHandler`). Sans ce qualificatif, l'écran 9 hériterait en
 * debug d'un handler qui retourne `Accepted` sur le boîtier associé sans toucher à la session
 * réelle, et afficherait un « Session annulée » mensonger. Écart au plan de l'étape 15, voir
 * `ETAPE-15.md`.
 *
 * À l'étape 18, seule la liaison change : `HandleValidNfcUseCase` prend cette place et le
 * `ScanToModifyViewModel` n'est pas touché.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionNfcScanHandler
