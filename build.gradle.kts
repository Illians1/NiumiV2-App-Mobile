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

    // Bruit attendu, à ne pas partir chercher : `:core:database` fait afficher à detekt
    // « There were 2 compiler errors found during analysis » à chaque exécution. Ce sont les deux
    // appels à `DirectBootSnapshot.Active.serializer()` de `FileDirectBootStore`, signalés comme
    // `unresolved reference 'serializer'`. Cette fonction n'existe pas dans le source : le plugin du
    // compilateur `kotlinx-serialization` la génère sur toute classe `@Serializable`. La passe
    // d'analyse de detekt s'exécute avec le classpath du module mais **sans** les plugins du
    // compilateur Kotlin, donc sans cette génération. Le vrai compilateur, lui, ne bronche pas :
    // `compileDebugKotlin`, les tests et `assembleDebug` sont verts. Frottement connu de detekt avec
    // les plugins du compilateur, le même que celui rencontré couramment avec Compose.
    //
    // Conséquence réelle, circonscrite : la résolution de types est perdue sur ce seul fichier, donc
    // les règles qui en dépendent peuvent y rater quelque chose. Les règles syntaxiques continuent de
    // s'y appliquer. Désactiver la type resolution sur tout le module ferait taire le message au prix
    // d'un affaiblissement bien plus large — mauvais échange, écarté le 2026-09-16 (étape 23).
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
