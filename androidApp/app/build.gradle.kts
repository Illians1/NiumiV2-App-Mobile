import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.HostTestBuilder
import org.gradle.process.CommandLineArgumentProvider
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Routes typées @Serializable de `navigation/NiumiRoute.kt` (navigation-compose 2.8+).
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

/**
 * Signature de publication. `keystore.properties`, à la racine du dépôt et **jamais versionné**
 * (`.gitignore`), porte `storeFile`, `storePassword`, `keyAlias` et `keyPassword` ; le keystore
 * lui-même vit hors du dépôt. Quand le fichier est absent — CI, autre poste — la variante release
 * reste simplement non signée au lieu de faire échouer la configuration : un build de
 * vérification n'a pas besoin de la clé d'upload, et l'exiger rendrait la CI impossible.
 *
 * `providers.fileContents` plutôt qu'une lecture directe : le fichier devient une entrée de
 * configuration, donc le configuration cache est invalidé s'il apparaît, change ou disparaît.
 */
val keystoreProperties: Provider<Properties> =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
        .asText
        .map { text -> Properties().apply { load(text.reader()) } }

fun Properties.required(key: String): String =
    requireNotNull(getProperty(key)) { "keystore.properties : la clé « $key » manque." }

android {
    namespace = "com.niumi.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.niumi.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "com.niumi.app.HiltTestRunner"
    }

    // Doit précéder `buildTypes`, qui y cherche la configuration par son nom.
    signingConfigs {
        if (keystoreProperties.isPresent) {
            val properties = keystoreProperties.get()
            create("release") {
                storeFile = rootProject.file(properties.required("storeFile"))
                storePassword = properties.required("storePassword")
                keyAlias = properties.required("keyAlias")
                keyPassword = properties.required("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // `null` sans `keystore.properties` : l'APK et l'AAB sortent non signés, sans erreur.
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        disable +=
            setOf(
                // targetSdk 36 est imposé mot pour mot par SPEC_ANDROID §5, indépendamment
                // du compileSdk 37 utilisé pour compiler. L'écart est volontaire, pas un oubli.
                "OldTargetApi",
                // Gradle 9.5.0 / AGP 9.1.1 sont la combinaison la plus haute couverte par la
                // matrice de compatibilité officielle de Kotlin 2.4.10 (voir le rapport
                // d'étape) ; suivre l'avis "version plus récente disponible" en sortirait.
                "AndroidGradlePluginVersion",
                // Lint suggère de fusionner mipmap-anydpi-v26 dans mipmap-anydpi puisque
                // minSdk (29) > 26. Fait, mais le merger de ressources d'AGP 9.1.1 supprime
                // silencieusement le dossier "mipmap-anydpi" sans qualificatif de version
                // (AAPT2 le compile seul sans erreur ; l'APK final se retrouve sans icône).
                // On garde donc le qualificatif -v26, redondant mais fonctionnel, jusqu'à
                // correction de ce comportement en amont.
                "ObsoleteSdkInt",
            )
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":core:database"))
    implementation(project(":core:system"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:setup"))
    implementation(project(":feature:session"))
    implementation(project(":feature:ringing"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    // `LocalLifecycleOwner` (androidx.lifecycle.compose) : l'accueil relit l'accusé de réception
    // de l'onboarding sur ON_RESUME.
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    // Étape 17 : `AlarmChainInstrumentedTest` est le seul test de la chaîne de réveil complète, et
    // `:app` est le seul module dont le graphe Dagger l'est aussi (les liaisons de blocage vivent
    // dans `:feature:session`, absent de l'APK de test de `:feature:ringing`).
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}

tasks.withType<Test>().configureEach {
    // Consommé par ModuleListTest et ReleaseHygieneTest : évite de dépendre du répertoire de
    // travail des tests.
    systemProperty("niumi.rootDir", rootProject.rootDir.absolutePath)
}

/**
 * Passe un fichier au test sous forme de propriété système, en le déclarant comme entrée de
 * tâche. Une classe nommée plutôt qu'une lambda : le configuration cache sérialise le
 * `Provider`, pas le `Project` qui l'a créé.
 */
private class SystemPropertyFileArgument(
    private val name: String,
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE)
    val file: Provider<RegularFile>,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("-D$name=${file.get().asFile.absolutePath}")
}

// `ReleaseHygieneTest` (variante release uniquement) contrôle le manifeste **fusionné**, celui
// qui part dans l'AAB : c'est là qu'apparaîtrait une permission ajoutée par une dépendance, que
// le manifeste de `:app` seul ne montrerait jamais. Brancher l'artefact ainsi fait tourner
// `processReleaseManifest` avant le test, sans coder son chemin en dur.
androidComponents {
    // AGP 9 ne crée les tâches de test unitaire que pour `testBuildType` (« debug ») : sans
    // cette ligne, `testReleaseUnitTest` n'existe pas et le garde-fou ne pourrait tourner que
    // sur un classpath qui contient `src/debug`, où il ne prouverait rien.
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        variantBuilder.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }

    onVariants(selector().withBuildType("release")) { variant ->
        val mergedManifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
        tasks.withType<Test>().matching { it.name == "testReleaseUnitTest" }.configureEach {
            jvmArgumentProviders.add(SystemPropertyFileArgument("niumi.mergedManifest", mergedManifest))
        }
    }
}
