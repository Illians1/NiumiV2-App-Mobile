package com.niumi.system.apps

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Noms et icônes sont lus en parallèle (étape 37 : 2,5 s à froid pour 125 applications lues une
 * à une, 0,6 s sur huit fils, mesuré le 2026-10-22 sur Xiaomi / Android 16). Le parallélisme ne
 * doit rien changer au résultat : mêmes applications, dans l'ordre de la requête, mêmes noms.
 */
@RunWith(AndroidJUnit4::class)
class PackageManagerPackageQueryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theParallelReadGivesTheSameEntriesInTheSameOrder() {
        val packageManager = context.packageManager

        // Lecture une à une, la référence ; la forme dépréciée vaut pour toutes les versions.
        @Suppress("DEPRECATION")
        val expected =
            packageManager
                .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }

        val entries = runBlocking { PackageManagerPackageQuery(context).launcherEntries() }

        assertThat(entries.map { it.packageName to it.label }).containsExactlyElementsIn(expected).inOrder()
        assertThat(entries.count { it.icon != null }).isEqualTo(entries.size)
    }
}
