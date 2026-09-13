package com.niumi.database.logging

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Lit le [DeviceContext] de SPEC_ANDROID §17 depuis les APIs Android. Séparé de `LoggingModule`
 * pour rester lisible et vérifiable par un test instrumenté : le module ne fait plus que fournir
 * le résultat.
 *
 * Une version d'application illisible donne `""` plutôt qu'une exception : le journal technique ne
 * doit jamais faire échouer le parcours qu'il observe (`RoomTechnicalEventLog`), et l'absence de
 * sa propre fiche de package est un état que seul un désinstallation en cours produit.
 */
fun readDeviceContext(context: Context): DeviceContext =
    DeviceContext(
        deviceModel = Build.MODEL.orEmpty(),
        androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        appVersion = readAppVersion(context),
    )

private fun readAppVersion(context: Context): String =
    runCatching {
        val info =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        "${info.versionName.orEmpty()} (${info.longVersionCode})"
    }.getOrDefault("")
