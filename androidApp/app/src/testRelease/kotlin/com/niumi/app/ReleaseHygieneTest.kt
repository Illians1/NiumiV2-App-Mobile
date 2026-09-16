package com.niumi.app

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Garde-fou permanent du build de publication (étape 21, SPEC_ANDROID §14, §16, §9.1, §22).
 *
 * **Pourquoi `testRelease` et non `test`.** Le classpath de `testDebugUnitTest` contient par
 * construction les classes de `src/debug` : y chercher l'absence d'une classe de debug ne
 * prouverait rien. Ce source set n'est compilé que pour la variante `release`, celle qui part
 * sur Play.
 *
 * Le manifeste fusionné n'est pas reconstruit ici : `androidApp/app/build.gradle.kts` branche
 * l'artefact `MERGED_MANIFEST` de la variante `release` sur la propriété système
 * `niumi.mergedManifest`, ce qui fait tourner `processReleaseManifest` avant ce test. Même
 * mécanisme que `niumi.rootDir` pour les sources (voir `ModuleListTest`).
 */
class ReleaseHygieneTest {
    private val rootDir =
        File(
            requireNotNull(System.getProperty("niumi.rootDir")) {
                "La propriété système niumi.rootDir n'a pas été injectée par le build Gradle."
            },
        )

    /**
     * Permissions du manifeste fusionné, lues en DOM et non par une recherche de texte : le
     * fusionneur **recopie les commentaires** des manifestes de chaque module, et ceux de
     * `:core:system` parlent justement des permissions interdites. Un `contains` les prendrait
     * pour des déclarations.
     */
    private fun usesPermissions(): Set<String> {
        val path =
            requireNotNull(System.getProperty("niumi.mergedManifest")) {
                "La propriété système niumi.mergedManifest n'a pas été injectée par le build Gradle."
            }
        val file = File(path)
        assertThat(file.invariantSeparatorsPath).contains("/release/")
        assertThat(file.isFile).isTrue()

        val document =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(file)
        return USES_PERMISSION_TAGS
            .flatMap { tag ->
                val nodes = document.getElementsByTagName(tag)
                (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(ANDROID_NAMESPACE, "name") }
            }.toSet()
    }

    /**
     * SPEC_ANDROID §14 fixe la liste exacte. Le test l'assert en **égalité** plutôt qu'en
     * absence des trois permissions interdites : une liste fermée attrape aussi celle qu'une
     * dépendance ajouterait demain sans que personne ne pense à l'interdire nommément.
     */
    @Test
    fun theReleaseManifestDeclaresExactlyTheNinePermissionsOfTheSpec() {
        val declared = usesPermissions().filterNot { it == GENERATED_RECEIVER_PERMISSION }

        assertThat(declared).containsExactlyElementsIn(SPEC_PERMISSIONS)
    }

    /** Les trois interdits de §14 et §16, nommés, pour que l'échec dise lequel a reparu. */
    @Test
    fun theReleaseManifestNeverDeclaresTheForbiddenPermissions() {
        assertThat(usesPermissions()).containsNoneIn(FORBIDDEN_PERMISSIONS)
    }

    /**
     * Aucune classe de POC, de doublure de test ni de dépôt de debug ne doit atteindre l'APK
     * de publication (CLAUDE.md : « pas de faux comportement de production »).
     */
    @Test
    fun theReleaseClasspathCarriesNoPocFakeOrDebugStoreClass() {
        val productionClasses = productionClasses()

        // Un balayage vide passerait tous les contrôles suivants sans rien prouver.
        assertThat(productionClasses).contains("com.niumi.app.NiumiApplication")
        assertThat(productionClasses.any { it.startsWith("com.niumi.system.") }).isTrue()

        val offenders =
            productionClasses.filter { name ->
                FORBIDDEN_CLASS_NAME.containsMatchIn(name.substringAfterLast('.'))
            }

        assertThat(offenders).isEmpty()
    }

    /**
     * SPEC_ANDROID §22 : « Aucun `TODO`, faux service, faux scan ou comportement silencieux ne
     * doit rester dans un lot déclaré terminé. »
     *
     * `STOP_RINGING_ACTION` est cherché **exactement** : `STOP_RINGING` seul est l'effet métier
     * de SPEC_CORE_KMP §6, présent en production à bon droit. C'est une action d'`Intent`
     * d'arrêt qui est interdite (§3, §10.2), pas l'effet qui arrête le son après un scan valide.
     */
    @Test
    fun theProductionSourcesCarryNoLeftoverMarker() {
        val forbidden = listOf("TODO", "FIXME", "STOP_RINGING_ACTION")

        val offenders =
            productionSources().flatMap { file ->
                val text = file.readText()
                forbidden.filter { text.contains(it) }.map { "${file.relativeTo(rootDir)} : $it" }
            }

        assertThat(offenders).isEmpty()
    }

    /**
     * SPEC_ANDROID §9.1 : `setAlarmClock()` est la seule API du **réveil**, et
     * `setExactAndAllowWhileIdle()` n'est toléré que par ses deux dérogations nommées — l'alarme de
     * secours de `RINGING` (§10.2, étape 20) et le début du blocage différé (§12.4, Lot 6). Toutes
     * deux tiennent au même raisonnement : ces alarmes ne sont pas des alarmes de l'utilisateur, et
     * les afficher au réglage « prochaine alarme » mentirait sur ce qu'elles sont.
     *
     * Le test verrouille chaque point d'appel **et** son fichier : une troisième API, ou la même API
     * dans un fichier de plus, échoue. C'est voulu — tout nouvel usage d'une API d'alarme doit être
     * acté ici en connaissance de cause, jamais glissé. `BlockingStartAlarmVisibilityTest` (`:app`,
     * instrumenté) prouve de son côté que la seconde dérogation tient sa promesse sur appareil : le
     * début du blocage n'atteint pas le réglage système.
     */
    @Test
    fun theOnlyAlarmSchedulingApisAreTheThreeAllowedByTheSpec() {
        val callSites =
            productionSources().flatMap { file ->
                val code = file.readText().withoutComments()
                val raw =
                    if (code.contains(ALARM_MANAGER_IMPORT) && RAW_SET_CALL.containsMatchIn(code)) {
                        listOf("${file.name} : set")
                    } else {
                        emptyList()
                    }
                raw +
                    ALARM_APIS
                        .filter { api -> code.contains(".$api(") }
                        .map { api -> "${file.name} : $api" }
            }

        assertThat(callSites)
            .containsExactly(
                "AndroidAlarmScheduler.kt : setAlarmClock",
                "AndroidRingingWatchdog.kt : setExactAndAllowWhileIdle",
                "AndroidBlockingStartScheduler.kt : setExactAndAllowWhileIdle",
            )
    }

    /**
     * Les KDoc de `AndroidAlarmScheduler` nomment précisément les API interdites pour expliquer
     * pourquoi elles le sont : les lire comme des appels ferait échouer le test sur sa propre
     * documentation. Seul le code compte ici.
     */
    private fun String.withoutComments(): String = replace(BLOCK_COMMENT, "").replace(LINE_COMMENT, "")

    /** Sources de production : `main` de chaque module Android, plus `commonMain` du module KMP. */
    private fun productionSources(): List<File> =
        (
            File(rootDir, "androidApp").walkTopDown().filter { it.isSourceIn("/src/main/") } +
                File(rootDir, "shared/core/src/commonMain").walkTopDown().filter { it.extension == "kt" }
        ).toList().also { assertThat(it).isNotEmpty() }

    private fun File.isSourceIn(sourceSet: String): Boolean =
        extension == "kt" && invariantSeparatorsPath.contains(sourceSet) && !invariantSeparatorsPath.contains("/build/")

    /**
     * Classes de production du dépôt présentes sur le classpath release : les entrées de
     * `java.class.path` situées sous la racine du dépôt, moins celles des source sets de test.
     * Les dépendances externes (cache Gradle, hors de [rootDir]) sont exclues : leurs classes ne
     * sont pas les nôtres et porteraient de faux positifs sur `Fake`.
     */
    private fun productionClasses(): List<String> =
        requireNotNull(System.getProperty("java.class.path"))
            .split(File.pathSeparator)
            .map(::File)
            .filter { it.exists() && it.startsWith(rootDir) && !it.isTestOutput() }
            .flatMap { entry ->
                if (entry.isDirectory) {
                    entry
                        .walkTopDown()
                        .filter { it.extension == "class" }
                        .map { it.relativeTo(entry).invariantSeparatorsPath }
                        .toList()
                } else {
                    ZipFile(entry).use { zip -> zip.entries().toList().map { it.name } }
                }
            }.filter { it.endsWith(".class") }
            .map { it.removeSuffix(".class").replace('/', '.') }

    /**
     * Les classes de ce test lui-même vivent sur son propre classpath : sans ce filtre, il
     * trouverait ses propres doublures et échouerait sur lui-même.
     */
    private fun File.isTestOutput(): Boolean =
        invariantSeparatorsPath.let { path ->
            path.contains("UnitTest") || path.contains("/test/") || path.contains("/androidTest/")
        }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val ALARM_MANAGER_IMPORT = "import android.app.AlarmManager"
        val USES_PERMISSION_TAGS = listOf("uses-permission", "uses-permission-sdk-23")

        /** `Poc` et `Fake` comme mots CamelCase entiers : ni `Epoch`, ni `Pocket`, ni `Faker`. */
        val FORBIDDEN_CLASS_NAME = Regex("""(?<![a-zA-Z])(Poc|Fake)(?![a-z])|Debug\w*Store""")

        /** `AlarmManager.set()` est inexacte : §9.1 l'interdit comme les autres. */
        val RAW_SET_CALL = Regex("""\.\s*set\s*\(""")

        val FORBIDDEN_PERMISSIONS =
            setOf(
                "android.permission.INTERNET",
                "android.permission.SCHEDULE_EXACT_ALARM",
                "android.permission.QUERY_ALL_PACKAGES",
            )
        val BLOCK_COMMENT = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
        val LINE_COMMENT = Regex("""//[^\n]*""")

        /** Ajoutée par androidx.core à tout APK, en `protectionLevel="signature"`. */
        const val GENERATED_RECEIVER_PERMISSION = "com.niumi.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"

        val SPEC_PERMISSIONS =
            setOf(
                "android.permission.NFC",
                "android.permission.USE_EXACT_ALARM",
                "android.permission.USE_FULL_SCREEN_INTENT",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.WAKE_LOCK",
                "android.permission.VIBRATE",
            )

        val ALARM_APIS =
            listOf(
                "setAlarmClock",
                "setExactAndAllowWhileIdle",
                "setExact",
                "setAndAllowWhileIdle",
                "setRepeating",
                "setInexactRepeating",
                "setWindow",
            )
    }
}
