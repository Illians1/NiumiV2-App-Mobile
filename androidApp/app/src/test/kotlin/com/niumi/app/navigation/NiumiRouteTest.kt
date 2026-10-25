package com.niumi.app.navigation

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import org.junit.Test

/**
 * `NiumiRoute.AppSelection` est la seule destination à argument du graphe depuis l'étape 45 : l'écran 4
 * rouvert depuis l'accueil (M11). `navigation-compose` encode et décode les routes typées à travers
 * leur sérialiseur généré : le descripteur ci-dessous est exactement ce qu'il consomme.
 */
@OptIn(ExperimentalSerializationApi::class)
class NiumiRouteTest {
    @Test
    fun theAppSelectionRouteDeclaresItsOptionalEditingArgument() {
        val descriptor = serializer<NiumiRoute.AppSelection>().descriptor

        assertThat(descriptor.elementsCount).isEqualTo(1)
        assertThat(descriptor.getElementName(0)).isEqualTo("editing")
        // La mise en route l'ouvre sans argument.
        assertThat(descriptor.isElementOptional(0)).isTrue()
    }

    @Test
    fun theOtherDestinationsCarryNoArgument() {
        assertThat(serializer<NiumiRoute.Home>().descriptor.elementsCount).isEqualTo(0)
        assertThat(serializer<NiumiRoute.TransitionalSettings>().descriptor.elementsCount).isEqualTo(0)
    }

    @Test
    fun theTwoModesOfScreenFourAreDistinctDestinations() {
        // `launchSingleTop` et `popUpTo` comparent les destinations par égalité de valeur.
        assertThat(NiumiRoute.AppSelection()).isEqualTo(NiumiRoute.AppSelection(editing = false))
        assertThat(NiumiRoute.AppSelection(editing = true)).isNotEqualTo(NiumiRoute.AppSelection())
    }
}
