package com.niumi.system.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord

/**
 * Traducteur Android → descripteur pur (SPEC_ANDROID §11.2). Recopie les trois champs bruts
 * d'un `NdefMessage` sans appeler `NdefRecord.toUri()`, qui normalise le schéma en minuscules
 * (décision consignée dans ETAPE-04.md). Non testable en JVM : `NdefRecord` est un stub sans
 * appareil ; la logique de décodage testable vit dans [NfcUriExtractor].
 */
public object AndroidNdefReader {
    public fun toRecordData(message: NdefMessage): List<NdefRecordData> =
        message.records.map { record ->
            NdefRecordData(tnf = record.tnf, type = record.type, payload = record.payload)
        }
}
