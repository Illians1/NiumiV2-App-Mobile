package com.niumi.app

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SPEC_ANDROID §15 (décision du 2026-10-23) : `MainActivity` et `AlarmActivity` sont verrouillées en
 * portrait. Le test lit le manifeste fusionné installé, pas les fichiers sources : il protège contre un
 * retrait accidentel de l'attribut dans l'un des deux modules ou par une fusion de manifestes.
 *
 * Nécessite un appareil ou un émulateur : voir « Validation sur appareil réel » dans `CLAUDE.md`.
 */
@RunWith(AndroidJUnit4::class)
class ManifestOrientationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun mainActivityIsLockedToPortrait() {
        assertThat(declaredOrientationOf("com.niumi.app.MainActivity"))
            .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    @Test
    fun alarmActivityIsLockedToPortrait() {
        assertThat(declaredOrientationOf("com.niumi.feature.ringing.AlarmActivity"))
            .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    private fun declaredOrientationOf(className: String): Int {
        val component = ComponentName(context.packageName, className)
        val info = context.packageManager.getActivityInfo(component, PackageManager.ComponentInfoFlags.of(0))
        return info.screenOrientation
    }
}
