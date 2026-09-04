import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    // `libs` (LibrariesForLibs) n'est disponible que dans le script qui l'a généré.
    // Depuis un `subprojects { }` du build root, on relit le catalogue via l'API générique.
    val versionCatalog = rootProject.extensions.getByType(VersionCatalogsExtension::class.java)
        .named("libs")

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "dev.detekt")

    extensions.configure<KtlintExtension> {
        version.set(versionCatalog.findVersion("ktlintCli").get().requiredVersion)
        android.set(true)
    }

    extensions.configure<DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig.set(true)
        parallel.set(true)
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget.set("17")
        reports {
            sarif.required.set(true)
            html.required.set(true)
        }
    }

    tasks.withType<DetektCreateBaselineTask>().configureEach {
        jvmTarget.set("17")
    }

    // Sur un module Kotlin Multiplatform, `dev.detekt` crée une tâche par source set
    // (`detektCommonMainSourceSet`, `detektJvmTestSourceSet`…) mais la tâche agrégée `detekt`
    // reste vide (NO-SOURCE) : elle n'analyse ni `commonMain` ni aucun autre source set KMP,
    // silencieusement. Sur un module Android classique, `detekt` est la seule tâche `Detekt` et
    // ce câblage est un no-op. Sans ce correctif, `./gradlew detekt` — la commande de
    // vérification imposée par le plan MVP — ne couvrirait jamais `:shared:core`.
    tasks.matching { it.name == "detekt" }.configureEach {
        dependsOn(tasks.withType<Detekt>().matching { detektTask -> detektTask.name != "detekt" })
    }
}
