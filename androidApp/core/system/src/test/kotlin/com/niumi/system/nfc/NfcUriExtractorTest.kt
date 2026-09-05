package com.niumi.system.nfc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SPEC_ANDROID §11.2 : le lecteur transmet l'URI brute au parseur commun sans dupliquer de
 * règle de validité. Aucune normalisation ne doit être appliquée ici : SPEC_CORE_KMP §9.1
 * interdit au parseur d'accepter une forme qu'une normalisation permissive rendrait
 * équivalente à la forme canonique, donc l'extracteur ne doit ni minusculiser ni modifier le
 * schéma. `NdefRecord.toUri()` d'Android le ferait (décision consignée dans le rapport
 * d'étape) — cet extracteur décode donc lui-même le format RTD-URI (NFC Forum).
 */
class NfcUriExtractorTest {
    @Test
    fun wellKnownUriRecordWithNoPrefixDecodesToRawUri() {
        val payload = byteArrayOf(0x00) + "niumi://box/v1/x?token=y".encodeToByteArray()
        val record = NdefRecordData(NdefRecordData.TNF_WELL_KNOWN, NdefRecordData.RTD_URI, payload)

        val uri = NfcUriExtractor.firstUri(listOf(record))

        assertThat(uri).isEqualTo("niumi://box/v1/x?token=y")
    }

    @Test
    fun wellKnownUriRecordWithHttpsWwwPrefixExpandsAbbreviation() {
        // Code 0x02 du tableau RTD-URI 1.0 = "https://www.".
        val payload = byteArrayOf(0x02) + "example.com".encodeToByteArray()
        val record = NdefRecordData(NdefRecordData.TNF_WELL_KNOWN, NdefRecordData.RTD_URI, payload)

        val uri = NfcUriExtractor.firstUri(listOf(record))

        assertThat(uri).isEqualTo("https://www.example.com")
    }

    @Test
    fun absoluteUriRecordDecodesPayloadDirectly() {
        val payload = "niumi://box/v1/x?token=y".encodeToByteArray()
        val record = NdefRecordData(NdefRecordData.TNF_ABSOLUTE_URI, byteArrayOf(), payload)

        val uri = NfcUriExtractor.firstUri(listOf(record))

        assertThat(uri).isEqualTo("niumi://box/v1/x?token=y")
    }

    @Test
    fun textOnlyRecordYieldsNull() {
        val textType = byteArrayOf('T'.code.toByte())
        val record = NdefRecordData(NdefRecordData.TNF_WELL_KNOWN, textType, byteArrayOf(0x02, 'e'.code.toByte()))

        val uri = NfcUriExtractor.firstUri(listOf(record))

        assertThat(uri).isNull()
    }

    @Test
    fun emptyRecordListYieldsNull() {
        assertThat(NfcUriExtractor.firstUri(emptyList())).isNull()
    }

    @Test
    fun secondRecordIsIgnoredWhenFirstIsAlreadyAUri() {
        val first = byteArrayOf(0x00) + "niumi://box/v1/first".encodeToByteArray()
        val second = byteArrayOf(0x00) + "niumi://box/v1/second".encodeToByteArray()
        val records =
            listOf(
                NdefRecordData(NdefRecordData.TNF_WELL_KNOWN, NdefRecordData.RTD_URI, first),
                NdefRecordData(NdefRecordData.TNF_WELL_KNOWN, NdefRecordData.RTD_URI, second),
            )

        val uri = NfcUriExtractor.firstUri(records)

        assertThat(uri).isEqualTo("niumi://box/v1/first")
    }

    @Test
    fun nonUriRecordFollowedByUriRecordReturnsTheUriRecord() {
        val textType = byteArrayOf('T'.code.toByte())
        val textRecord = NdefRecordData(NdefRecordData.TNF_WELL_KNOWN, textType, byteArrayOf(0x02, 'e'.code.toByte()))
        val uriPayload = byteArrayOf(0x00) + "niumi://box/v1/x?token=y".encodeToByteArray()
        val uriRecord = NdefRecordData(NdefRecordData.TNF_WELL_KNOWN, NdefRecordData.RTD_URI, uriPayload)

        val uri = NfcUriExtractor.firstUri(listOf(textRecord, uriRecord))

        assertThat(uri).isEqualTo("niumi://box/v1/x?token=y")
    }

    @Test
    fun payloadLargerThan96BytesYieldsNull() {
        // SPEC_ANDROID §16 : borner la taille avant toute allocation. 97 octets utiles après
        // l'octet de préfixe, soit un payload total de 98 octets.
        val payload = byteArrayOf(0x00) + ByteArray(97) { 'a'.code.toByte() }
        val record = NdefRecordData(NdefRecordData.TNF_WELL_KNOWN, NdefRecordData.RTD_URI, payload)

        val uri = NfcUriExtractor.firstUri(listOf(record))

        assertThat(uri).isNull()
    }

    @Test
    fun uppercaseSchemeIsNeverLowercased() {
        // Le parseur commun doit voir la forme brute pour rejeter NIUMI:// (SPEC_CORE_KMP §9.1).
        val payload = byteArrayOf(0x00) + "NIUMI://box/v1/x?token=y".encodeToByteArray()
        val record = NdefRecordData(NdefRecordData.TNF_WELL_KNOWN, NdefRecordData.RTD_URI, payload)

        val uri = NfcUriExtractor.firstUri(listOf(record))

        assertThat(uri).isEqualTo("NIUMI://box/v1/x?token=y")
    }

    @Test
    fun emptyPayloadYieldsNull() {
        val record = NdefRecordData(NdefRecordData.TNF_WELL_KNOWN, NdefRecordData.RTD_URI, byteArrayOf())

        val uri = NfcUriExtractor.firstUri(listOf(record))

        assertThat(uri).isNull()
    }

    @Test
    fun outOfRangePrefixCodeYieldsNull() {
        // Le tableau RTD-URI 1.0 ne définit que les codes 0x00 à 0x23.
        val payload = byteArrayOf(0x24.toByte()) + "rest".encodeToByteArray()
        val record = NdefRecordData(NdefRecordData.TNF_WELL_KNOWN, NdefRecordData.RTD_URI, payload)

        val uri = NfcUriExtractor.firstUri(listOf(record))

        assertThat(uri).isNull()
    }
}
