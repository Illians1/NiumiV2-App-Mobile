package com.niumi.system.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Traduction Android de [PackageQuery], non testable en JVM (`PackageManager`). La visibilité des
 * paquets est obtenue par la section `<queries>` du manifeste de ce module, jamais par
 * `QUERY_ALL_PACKAGES` (SPEC_ANDROID §12.1, §14).
 */
class PackageManagerPackageQuery(
    private val context: Context,
    private val loadDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(LOAD_PARALLELISM),
) : PackageQuery {
    private val packageManager: PackageManager get() = context.packageManager

    /**
     * Noms et icônes sont lus en parallèle, l'ordre de la requête conservé. Mesuré le 2026-10-22
     * sur Xiaomi 25080RABDG / Android 16, 125 applications, processus neuf : 2,5 s lues une à une
     * (1,0 s de noms, 1,5 s d'icônes), 0,6 s sur huit fils. La requête elle-même prend 20 ms.
     */
    override suspend fun launcherEntries(): List<InstalledApp> =
        coroutineScope {
            queryActivities(launcherIntent())
                .map { resolveInfo -> async(loadDispatcher) { entryOf(resolveInfo) } }
                .awaitAll()
        }

    private fun entryOf(resolveInfo: ResolveInfo): InstalledApp =
        InstalledApp(
            packageName = resolveInfo.activityInfo.packageName,
            label =
                runCatching { resolveInfo.loadLabel(packageManager).toString() }
                    .getOrNull()
                    .orEmpty()
                    .ifEmpty { resolveInfo.activityInfo.packageName },
            icon = runCatching { resolveInfo.loadIcon(packageManager) }.getOrNull(),
        )

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

    private companion object {
        /** Huit lectures à la fois, la valeur mesurée (0,6 s) ; les autres n'ont pas été essayées. */
        const val LOAD_PARALLELISM = 8
    }
}
