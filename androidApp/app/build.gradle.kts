plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.niumi.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.niumi.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
}

tasks.withType<Test>().configureEach {
    // Consommé par ModuleListTest : évite de dépendre du répertoire de travail des tests.
    systemProperty("niumi.rootDir", rootProject.rootDir.absolutePath)
}
