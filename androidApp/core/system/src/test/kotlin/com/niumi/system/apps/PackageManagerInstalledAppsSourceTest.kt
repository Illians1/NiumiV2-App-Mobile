package com.niumi.system.apps

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val NIUMI_PACKAGE = "com.niumi.app"

/**
 * Exclusions de SPEC_ANDROID §12.1, vérifiées une par une. Le fake [FakePackageQuery] remplace
 * `PackageManager` : la traduction Android vit dans `PackageManagerPackageQuery`, la logique
 * (dédoublonnage, exclusions, tri) est pure et testable ici.
 *
 * Les icônes sont `null` dans tous ces cas : `Drawable` est un type du framework, absent de la
 * JVM de test ; la nullabilité du champ suffit à couvrir la logique, qui ne regarde jamais
 * l'icône.
 */
class PackageManagerInstalledAppsSourceTest {
    private class FakePackageQuery(
        var entries: List<InstalledApp> = emptyList(),
        var home: Set<String> = emptySet(),
        var settings: String? = null,
        var dialers: Set<String> = emptySet(),
    ) : PackageQuery {
        override fun launcherEntries(): List<InstalledApp> = entries

        override fun homePackages(): Set<String> = home

        override fun settingsPackage(): String? = settings

        override fun defaultDialerPackages(): Set<String> = dialers
    }

    private fun app(
        packageName: String,
        label: String = packageName,
    ) = InstalledApp(packageName, label, icon = null)

    private fun source(query: PackageQuery) =
        PackageManagerInstalledAppsSource(query, NIUMI_PACKAGE, UnconfinedTestDispatcher())

    @Test
    fun theNiumiPackageIsNeverOfferedForBlocking() =
        runTest {
            val query = FakePackageQuery(entries = listOf(app(NIUMI_PACKAGE, "Niumi"), app("com.example.chat")))

            val apps = source(query).launchableApps()

            assertThat(apps.map { it.packageName }).containsExactly("com.example.chat")
        }

    @Test
    fun everyHomeRoleHolderIsExcludedNotOnlyTheDefaultOne() =
        runTest {
            val query =
                FakePackageQuery(
                    entries = listOf(app("com.android.launcher3"), app("com.other.launcher"), app("com.example.chat")),
                    home = setOf("com.android.launcher3", "com.other.launcher"),
                )

            val apps = source(query).launchableApps()

            assertThat(apps.map { it.packageName }).containsExactly("com.example.chat")
        }

    @Test
    fun theResolvedSettingsPackageIsExcluded() =
        runTest {
            val query =
                FakePackageQuery(
                    entries = listOf(app("com.android.settings"), app("com.example.chat")),
                    settings = "com.android.settings",
                )

            val apps = source(query).launchableApps()

            assertThat(apps.map { it.packageName }).containsExactly("com.example.chat")
        }

    @Test
    fun theSystemUserInterfaceIsExcluded() =
        runTest {
            val query = FakePackageQuery(entries = listOf(app("com.android.systemui"), app("com.example.chat")))

            val apps = source(query).launchableApps()

            assertThat(apps.map { it.packageName }).containsExactly("com.example.chat")
        }

    @Test
    fun theDialerAndEmergencyRelatedPackagesAreExcluded() =
        runTest {
            val query =
                FakePackageQuery(
                    entries = listOf(app("com.android.dialer"), app("com.oem.phone"), app("com.example.chat")),
                    dialers = setOf("com.android.dialer", "com.oem.phone"),
                )

            val apps = source(query).launchableApps()

            assertThat(apps.map { it.packageName }).containsExactly("com.example.chat")
        }

    @Test
    fun severalLauncherActivitiesOfTheSamePackageYieldASingleEntry() =
        runTest {
            val query =
                FakePackageQuery(
                    entries =
                        listOf(
                            app("com.example.suite", "Documents"),
                            app("com.example.suite", "Tableur"),
                            app("com.example.chat", "Chat"),
                        ),
                )

            val apps = source(query).launchableApps()

            assertThat(apps.map { it.packageName }).containsExactly("com.example.suite", "com.example.chat")
        }

    @Test
    fun appsAreSortedByLabelIgnoringCase() =
        runTest {
            val query =
                FakePackageQuery(
                    entries =
                        listOf(
                            app("com.example.z", "zèbre"),
                            app("com.example.a", "Agenda"),
                            app("com.example.b", "banque"),
                        ),
                )

            val apps = source(query).launchableApps()

            assertThat(apps.map { it.label }).containsExactly("Agenda", "banque", "zèbre").inOrder()
        }

    @Test
    fun anEmptyDeviceYieldsAnEmptyListRatherThanThrowing() =
        runTest {
            assertThat(source(FakePackageQuery()).launchableApps()).isEmpty()
        }
}
