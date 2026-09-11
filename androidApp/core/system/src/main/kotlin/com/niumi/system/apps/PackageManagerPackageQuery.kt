package com.niumi.system.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager

/**
 * Traduction Android de [PackageQuery], non testable en JVM (`PackageManager`). La visibilité des
 * paquets est obtenue par la section `<queries>` du manifeste de ce module, jamais par
 * `QUERY_ALL_PACKAGES` (SPEC_ANDROID §12.1, §14).
 */
class PackageManagerPackageQuery(
    private val context: Context,
) : PackageQuery {
    private val packageManager: PackageManager get() = context.packageManager

    override fun launcherEntries(): List<InstalledApp> =
        queryActivities(launcherIntent()).map { resolveInfo ->
            InstalledApp(
                packageName = resolveInfo.activityInfo.packageName,
                label =
                    runCatching { resolveInfo.loadLabel(packageManager).toString() }
                        .getOrNull()
                        .orEmpty()
                        .ifEmpty { resolveInfo.activityInfo.packageName },
                icon = runCatching { resolveInfo.loadIcon(packageManager) }.getOrNull(),
            )
        }

    /**
     * Tous les lanceurs installés, pas seulement le défaut : changer de lanceur pendant une
     * session rendrait l'appareil inutilisable si l'ancien avait été bloqué.
     */
    override fun homePackages(): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return queryActivities(homeIntent).map { it.activityInfo.packageName }.toSet() +
            setOfNotNull(homeIntent.resolveActivity(packageManager)?.packageName)
    }

    override fun settingsPackage(): String? =
        Intent(Settings.ACTION_SETTINGS).resolveActivity(packageManager)?.packageName

    /**
     * `TelecomManager.getDefaultDialerPackage()` est publique et ne demande aucune permission ;
     * `ACTION_DIAL` complète le cas où aucun composeur par défaut n'est déclaré.
     */
    override fun defaultDialerPackages(): Set<String> {
        val telecomManager = context.getSystemService(TelecomManager::class.java)
        val defaultDialer = runCatching { telecomManager?.defaultDialerPackage }.getOrNull()
        val dialIntentTarget = Intent(Intent.ACTION_DIAL).resolveActivity(packageManager)?.packageName
        return setOfNotNull(defaultDialer, dialIntentTarget)
    }

    private fun launcherIntent(): Intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    private fun queryActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
}
