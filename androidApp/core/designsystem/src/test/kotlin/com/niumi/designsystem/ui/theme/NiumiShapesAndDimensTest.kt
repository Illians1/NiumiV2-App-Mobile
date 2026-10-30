package com.niumi.designsystem.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * Formes et dimensions des maquettes (VALEURS-DE-DESIGN.md §4, `valeurs-de-design.json` →
 * `formes`). Unité des maquettes : le point iPhone, soit le dp Android.
 */
class NiumiShapesAndDimensTest {
    private val forms = DesignValues.obj("formes")

    private fun number(
        key: String,
        field: String,
    ): Float =
        forms
            .getValue(key)
            .jsonObject
            .getValue(field)
            .jsonPrimitive.content
            .toFloat()

    private fun number(key: String): Float =
        forms
            .getValue(key)
            .jsonPrimitive.content
            .toFloat()

    /** Rayon d'un coin, mesuré sur un carré de 100 × 100 à densité 1 (un dp vaut un pixel). */
    private fun RoundedCornerShape.radii(): List<Float> {
        val size = Size(SQUARE, SQUARE)
        val density = Density(1f)
        return listOf(topStart, topEnd, bottomEnd, bottomStart).map { it.toPx(size, density) }
    }

    private fun assertDp(
        label: String,
        actual: Dp,
        expected: Float,
    ) = assertWithMessage(label).that(actual.value).isEqualTo(expected)

    private fun assertUniformRadius(
        label: String,
        shape: RoundedCornerShape,
        expected: Float,
    ) = assertWithMessage(label).that(shape.radii()).containsExactly(expected, expected, expected, expected)

    @Test
    fun screenMarginsAndTouchTarget() {
        assertDp("marge des côtés", NiumiDimens.screenHorizontal, number("marges", "cotes"))
        assertDp("marge du bas", NiumiDimens.screenBottom, number("marges", "bas"))
        assertDp("marge du haut", NiumiDimens.screenTop, number("marges", "haut"))
        assertDp("zone de toucher", NiumiDimens.minTouch, number("zoneDeToucherMinimale"))
    }

    @Test
    fun buttonsHaveTheHeightsAndCornersOfTheDesign() {
        assertDp("bouton principal", NiumiDimens.primaryButton, number("boutonPrincipal", "hauteur"))
        assertUniformRadius("bouton principal", NiumiShapes.button, number("boutonPrincipal", "rayon"))
        assertDp("bouton d'alerte ou de scan", NiumiDimens.sheetButton, number("boutonAlerteOuScan", "hauteur"))
        assertUniformRadius("bouton d'alerte ou de scan", NiumiShapes.button, number("boutonAlerteOuScan", "rayon"))
        assertDp("bouton de la recopie", NiumiDimens.transcriptionButton, number("boutonRecopie", "hauteur"))
        assertUniformRadius("bouton de la recopie", NiumiShapes.button, number("boutonRecopie", "rayon"))
        assertDp("bouton Autoriser", NiumiDimens.authorizeButton, number("boutonAutoriser", "hauteur"))
        assertUniformRadius("bouton Autoriser", NiumiShapes.authorizeButton, number("boutonAutoriser", "rayon"))
    }

    /** « Coins égaux à la moitié de la hauteur » : un rayon de 50 %, quelle que soit la hauteur. */
    @Test
    fun pillButtonsAreFullyRounded() {
        val heights =
            forms.getValue("boutonPilule").jsonObject.getValue("hauteurs").jsonArray.map {
                it.jsonPrimitive.content.toFloat()
            }

        assertThat(
            listOf(NiumiDimens.pillButton.value, NiumiDimens.pillButtonLarge.value),
        ).containsExactlyElementsIn(heights).inOrder()
        assertUniformRadius("pilule", NiumiShapes.pill, SQUARE / 2)
    }

    @Test
    fun cardsTilesSheetsAndAppIcons() {
        assertUniformRadius("carte", NiumiShapes.card, number("carte", "rayon"))
        assertUniformRadius("tuile", NiumiShapes.tile, number("tuile", "rayon"))
        val sheet = number("feuilleDuBas", "rayonHaut")
        assertWithMessage(
            "feuille du bas : coins hauts seulement",
        ).that(NiumiShapes.sheet.radii()).containsExactly(sheet, sheet, 0f, 0f).inOrder()
        assertDp("emplacement d'icône", NiumiDimens.appIcon, number("emplacementIcone", "cote"))
        assertUniformRadius("emplacement d'icône", NiumiShapes.appIcon, number("emplacementIcone", "rayon"))
    }

    @Test
    fun dayChipsAndSwitch() {
        assertDp("pastille de jour", NiumiDimens.dayChip, number("pastilleJour", "diametre"))
        assertDp("interrupteur, largeur", NiumiDimens.switchWidth, number("interrupteur", "largeur"))
        assertDp("interrupteur, hauteur", NiumiDimens.switchHeight, number("interrupteur", "hauteur"))
        assertDp("interrupteur, pastille", NiumiDimens.switchThumb, number("interrupteur", "pastille"))
    }

    /** Interrupteur de ligne (feuille Sonnerie, maquette 18 ; décision du 2026-10-23) : 44 × 26, pastille de 20. */
    @Test
    fun rowSwitch() {
        assertDp("interrupteur de ligne, largeur", NiumiDimens.switchRowWidth, number("interrupteurDeLigne", "largeur"))
        assertDp(
            "interrupteur de ligne, hauteur",
            NiumiDimens.switchRowHeight,
            number("interrupteurDeLigne", "hauteur"),
        )
        assertDp(
            "interrupteur de ligne, pastille",
            NiumiDimens.switchRowThumb,
            number("interrupteurDeLigne", "pastille"),
        )
    }

    /** Barre segmentée (feuille Sonnerie, maquette 18) : 48 de haut, coins 14, marge 4, segments de 40, coins 10. */
    @Test
    fun segmentedBar() {
        assertDp("barre segmentée, hauteur", NiumiDimens.segmentedBarHeight, number("barreSegmentee", "hauteur"))
        assertUniformRadius("barre segmentée", NiumiShapes.segmentedBar, number("barreSegmentee", "rayon"))
        assertDp("barre segmentée, marge", NiumiDimens.segmentedBarPadding, number("barreSegmentee", "marge"))
        val segment =
            forms
                .getValue("barreSegmentee")
                .jsonObject
                .getValue("segment")
                .jsonObject
        assertDp(
            "segment, hauteur",
            NiumiDimens.segmentHeight,
            segment
                .getValue("hauteur")
                .jsonPrimitive.content
                .toFloat(),
        )
        assertUniformRadius(
            "segment",
            NiumiShapes.segment,
            segment
                .getValue("rayon")
                .jsonPrimitive.content
                .toFloat(),
        )
    }

    /**
     * Barre d'onglets du bas (maquette 16, décision de Mehdi du 2026-10-25) : 66 de contenu, les 34
     * restants des 100 de la maquette étant la barre d'accueil de l'iPhone ; badge en pilule.
     */
    @Test
    fun tabBar() {
        val bar = forms.getValue("barreDOnglets").jsonObject
        val badge = bar.getValue("badge").jsonObject

        fun Map<String, JsonElement>.dp(field: String) = getValue(field).jsonPrimitive.content.toFloat()

        assertDp("barre d'onglets, hauteur", NiumiDimens.tabBarHeight, bar.dp("hauteur"))
        assertWithMessage("barre d'onglets : hauteur iPhone moins la barre d'accueil")
            .that(bar.dp("hauteurAvecBarreIphone") - NiumiDimens.screenBottom.value)
            .isEqualTo(NiumiDimens.tabBarHeight.value)
        assertDp("onglet, largeur", NiumiDimens.tabBarItemWidth, bar.dp("largeurOnglet"))
        assertDp("onglet, icône", NiumiDimens.tabBarIcon, bar.dp("icone"))
        assertDp("onglet, écart icône-libellé", NiumiDimens.tabBarLabelGap, bar.dp("ecartIconeLibelle"))
        assertDp("badge, hauteur", NiumiDimens.tabBarBadgeHeight, badge.dp("hauteur"))
        assertDp("badge, marge", NiumiDimens.tabBarBadgePadding, badge.dp("margeCotes"))
        assertDp("badge, liseré", NiumiDimens.tabBarBadgeOutline, badge.dp("lisere"))
    }

    /**
     * Carte de section de l'accueil (maquettes du 2026-10-30) : coins de la carte, marges intérieures
     * du cadran, des jours et des lignes (haut, droite, bas, gauche).
     */
    @Test
    fun sectionCard() {
        val card = forms.getValue("carteDeSection").jsonObject

        fun margins(field: String) = card.getValue(field).jsonArray.map { it.jsonPrimitive.content.toFloat() }

        fun PaddingValues.sides() =
            listOf(
                calculateTopPadding().value,
                calculateRightPadding(LayoutDirection.Ltr).value,
                calculateBottomPadding().value,
                calculateLeftPadding(LayoutDirection.Ltr).value,
            )

        assertUniformRadius(
            "carte de section",
            NiumiShapes.card,
            card
                .getValue("rayon")
                .jsonPrimitive.content
                .toFloat(),
        )
        assertWithMessage("marges du cadran")
            .that(NiumiDimens.sectionCardDialPadding.sides())
            .isEqualTo(margins("margesCadran"))
        assertWithMessage("marges des jours")
            .that(NiumiDimens.sectionCardDaysPadding.sides())
            .isEqualTo(margins("margesJours"))
        assertWithMessage("marges des lignes")
            .that(NiumiDimens.sectionCardRowsPadding.sides())
            .isEqualTo(margins("margesLignes"))
    }

    /** Une forme ajoutée au design sans jeton ferait échouer ce test au lieu d'être oubliée. */
    @Test
    fun everyFormOfTheDesignValuesIsCovered() {
        val covered =
            setOf(
                "marges",
                "zoneDeToucherMinimale",
                "boutonPrincipal",
                "boutonAlerteOuScan",
                "boutonRecopie",
                "boutonAutoriser",
                "boutonPilule",
                "pastilleJour",
                "carte",
                "carteDeSection",
                "tuile",
                "feuilleDuBas",
                "emplacementIcone",
                "interrupteur",
                "interrupteurDeLigne",
                "barreSegmentee",
                "barreDOnglets",
            )
        // L'écran de référence des maquettes (390 × 844) n'est pas un jeton : les écrans s'adaptent.
        val notATokenKeys = setOf("ecranDeReference")

        assertThat(forms.keys - notATokenKeys).containsExactlyElementsIn(covered)
    }

    private companion object {
        const val SQUARE = 100f
    }
}
