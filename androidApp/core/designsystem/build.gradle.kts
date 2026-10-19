plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.niumi.designsystem"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Module de thème et de composants visuels partagés (SPEC_ANDROID §6) : aucune
    // dépendance vers :shared:core, :core:database ou Hilt. Les modules feature ne peuvent
    // pas dépendre de :app (règle de dépendance §6) mais ont besoin de NiumiTheme pour leurs
    // propres écrans (ex. AlarmActivity dans :feature:ringing).
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    // `enableEdgeToEdge(SystemBarStyle)` : les barres système suivent le thème de la session,
    // jamais le réglage du téléphone (étape 33, `NiumiSystemBars.kt`).
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Tests JVM des jetons (étape 33) : comparés à `design-plan/valeurs-de-design.json`, lu dans le
    // dépôt — une seule source, jamais une copie en ressource de test.
    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.serialization.json)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.truth)
}

// Même motif que `:app` et `:core:database` : les tests lisent des fichiers du dépôt
// (`design-plan/valeurs-de-design.json`, polices embarquées) par chemin absolu.
tasks.withType<Test>().configureEach {
    systemProperty("niumi.rootDir", rootProject.rootDir.absolutePath)
}
