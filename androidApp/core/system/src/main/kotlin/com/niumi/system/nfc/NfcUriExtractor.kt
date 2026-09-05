package com.niumi.system.nfc

/**
 * Décode le premier enregistrement URI reconnu d'un tag NFC (SPEC_ANDROID §11.2), selon le
 * format RTD-URI 1.0 du NFC Forum : un octet de préfixe abrégé suivi du reste de l'URI en
 * UTF-8. Implémentation volontairement indépendante d'`android.nfc.NdefRecord.toUri()`, qui
 * normalise le schéma en minuscules — une normalisation que SPEC_CORE_KMP §9.1 interdit
 * d'appliquer avant le parseur commun (décision consignée dans le rapport d'étape 4). Les
 * enregistrements suivants sont ignorés dès qu'une URI valide est trouvée (§11.2).
 */
public object NfcUriExtractor {
    // SPEC_ANDROID §16 : borner la taille du payload avant toute allocation. 96 octets UTF-8
    // est la limite du payload NFC complet (SPEC_CORE_KMP §9.1) ; l'octet de préfixe RTD-URI
    // n'en fait pas partie côté texte, donc le budget disponible pour le reste est de 96 octets.
    private const val MAX_URI_BYTES = 96

    // Table des préfixes RTD-URI 1.0 (NFC Forum), index = octet de préfixe. Un index hors de
    // cette table (0x24 et au-delà) est réservé et invalide.
    private val URI_PREFIXES =
        listOf(
            "",
            "http://www.",
            "https://www.",
            "http://",
            "https://",
            "tel:",
            "mailto:",
            "ftp://anonymous:anonymous@",
            "ftp://ftp.",
            "ftps://",
            "sftp://",
            "smb://",
            "nfs://",
            "ftp://",
            "dav://",
            "news:",
            "telnet://",
            "imap:",
            "rtsp://",
            "urn:",
            "pop:",
            "sip:",
            "sips:",
            "tftp:",
            "btspp://",
            "btl2cap://",
            "btgoep://",
            "tcpobex://",
            "irdaobex://",
            "file://",
            "urn:epc:id:",
            "urn:epc:tag:",
            "urn:epc:pat:",
            "urn:epc:raw:",
            "urn:epc:",
            "urn:nfc:",
        )

    public fun firstUri(records: List<NdefRecordData>): String? = records.firstNotNullOfOrNull(::decodeUriRecord)

    private fun decodeUriRecord(record: NdefRecordData): String? =
        when {
            record.tnf == NdefRecordData.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecordData.RTD_URI) -> {
                decodeWellKnownUri(record.payload)
            }

            record.tnf == NdefRecordData.TNF_ABSOLUTE_URI -> {
                decodeAbsoluteUri(record.payload)
            }

            else -> {
                null
            }
        }

    private fun decodeWellKnownUri(payload: ByteArray): String? {
        if (payload.isEmpty()) return null
        val prefixCode = payload[0].toInt() and 0xFF
        val prefix = URI_PREFIXES.getOrNull(prefixCode)
        val remainder = payload.copyOfRange(1, payload.size)
        return if (prefix == null || remainder.size > MAX_URI_BYTES) {
            null
        } else {
            prefix + remainder.decodeToString()
        }
    }

    private fun decodeAbsoluteUri(payload: ByteArray): String? {
        if (payload.isEmpty() || payload.size > MAX_URI_BYTES) return null
        return payload.decodeToString()
    }
}
