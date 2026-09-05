package com.niumi.system.nfc

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.FormatException
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import com.niumi.system.common.OperationResult
import java.io.IOException

/**
 * Implémentation Android du Reader Mode (SPEC_ANDROID §4.4, §11.1, §11.2). Traducteur non
 * testable en JVM (motif « descripteurs purs », ETAPE-03.md) : la logique de décodage testable
 * vit dans [NfcUriExtractor], appelée ici après traduction par [AndroidNdefReader].
 */
public class ReaderModeNfcReader(
    private val context: Context,
) : NfcReader {
    private val nfcAdapter: NfcAdapter? get() = NfcAdapter.getDefaultAdapter(context)

    override val availability: NfcAvailability
        get() =
            when {
                !context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) -> NfcAvailability.ABSENT
                nfcAdapter?.isEnabled != true -> NfcAvailability.DISABLED
                else -> NfcAvailability.ENABLED
            }

    override fun start(
        activity: Activity,
        onUri: (String) -> Unit,
        onUnreadable: () -> Unit,
    ): OperationResult {
        val adapter = nfcAdapter ?: return OperationResult.Failure("NFC_ADAPTER_UNAVAILABLE")
        val extras =
            Bundle().apply {
                putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, READER_PRESENCE_CHECK_DELAY_MS)
            }
        val callback =
            NfcAdapter.ReaderCallback { tag -> handleTag(tag, onUri, onUnreadable) }
        adapter.enableReaderMode(activity, callback, NfcAdapter.FLAG_READER_NFC_A, extras)
        return OperationResult.Success
    }

    override fun stop(activity: Activity) {
        // `disableReaderMode` est idempotent côté Android : aucun état à vérifier ici.
        nfcAdapter?.disableReaderMode(activity)
    }

    // Invoqué sur un thread binder (hors thread principal) : les callbacks ne doivent faire
    // aucune hypothèse sur le thread appelant. Aucune exception ne doit s'échapper (§16) : un
    // tag physiquement illisible (déconnexion pendant la lecture, NDEF corrompu) devient
    // `onUnreadable()`, jamais une exception qui remonterait jusqu'au binder Android.
    @Suppress("SwallowedException")
    private fun handleTag(
        tag: Tag,
        onUri: (String) -> Unit,
        onUnreadable: () -> Unit,
    ) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            onUnreadable()
            return
        }
        try {
            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
            val uri = message?.let { NfcUriExtractor.firstUri(AndroidNdefReader.toRecordData(it)) }
            if (uri != null) {
                onUri(uri)
            } else {
                onUnreadable()
            }
        } catch (unreadableTag: IOException) {
            onUnreadable()
        } catch (malformedNdef: FormatException) {
            onUnreadable()
        } finally {
            runCatching { ndef.close() }
        }
    }

    private companion object {
        const val READER_PRESENCE_CHECK_DELAY_MS = 250
    }
}
