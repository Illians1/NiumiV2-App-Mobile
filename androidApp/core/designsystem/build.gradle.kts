plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.niumi.designsystem"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
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
    debugImplementation(libs.compose.ui.tooling)
}
