plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.niumi.feature.session"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        // HiltTestRunner : NiumiBlockingAccessibilityService est @AndroidEntryPoint, les tests
        // instrumentés ont besoin d'une Application Hilt (HiltTestApplication). Même motif que
        // :feature:ringing.
        testInstrumentationRunner = "com.niumi.feature.session.HiltTestRunner"
    }

    buildFeatures {
        compose = true
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

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.hilt.android)
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
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.truth)
    kspAndroidTest(libs.hilt.compiler)
}

tasks.withType<Test>().configureEach {
    // Consommé par NiumiBlockingAccessibilityServiceSourceTest : garde-fou de SPEC_ANDROID
    // §12.2 lu directement dans le source du service, en JVM. Même mécanisme que
    // ModuleListTest (:app) et NiumiAlarmWavTest (:feature:ringing).
    systemProperty("niumi.rootDir", rootProject.rootDir.absolutePath)
}
