package com.niumi.system.nfc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Filtre pur de [nfcAdapterSettledChanges], testable en JVM. Seuls les états stables comptent :
 * rejouer un diagnostic sur `STATE_TURNING_ON` lirait encore le NFC éteint.
 */
public object NfcAdapterStates {
    public fun isSettled(state: Int): Boolean = state == NfcAdapter.STATE_ON || state == NfcAdapter.STATE_OFF
}

/**
 * Émet chaque fois que le NFC **finit** de s'allumer ou de s'éteindre, tant que le flux est
 * collecté (SPEC_ANDROID §13, écart 10 de `RELEASE_REPORT.md`).
 *
 * Le retour du focus ne suffit pas pour le NFC. Mesuré le 2026-09-29 sur Xiaomi 25080RABDG /
 * Android 16 : l'allumage prend environ 1,4 s, et le volet rapide était refermé avant la fin
 * (focus rendu 0,35 à 0,85 s avant `enableInternal: end`, huit allumages sur neuf). Le diagnostic
 * rejoué au retour du focus lisait alors, à raison, un NFC encore éteint. L'extinction prend
 * 80 ms et ne présente pas ce défaut.
 *
 * **`RECEIVER_EXPORTED` est voulu.** L'annonce vient du service NFC, qui tourne sous l'identité
 * `nfc` et non `system` : un receveur non exporté n'accepte que sa propre application et
 * `system`. L'exporter n'ouvre rien : `ACTION_ADAPTER_STATE_CHANGED` est une annonce protégée,
 * qu'aucune application ne peut émettre (le shell lui-même est refusé, vérifié le 2026-09-29).
 */
public fun Context.nfcAdapterSettledChanges(): Flow<Unit> =
    callbackFlow {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    val state = intent?.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, UNKNOWN_STATE) ?: return
                    if (NfcAdapterStates.isSettled(state)) trySend(Unit)
                }
            }
        registerReceiver(
            receiver,
            IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED),
            Context.RECEIVER_EXPORTED,
        )
        awaitClose { unregisterReceiver(receiver) }
    }

private const val UNKNOWN_STATE = -1
