package com.niumi.system.blocking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val COMPONENT = "com.niumi.app/com.niumi.feature.session.blocking.NiumiBlockingAccessibilityService"

/**
 * Parsing de `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` (SPEC_ANDROID §13, §19.1).
 * Android accepte deux formes pour la partie classe : un nom qualifié complet et un nom
 * relatif préfixé par un point.
 */
class EnabledAccessibilityServicesParserTest {
    @Test
    fun emptyStringHasNoEnabledService() {
        val result = EnabledAccessibilityServicesParser.isEnabled("", COMPONENT)

        assertThat(result).isFalse()
    }

    @Test
    fun nullStringHasNoEnabledService() {
        val result = EnabledAccessibilityServicesParser.isEnabled(null, COMPONENT)

        assertThat(result).isFalse()
    }

    @Test
    fun onlyAnotherServiceIsNotEnabled() {
        val result = EnabledAccessibilityServicesParser.isEnabled("com.exemple/com.exemple.AutreService", COMPONENT)

        assertThat(result).isFalse()
    }

    @Test
    fun fullyQualifiedComponentIsEnabled() {
        val result = EnabledAccessibilityServicesParser.isEnabled(COMPONENT, COMPONENT)

        assertThat(result).isTrue()
    }

    @Test
    fun relativeComponentFormIsEnabled() {
        // La forme relative n'a de sens que lorsque le nom de classe partage le préfixe du
        // package du composant — ce qui n'est pas le cas de NiumiBlockingAccessibilityService
        // (vit dans com.niumi.feature.session.blocking, hors de com.niumi.app) : Android ne
        // l'écrirait jamais sous cette forme pour ce service précis. Ce cas couvre le
        // comportement général du parseur avec un composant où la forme relative s'applique.
        val expected = "com.niumi.app/com.niumi.app.MainActivityAccessibilityService"

        val result =
            EnabledAccessibilityServicesParser.isEnabled(
                "com.niumi.app/.MainActivityAccessibilityService",
                expected,
            )

        assertThat(result).isTrue()
    }

    @Test
    fun severalServicesSeparatedByColonAreAllConsidered() {
        val raw = "com.exemple/com.exemple.AutreService:$COMPONENT"

        val result = EnabledAccessibilityServicesParser.isEnabled(raw, COMPONENT)

        assertThat(result).isTrue()
    }
}
