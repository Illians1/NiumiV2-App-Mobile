package com.niumi.designsystem.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.math.abs

/**
 * Accès aux valeurs relevées dans les 58 maquettes (`design-plan/valeurs-de-design.json`), lues
 * dans le dépôt et jamais recopiées : un jeton qui diverge du design fait échouer le test qui le
 * compare, sans seconde copie à tenir à jour.
 */
internal object DesignValues {
    val rootDir: File =
        File(
            requireNotNull(System.getProperty("niumi.rootDir")) {
                "La propriété système niumi.rootDir n'a pas été injectée par le build Gradle."
            },
        )

    private val root: JsonObject by lazy {
        Json.parseToJsonElement(File(rootDir, "design-plan/valeurs-de-design.json").readText()).jsonObject
    }

    fun obj(vararg path: String): JsonObject = path.fold(root) { current, key -> current.getValue(key).jsonObject }

    fun string(
        obj: JsonObject,
        key: String,
    ): String = obj.getValue(key).jsonPrimitive.content

    fun number(
        obj: JsonObject,
        key: String,
    ): Double? = obj[key]?.jsonPrimitive?.content?.toDouble()

    /** Fichier du module `:core:designsystem`, chemin relatif à son dossier. */
    fun moduleFile(relative: String): File = File(rootDir, "androidApp/core/designsystem/$relative")
}

private val RGBA = Regex("""rgba\(\s*(\d+),\s*(\d+),\s*(\d+),\s*([0-9.]+)\s*\)""")
private const val ALPHA_TOLERANCE = 1f / 255f

/**
 * Compare [actual] à une couleur écrite comme dans le JSON : `#RRGGBB` (opaque) ou
 * `rgba(r, g, b, a)` (voile). L'alpha d'un voile est comparé au 1/255 près : une couleur sRGB est
 * stockée sur 8 bits par canal.
 */
internal fun assertColorEquals(
    label: String,
    actual: Color,
    css: String,
) {
    val rgba = RGBA.matchEntire(css)
    if (rgba == null) {
        require(css.matches(Regex("#[0-9A-Fa-f]{6}"))) { "Couleur illisible dans le JSON : $css" }
        val expected = (0xFF000000L or css.drop(1).toLong(16)).toInt()
        assertWithMessage("$label (attendu $css)")
            .that(Integer.toHexString(actual.toArgb()).uppercase())
            .isEqualTo(Integer.toHexString(expected).uppercase())
    } else {
        val (r, g, b) =
            rgba.groupValues
                .drop(1)
                .take(3)
                .map { it.toInt() }
        val alpha = rgba.groupValues[4].toFloat()
        val argb = actual.toArgb()
        assertWithMessage("$label rouge").that((argb shr 16) and 0xFF).isEqualTo(r)
        assertWithMessage("$label vert").that((argb shr 8) and 0xFF).isEqualTo(g)
        assertWithMessage("$label bleu").that(argb and 0xFF).isEqualTo(b)
        assertWithMessage("$label alpha (attendu $alpha, obtenu ${actual.alpha})")
            .that(abs(actual.alpha - alpha) <= ALPHA_TOLERANCE)
            .isTrue()
    }
}
