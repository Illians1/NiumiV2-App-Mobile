package com.niumi.system.power

import android.content.Context
import android.os.PowerManager

/**
 * Exemption d'optimisation de batterie (SPEC_ANDROID §13). **Détection structurellement
 * partielle** : `isIgnoringBatteryOptimizations()` n'observe que la liste blanche AOSP. La mesure
 * de l'étape 5 sur HyperOS a montré qu'un appareil peut geler Niumi alors que cette méthode
 * renvoie `false` après correction du réglage OEM, et inversement. Cette sonde ne décide donc
 * jamais seule du contrôle de disponibilité : elle sert à savoir s'il reste utile de proposer la
 * demande d'exemption AOSP.
 */
fun interface BatteryOptimizationStatus {
    fun isIgnoringBatteryOptimizations(): Boolean
}

class AndroidBatteryOptimizationStatus(
    private val context: Context,
) : BatteryOptimizationStatus {
    override fun isIgnoringBatteryOptimizations(): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)
}
