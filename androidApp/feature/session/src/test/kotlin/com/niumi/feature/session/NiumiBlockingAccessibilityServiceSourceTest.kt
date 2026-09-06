package com.niumi.feature.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Garde-fou permanent de SPEC_ANDROID §12.2 : « [le service] ne doit pas parcourir l'arbre
 * d'accessibilité, lire le texte affiché, inspecter les saisies ». Vérifié en lisant le source
 * du service directement plutôt qu'en exécutant le code (le service ne peut pas s'exécuter hors
 * d'un appareil connecté à l'accessibilité) — même mécanisme que `ModuleListTest` (`:app`) et
 * `NiumiAlarmWavTest` (`:feature:ringing`). Remplace le grep manuel exigé par le plan MVP.
 */
class NiumiBlockingAccessibilityServiceSourceTest {
    @Test
    fun serviceSourceNeverTouchesWindowContentOrText() {
        val rootDir =
            File(
                requireNotNull(System.getProperty("niumi.rootDir")) {
                    "La propriété système niumi.rootDir n'a pas été injectée par le build Gradle."
                },
            )
        val serviceFile =
            File(
                rootDir,
                "androidApp/feature/session/src/main/kotlin/com/niumi/feature/session/blocking/" +
                    "NiumiBlockingAccessibilityService.kt",
            )
        assertThat(serviceFile.exists()).isTrue()

        val source = serviceFile.readText()
        val forbiddenTokens =
            listOf("rootInActiveWindow", "getText", "contentDescription", "AccessibilityNodeInfo")
        assertThat(forbiddenTokens.filter { source.contains(it) }).isEmpty()
    }
}
