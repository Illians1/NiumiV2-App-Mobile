package com.niumi.designsystem.ui.theme

import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Fichiers de marque embarqués à l'étape 33 : la police IBM Plex Sans prise dans l'archive
 * officielle d'IBM (`@ibm/plex-sans@1.1.0`), le texte de licence livré avec elle (que la page
 * Licences affichera, étape 51), et le logo converti depuis les SVG de `design-plan/logo/`.
 */
class NiumiBrandAssetsTest {
    @Test
    fun theThreeWeightsAreTrueTypeFiles() {
        listOf("regular", "medium", "semibold").forEach { weight ->
            val file = DesignValues.moduleFile("src/main/res/font/ibm_plex_sans_$weight.ttf")
            assertWithMessage("${file.name} présent").that(file.isFile).isTrue()
            val magic = file.inputStream().use { it.readNBytes(4) }
            assertWithMessage("${file.name} est un TrueType")
                .that(magic.toList())
                .isEqualTo(listOf<Byte>(0, 1, 0, 0))
        }
    }

    /** OFL 1.1 : la mention de droit d'auteur et le texte de la licence accompagnent la police. */
    @Test
    fun theLicenceShippedWithTheFontIsEmbedded() {
        val licence = DesignValues.moduleFile("src/main/res/raw/ofl_ibm_plex_sans.txt").readText()

        assertThat(licence).contains("Copyright © 2017 IBM Corp.")
        assertThat(licence).contains("SIL Open Font License, Version 1.1")
        assertThat(licence).contains("PERMISSION & CONDITIONS")
        assertThat(licence).contains("DISCLAIMER")
    }

    /** Les deux logos dessinent exactement les traits du SVG fourni par Mehdi, dans sa couleur. */
    @Test
    fun theLogosDrawTheStrokesOfTheSvgSources() {
        listOf(
            Triple("niumi_logo_light.xml", "niumi-logo-fond-clair.svg", "FF0B0B0B"),
            Triple("niumi_logo_dark.xml", "niumi-logo-fond-sombre.svg", "FFFAFAF8"),
        ).forEach { (drawable, svg, strokeColor) ->
            val vector = DesignValues.moduleFile("src/main/res/drawable/$drawable").readText()
            val source = File(DesignValues.rootDir, "design-plan/logo/$svg").readText()

            val svgStrokes =
                Regex(
                    """<path d="([^"]+)"""",
                ).findAll(source).map { normalized(it.groupValues[1]) }.toList()
            val vectorPaths =
                Regex(
                    """android:pathData="([^"]+)"""",
                ).findAll(vector).map { normalized(it.groupValues[1]) }.toList()

            assertWithMessage(drawable).that(svgStrokes).hasSize(6)
            assertWithMessage(drawable).that(vectorPaths.dropLast(1)).containsExactlyElementsIn(svgStrokes).inOrder()
            assertWithMessage("$drawable : point ambre")
                .that(vectorPaths.last())
                .isEqualTo(normalized(circleOf(source)))
            assertWithMessage("$drawable : couleur des traits")
                .that(
                    Regex("""android:strokeColor="#([0-9A-F]{8})"""").findAll(vector).map { it.groupValues[1] }.toSet(),
                ).containsExactly(strokeColor)
            assertWithMessage("$drawable : point ambre de la palette")
                .that(Regex("""android:fillColor="#([0-9A-F]{8})"""").find(vector)?.groupValues?.get(1))
                .isEqualTo(Integer.toHexString(NiumiPalettes.Light.amber.toArgb()).uppercase())
            val box = Regex("""viewBox="([0-9.]+) ([0-9.]+) ([0-9.]+) ([0-9.]+)"""").find(source)!!.groupValues
            assertWithMessage("$drawable : viewBox du SVG").that(vector).contains("""android:translateY="-${box[2]}"""")
            assertWithMessage(
                "$drawable : largeur du cadre",
            ).that(vector).contains("""android:viewportWidth="${box[3]}"""")
            assertWithMessage(
                "$drawable : hauteur du cadre",
            ).that(vector).contains("""android:viewportHeight="${box[4]}"""")
        }
    }

    /** Le cercle SVG, réécrit comme deux demi-arcs (VectorDrawable n'a pas de primitive cercle). */
    private fun circleOf(svg: String): String {
        val (cx, cy, r) =
            Regex("""<circle cx="([0-9.]+)" cy="([0-9.]+)" r="([0-9.]+)"""")
                .find(svg)!!
                .destructured
                .let { (x, y, radius) -> Triple(x.toDouble(), y.toDouble(), radius.toDouble()) }
        val left = fmt(cx - r)
        val right = fmt(cx + r)
        return "M$left ${fmt(cy)} A${fmt(r)} ${fmt(r)} 0 1 0 $right ${fmt(cy)} " +
            "A${fmt(r)} ${fmt(r)} 0 1 0 $left ${fmt(cy)} Z"
    }

    private fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    private fun normalized(path: String): String = path.replace(',', ' ').trim().replace(Regex("""\s+"""), " ")
}
