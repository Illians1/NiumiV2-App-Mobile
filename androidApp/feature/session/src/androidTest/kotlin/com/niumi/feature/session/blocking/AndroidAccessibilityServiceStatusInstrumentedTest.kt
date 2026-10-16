package com.niumi.feature.session.blocking

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.system.blocking.AccessibilityServiceBinding
import com.niumi.system.blocking.AccessibilityServiceState
import com.niumi.system.blocking.AndroidAccessibilityServiceStatus
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lecture réelle de `Settings.Secure` par [AndroidAccessibilityServiceStatus] (SPEC_ANDROID §13),
 * sur un vrai appareil, croisée avec une liaison simulée : l'APK de test de ce module
 * (`com.niumi.feature.session.test`) ne lie jamais le service, la vraie liaison ne peut donc pas
 * être observée ici. C'est la seule partie de la chaîne de blocage que ce module peut vérifier
 * honnêtement.
 *
 * Le parcours complet (application bloquée ramenée à l'accueil + overlay, SPEC_ANDROID §19.2)
 * **ne peut pas** être testé ici : l'APK de test d'un module `library` est une application
 * distincte, alors que le service activé appartient à `com.niumi.app` et vit dans son process.
 * Ce test de bout en bout doit vivre dans `androidTest` de `:app` ; voir `ETAPE-05.md`.
 */
@RunWith(AndroidJUnit4::class)
class AndroidAccessibilityServiceStatusInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private class FakeBinding(
        override val isBound: Boolean,
    ) : AccessibilityServiceBinding

    private fun status(
        component: String,
        bound: Boolean,
    ) = AndroidAccessibilityServiceStatus(
        contentResolver = context.contentResolver,
        expectedComponent = component,
        binding = FakeBinding(bound),
    )

    /** Le premier service réellement activé sur l'appareil, quel qu'il soit. */
    private fun firstEnabledComponent(): String {
        val raw =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
        assumeTrue("Aucun service d'accessibilité activé sur cet appareil", !raw.isNullOrEmpty())
        return raw.split(":").first()
    }

    /** La liaison ne compte pas sans l'inscription : c'est le choix de l'utilisateur qui prime. */
    @Test
    fun unknownComponentIsDisabledEvenWhenBound() {
        val status = status("com.exemple.inexistant/com.exemple.inexistant.Service", bound = true)

        assertThat(status.read()).isEqualTo(AccessibilityServiceState.DISABLED)
        assertThat(status.isEnabled()).isFalse()
    }

    /**
     * Le cas du 2026-10-15 : inscrit, mais aucune instance reliée dans ce processus — quel que soit
     * l'interrupteur global, tenu à 1 par n'importe quel autre service.
     */
    @Test
    fun aListedComponentNotBoundInThisProcessIsPending() {
        val status = status(firstEnabledComponent(), bound = false)

        assertThat(status.read()).isEqualTo(AccessibilityServiceState.PENDING)
        assertThat(status.isEnabled()).isFalse()
    }

    @Test
    fun aListedComponentBoundInThisProcessIsEnabled() {
        val status = status(firstEnabledComponent(), bound = true)

        assertThat(status.read()).isEqualTo(AccessibilityServiceState.ENABLED)
        assertThat(status.isEnabled()).isTrue()
    }
}
