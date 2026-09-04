package com.niumi.app

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Garde-fou de "Contraintes globales" du plan MVP : exactement huit modules Gradle, jamais un
 * de plus. `:core:designsystem` a été ajouté à l'étape 3 : les modules `feature` ne peuvent
 * pas dépendre de `:app` (règle de dépendance SPEC_ANDROID §6), mais ont besoin du thème
 * Niumi pour leurs propres écrans (ex. `AlarmActivity` dans `:feature:ringing`) ; un module de
 * design system dédié résout ce besoin sans dépendance inverse. `niumi.rootDir` est injecté
 * par `tasks.withType<Test>` dans `build.gradle.kts` pour ne pas dépendre du répertoire de
 * travail dans lequel Gradle exécute les tests.
 */
class ModuleListTest {
    private val expectedModules =
        setOf(
            ":app",
            ":shared:core",
            ":core:database",
            ":core:system",
            ":core:designsystem",
            ":feature:setup",
            ":feature:session",
            ":feature:ringing",
        )

    @Test
    fun settingsDeclaresExactlyTheEightExpectedModules() {
        val rootDir =
            File(
                requireNotNull(System.getProperty("niumi.rootDir")) {
                    "La propriété système niumi.rootDir n'a pas été injectée par le build Gradle."
                },
            )
        val settingsFile = File(rootDir, "settings.gradle.kts")
        assertThat(settingsFile.exists()).isTrue()

        val includeBlock =
            Regex("""include\((.*?)\)""", RegexOption.DOT_MATCHES_ALL)
                .find(settingsFile.readText())
                ?.groupValues
                ?.get(1)
        requireNotNull(includeBlock) { "Aucun appel include(...) trouvé dans settings.gradle.kts." }

        val includedModules =
            Regex(""""(:[a-zA-Z:]+)"""")
                .findAll(includeBlock)
                .map { it.groupValues[1] }
                .toSet()

        assertThat(includedModules).isEqualTo(expectedModules)
    }
}
