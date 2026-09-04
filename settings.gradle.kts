pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "niumi-mobile"

include(
    ":app",
    ":shared:core",
    ":core:database",
    ":core:system",
    ":core:designsystem",
    ":feature:setup",
    ":feature:session",
    ":feature:ringing",
)

project(":app").projectDir = file("androidApp/app")
project(":core").projectDir = file("androidApp/core")
project(":core:database").projectDir = file("androidApp/core/database")
project(":core:system").projectDir = file("androidApp/core/system")
project(":core:designsystem").projectDir = file("androidApp/core/designsystem")
project(":feature").projectDir = file("androidApp/feature")
project(":feature:setup").projectDir = file("androidApp/feature/setup")
project(":feature:session").projectDir = file("androidApp/feature/session")
project(":feature:ringing").projectDir = file("androidApp/feature/ringing")
