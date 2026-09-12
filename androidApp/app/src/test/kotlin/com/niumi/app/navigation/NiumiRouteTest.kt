package com.niumi.app.navigation

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.serializer
import org.junit.Test

/**
 * `NiumiRoute.Summary` est la seule destination à argument du graphe : c'est le seul endroit où un
 * argument de navigation peut se perdre. `navigation-compose` encode et décode les routes typées à
 * travers leur sérialiseur généré — le descripteur ci-dessous est exactement ce qu'il consomme
 * pour construire la route et en relire l'argument (`toRoute<NiumiRoute.Summary>()`).
 *
 * Le test porte sur le descripteur et non sur un aller-retour JSON : `navigation-compose` n'utilise
 * pas le format JSON, et l'ajouter au seul bénéfice du test ferait entrer une dépendance de plus
 * dans `:app` sans rien prouver de la navigation réelle.
 */
class NiumiRouteTest {
    @Test
    fun theSummaryRouteDeclaresItsLocalTimeArgument() {
        val descriptor = serializer<NiumiRoute.Summary>().descriptor

        assertThat(descriptor.elementsCount).isEqualTo(1)
        assertThat(descriptor.getElementName(0)).isEqualTo("localTimeIso")
        assertThat(descriptor.isElementOptional(0)).isFalse()
    }

    @Test
    fun theOtherDestinationsCarryNoArgument() {
        assertThat(serializer<NiumiRoute.WakeTime>().descriptor.elementsCount).isEqualTo(0)
        assertThat(serializer<NiumiRoute.ActiveSession>().descriptor.elementsCount).isEqualTo(0)
        assertThat(serializer<NiumiRoute.Home>().descriptor.elementsCount).isEqualTo(0)
    }

    @Test
    fun twoSummaryRoutesWithTheSameTimeAreEqual() {
        // `launchSingleTop` et `popUpTo` comparent les destinations par égalité de valeur.
        assertThat(NiumiRoute.Summary("07:00")).isEqualTo(NiumiRoute.Summary("07:00"))
        assertThat(NiumiRoute.Summary("07:00")).isNotEqualTo(NiumiRoute.Summary("06:30"))
    }
}
