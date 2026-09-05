package com.niumi.system.nfc

/**
 * Descripteur pur d'un enregistrement NDEF (TNF, type, charge utile), sans dépendance à
 * `android.nfc.NdefRecord` — même motif « descripteurs purs » que `PendingIntentSpec` dans
 * `:core:system` (ETAPE-03.md) : testable en JVM, traduit depuis le vrai `NdefMessage` par
 * `AndroidNdefReader`, qui n'est pas testable en JVM.
 */
public data class NdefRecordData(
    val tnf: Short,
    val type: ByteArray,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NdefRecordData) return false
        return tnf == other.tnf && type.contentEquals(other.type) && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = tnf.toInt()
        result = 31 * result + type.contentHashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }

    public companion object {
        // Valeurs NFC Forum, reprises sans dépendre de `android.nfc.NdefRecord` (stub en JVM).
        public const val TNF_WELL_KNOWN: Short = 0x01
        public const val TNF_ABSOLUTE_URI: Short = 0x03
        public val RTD_URI: ByteArray = byteArrayOf('U'.code.toByte())
    }
}
