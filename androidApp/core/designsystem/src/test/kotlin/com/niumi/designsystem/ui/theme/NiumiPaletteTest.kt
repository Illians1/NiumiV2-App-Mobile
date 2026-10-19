package com.niumi.designsystem.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.lang.reflect.Modifier
import kotlin.reflect.KProperty1

private typealias Role = KProperty1<NiumiPalette, Color>

/**
 * Chaque couleur des deux palettes vient de `valeurs-de-design.json` (VALEURS-DE-DESIGN.md §2),
 * ou d'une décision écrite quand le design ne donne pas de valeur pour ce rôle dans ce thème.
 */
class NiumiPaletteTest {
    private val light = DesignValues.obj("theme", "clair")
    private val dark = DesignValues.obj("theme", "sombre")

    /** Clé JSON du thème clair → rôles qui en prennent la valeur. */
    private val lightRoles: Map<String, List<Role>> =
        mapOf(
            "fond" to listOf(NiumiPalette::background),
            "surface" to listOf(NiumiPalette::surface),
            "encre" to listOf(NiumiPalette::ink),
            // D9 (tranché le 2026-10-19) : texte du bouton inactif = texte secondaire.
            "texteSecondaire" to listOf(NiumiPalette::textSecondary, NiumiPalette::disabledText),
            "bordure" to listOf(NiumiPalette::border),
            "separateur" to listOf(NiumiPalette::separator),
            // VALEURS §2 et §4 : piste du cadran, « Annuler » de la feuille de scan, bouton inactif (D9).
            "piste" to listOf(NiumiPalette::track, NiumiPalette::cancelButton, NiumiPalette::disabledFill),
            "desactive" to listOf(NiumiPalette::disabled),
            "tuileGrise" to listOf(NiumiPalette::tileGrey),
            "caseGrisee" to listOf(NiumiPalette::cellGrey),
            "ambre" to listOf(NiumiPalette::amber),
            // Le mot signalé de la recopie n'est dessiné qu'en sombre : en clair, la teinte ambre.
            "teinteAmbre" to listOf(NiumiPalette::amberTint, NiumiPalette::mismatchTint),
            "texteSecondaireSurEncre" to listOf(NiumiPalette::onInkSecondary),
            "voile" to listOf(NiumiPalette::veil),
            "texteSurBoutonPrincipal" to listOf(NiumiPalette::onInk),
        )

    /** Clé JSON du thème sombre → rôles qui en prennent la valeur. */
    private val darkRoles: Map<String, List<Role>> =
        mapOf(
            "fond" to listOf(NiumiPalette::background),
            "surface" to listOf(NiumiPalette::surface),
            "texte" to listOf(NiumiPalette::ink),
            "texteSecondaire" to listOf(NiumiPalette::textSecondary),
            "bordure" to listOf(NiumiPalette::border),
            "bordCarte" to listOf(NiumiPalette::border),
            "texteDesactive" to listOf(NiumiPalette::disabledText),
            "separateur" to listOf(NiumiPalette::separator),
            // VALEURS §2 sombre : piste du cadran, fond du bouton inactif, rond de l'icône de scan.
            "piste" to
                listOf(
                    NiumiPalette::track,
                    NiumiPalette::disabledFill,
                    NiumiPalette::tileGrey,
                    NiumiPalette::cellGrey,
                ),
            "fondDesactive" to listOf(NiumiPalette::disabled),
            "bordMenu" to listOf(NiumiPalette::menuBorder),
            "boutonGris" to listOf(NiumiPalette::cancelButton),
            "ambre" to listOf(NiumiPalette::amber),
            "teinteMotSignale" to listOf(NiumiPalette::mismatchTint, NiumiPalette::amberTint),
            "texteSurBoutonPrincipal" to listOf(NiumiPalette::onInk),
            "voile" to listOf(NiumiPalette::veil),
            "lisereFeuille" to listOf(NiumiPalette::sheetOutline),
        )

    /**
     * Clés du JSON volontairement sans rôle : `texteSurCarteNoire` (`#FAFAF8`) ne se distingue pas
     * du blanc du bouton principal (VALEURS §2 : « l'écart ne se voit pas à l'œil ») ; la carte
     * « Réveil activé » utilise `onInk`. `degradeSoleil` est vérifié par son propre test.
     */
    private val lightKeysWithoutRole = setOf("texteSurCarteNoire", "degradeSoleil")

    @Test
    fun everyLightRoleTakenFromTheDesignValuesMatchesTheJson() {
        lightRoles.forEach { (key, roles) ->
            roles.forEach { role ->
                assertColorEquals(
                    "clair.${role.name} ← $key",
                    role.get(NiumiPalettes.Light),
                    DesignValues.string(light, key),
                )
            }
        }
    }

    @Test
    fun everyDarkRoleTakenFromTheDesignValuesMatchesTheJson() {
        darkRoles.forEach { (key, roles) ->
            roles.forEach { role ->
                assertColorEquals(
                    "sombre.${role.name} ← $key",
                    role.get(NiumiPalettes.Dark),
                    DesignValues.string(dark, key),
                )
            }
        }
    }

    /** Rôles sans valeur dans le JSON pour ce thème : décisions de l'étape 33, écrites ici. */
    @Test
    fun rolesWithoutADesignValueFollowTheStep33Rulings() {
        // En clair, les feuilles n'ont pas de liseré et le menu n'a pas de bord (seule l'ombre).
        assertThat(NiumiPalettes.Light.sheetOutline).isEqualTo(Color.Transparent)
        assertThat(NiumiPalettes.Light.menuBorder).isEqualTo(Color.Transparent)
        // En sombre, le texte secondaire posé sur l'encre claire (bouton ou carte blanc cassé).
        assertColorEquals("sombre.onInkSecondary", NiumiPalettes.Dark.onInkSecondary, "#5C5C60")
    }

    @Test
    fun everyRoleOfBothPalettesIsAccountedFor() {
        // Champs d'instance seulement : le compilateur Compose ajoute un champ statique `$stable`.
        val roles =
            NiumiPalette::class.java.declaredFields
                .filterNot { Modifier.isStatic(it.modifiers) }
                .map { it.name }
                .toSet()
        val explicit = setOf("sheetOutline", "menuBorder")
        val lightCovered =
            lightRoles.values
                .flatten()
                .map { it.name }
                .toSet() + explicit
        val darkCovered =
            darkRoles.values
                .flatten()
                .map { it.name }
                .toSet() + "onInkSecondary"

        assertThat(roles).hasSize(21)
        assertWithMessage("rôles clairs sans origine").that(roles - lightCovered).isEmpty()
        assertWithMessage("rôles sombres sans origine").that(roles - darkCovered).isEmpty()
    }

    /** Une valeur ajoutée au design sans jeton ferait échouer ce test au lieu d'être oubliée. */
    @Test
    fun everyColorOfTheDesignValuesHasAToken() {
        assertThat(light.keys - lightKeysWithoutRole).containsExactlyElementsIn(lightRoles.keys)
        assertThat(dark.keys).containsExactlyElementsIn(darkRoles.keys)
    }

    /** VALEURS §2 : le seul dégradé des écrans clairs, soleil de l'écran 11. */
    @Test
    fun theSunGradientHasTheThreeStopsOfTheDesign() {
        val stops = light.getValue("degradeSoleil").jsonArray.map { it.jsonObject }

        assertThat(NiumiPalettes.SunGradient).hasSize(stops.size)
        stops.zip(NiumiPalettes.SunGradient).forEach { (stop, actual) ->
            assertThat(actual.first).isEqualTo(
                stop
                    .getValue("position")
                    .jsonPrimitive.content
                    .toFloat(),
            )
            assertColorEquals("soleil ${actual.first}", actual.second, DesignValues.string(stop, "couleur"))
        }
    }

    /** Le fond de fenêtre affiché avant Compose (`colors.xml`) est celui de la palette. */
    @Test
    fun theWindowBackgroundsOfTheXmlThemesAreThePaletteBackgrounds() {
        val xml = DesignValues.moduleFile("src/main/res/values/colors.xml").readText()

        fun xmlColor(name: String): Int =
            Regex("""<color name="$name">#([0-9A-Fa-f]{8})</color>""")
                .find(xml)
                ?.groupValues
                ?.get(1)
                ?.toLong(16)
                ?.toInt()
                ?: error("couleur $name absente de colors.xml")

        assertThat(xmlColor("niumi_background_light")).isEqualTo(NiumiPalettes.Light.background.toArgb())
        assertThat(xmlColor("niumi_background_dark")).isEqualTo(NiumiPalettes.Dark.background.toArgb())
    }
}
