package com.niumi.system.power

import android.content.Context
import android.os.PowerManager

/**
 * Exemption d'optimisation de batterie (SPEC_ANDROID §13). Cette sonde **décide** du contrôle de
 * disponibilité. Mesuré le 2026-09-28 sur HyperOS : c'est la liste blanche AOSP qui empêche le
 * gel du blocage, que Niumi y soit inscrit par la page Android (« Sans restriction ») ou par le
 * réglage de la surcouche (« Pas de restriction »), qui l'y inscrit aussi. Les autres surcouches
 * ne sont pas mesurées.
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
