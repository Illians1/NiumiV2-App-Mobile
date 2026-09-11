package com.niumi.system.apps

import com.google.common.truth.Truth.assertThat
import com.niumi.database.BlockedPackage
import org.junit.Test

/**
 * La sélection courante persiste le libellé figé à côté du package (SPEC_ANDROID §12.2) : une
 * application désinstallée ou masquée n'a plus de libellé résoluble par `PackageManager`, et le
 * texte d'overlay imposé afficherait alors un nom de package. DataStore ne sachant stocker qu'un
 * `Set<String>`, le couple est sérialisé en JSON.
 */
class AppSelectionCodecTest {
    @Test
    fun roundTripPreservesPackagesLabelsAndOrder() {
        val selection =
            listOf(
                BlockedPackage("com.example.zeta", "Zeta"),
                BlockedPackage("com.example.alpha", "Alpha"),
                BlockedPackage("com.example.beta", "Beta"),
            )

        val decoded = AppSelectionCodec.decode(AppSelectionCodec.encode(selection))

        assertThat(decoded).containsExactlyElementsIn(selection).inOrder()
    }

    @Test
    fun labelsWithQuotesAccentsAndEmojiSurviveTheRoundTrip() {
        val selection =
            listOf(
                BlockedPackage("com.example.quotes", """Mon "app" préférée"""),
                BlockedPackage("com.example.emoji", "Réveil 🌙 & café"),
                BlockedPackage("com.example.backslash", """Chemin\vers\app"""),
            )

        val decoded = AppSelectionCodec.decode(AppSelectionCodec.encode(selection))

        assertThat(decoded).containsExactlyElementsIn(selection).inOrder()
    }

    @Test
    fun anEmptySelectionRoundTripsToAnEmptyList() {
        assertThat(AppSelectionCodec.decode(AppSelectionCodec.encode(emptyList()))).isEmpty()
    }

    @Test
    fun corruptedOrAbsentContentDecodesToAnEmptySelectionRatherThanThrowing() {
        assertThat(AppSelectionCodec.decode(null)).isEmpty()
        assertThat(AppSelectionCodec.decode("")).isEmpty()
        assertThat(AppSelectionCodec.decode("{ceci n'est pas du JSON")).isEmpty()
        assertThat(AppSelectionCodec.decode("""["une","liste","au lieu d'un objet"]""")).isEmpty()
    }
}
