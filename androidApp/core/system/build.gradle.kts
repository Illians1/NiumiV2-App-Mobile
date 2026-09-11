plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.niumi.system"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":core:database"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)

    // SetupPreferences (étape 12) : les deux accusés de réception de la mise en route vivent
    // hors Room, pour rester lisibles sans déverrouillage et sans migration de schéma.
    implementation(libs.datastore.preferences)

    // AppSelectionStore (étape 13) : la sélection courante persiste packages + libellés figés
    // (§12.2) dans DataStore, qui ne sait stocker qu'un `Set<String>` nativement — sérialisée en
    // JSON. Bibliothèque déjà au catalogue et déjà utilisée par `:core:database` ; pas le plugin,
    // un `MapSerializer` suffit.
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
