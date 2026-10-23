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
 *
 * Depuis l'étape 39 de la refonte, l'écran 7 reste affiché jusqu'à un bouton : chaque autre chemin
 * de retrait (fin du blocage, extinction de l'écran, interruption, déliaison) doit exister dans le
 * source, et aucune minuterie ne doit revenir dans `blocking/`.
 */
class NiumiBlockingAccessibilityServiceSourceTest {
    private val rootDir =
        File(
            requireNotNull(System.getProperty("niumi.rootDir")) {
                "La propriété système niumi.rootDir n'a pas été injectée par le build Gradle."
            },
        )
    private val blockingDir =
        File(rootDir, "androidApp/feature/session/src/main/kotlin/com/niumi/feature/session/blocking")
    private val serviceSource: String
        get() {
            val serviceFile = File(blockingDir, "NiumiBlockingAccessibilityService.kt")
            assertThat(serviceFile.exists()).isTrue()
            return serviceFile.readText()
        }

    @Test
    fun serviceSourceNeverTouchesWindowContentOrText() {
        val forbiddenTokens =
            listOf("rootInActiveWindow", "getText", "contentDescription", "AccessibilityNodeInfo")
        assertThat(forbiddenTokens.filter { serviceSource.contains(it) }).isEmpty()
    }

    @Test
    fun theBlockScreenIsHiddenOnInterruptUnbindEndOfBlockingAndScreenOff() {
        val source = serviceSource
        assertThat(bodyOf(source, "override fun onInterrupt()")).contains("hide()")
        assertThat(bodyOf(source, "override fun onUnbind(")).contains("hide()")
        assertThat(bodyOf(source, "private fun onBlockingDeactivated()")).contains("hide()")
        assertThat(bodyOf(source, "override fun onReceive(")).contains("hide()")
        assertThat(source).contains("Intent.ACTION_SCREEN_OFF")
        assertThat(bodyOf(source, "override fun onUnbind(")).contains("unregisterReceiver(")
    }

    /**
     * Un blocage décidé écran éteint (début différé rejoué sur un paquet périmé, §12.4) renvoie à
     * l'accueil mais ne pose pas l'écran 7 : aucune extinction ne viendrait plus le retirer, et il
     * paraîtrait par-dessus l'écran de verrouillage au rallumage.
     */
    @Test
    fun theBlockScreenIsNeverShownWhileTheScreenIsOff() {
        assertThat(bodyOf(serviceSource, "private fun showBlockScreen(")).contains("isInteractive")
    }

    /** Décision du 2026-10-22 : une application non bloquée au premier plan (appel entrant) retire l'écran 7. */
    @Test
    fun anotherApplicationInTheForegroundHidesTheBlockScreen() {
        val branch = bodyOf(serviceSource, "BlockAction.None ->")
        assertThat(branch).contains("BlockScreenDismissal.dismisses(")
        assertThat(branch).contains("hide()")
    }

    /** Q6 (2026-10-22) : l'écran 7 n'a plus de minuterie ; il part sur un geste ou un événement. */
    @Test
    fun noTimerRemainsInTheBlockingPackage() {
        val sources = blockingDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertThat(sources).isNotEmpty()
        val timed = sources.filter { file -> TIMER_TOKENS.any { file.readText().contains(it) } }
        assertThat(timed.map { it.name }).isEmpty()
    }

    /** Corps d'une fonction : du premier `{` après [signature] à l'accolade qui le referme. */
    private fun bodyOf(
        source: String,
        signature: String,
    ): String {
        val start = source.indexOf(signature)
        assertThat(start).isAtLeast(0)
        val open = source.indexOf('{', start)
        var depth = 0
        for (index in open until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return source.substring(open, index + 1)
            }
        }
        error("Corps de « $signature » non refermé")
    }

    private companion object {
        val TIMER_TOKENS = listOf("postDelayed", "Handler(", "OVERLAY_DISPLAY_DURATION")
    }
}
