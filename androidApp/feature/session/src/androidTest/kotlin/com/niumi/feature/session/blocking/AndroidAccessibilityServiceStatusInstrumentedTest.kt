package com.niumi.feature.session.blocking

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.system.blocking.AndroidAccessibilityServiceStatus
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lecture réelle de `Settings.Secure` par [AndroidAccessibilityServiceStatus] (SPEC_ANDROID §13),
 * sur un vrai appareil : c'est la seule partie de la chaîne de blocage que l'APK de test de ce
 * module peut vérifier honnêtement.
 *
 * Le parcours complet (application bloquée ramenée à l'accueil + overlay, SPEC_ANDROID §19.2)
 * **ne peut pas** être testé ici : l'APK de test d'un module `library` est une application
 * distincte (`com.niumi.feature.session.test`), alors que le service activé appartient à
 * `com.niumi.app` et vit dans son process. Les deux ont donc chacun leur
 * `InMemoryBlockedPackagesProjection` et le test ne peut pas piloter le service réel. Ce test
 * de bout en bout doit vivre dans `androidTest` de `:app` ; voir `ETAPE-05.md`.
 */
@RunWith(AndroidJUnit4::class)
class AndroidAccessibilityServiceStatusInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun unknownComponentIsNeverReportedAsEnabled() {
        val status =
            AndroidAccessibilityServiceStatus(
                contentResolver = context.contentResolver,
                expectedComponent = "com.exemple.inexistant/com.exemple.inexistant.Service",
            )

        assertThat(status.isEnabled()).isFalse()
    }

    @Test
    fun aComponentActuallyPresentInSettingsIsReportedAsEnabled() {
        val raw =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
        assumeTrue("Aucun service d'accessibilité activé sur cet appareil", !raw.isNullOrEmpty())

        val firstEnabledComponent = raw.split(":").first()
        val status =
            AndroidAccessibilityServiceStatus(
                contentResolver = context.contentResolver,
                expectedComponent = firstEnabledComponent,
            )

        assertThat(status.isEnabled()).isTrue()
    }
}
