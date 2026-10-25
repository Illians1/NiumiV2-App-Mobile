package com.niumi.designsystem.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.unit.TextUnit
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.niumi.designsystem.R
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import kotlin.math.abs

/**
 * L'échelle de texte de Niumi est celle relevée dans les maquettes (VALEURS-DE-DESIGN.md §3,
 * `valeurs-de-design.json` → `tailles`), en IBM Plex Sans embarquée. Unité des maquettes : le point
 * iPhone, soit le dp Android ; les tailles de texte en sp suivent le réglage de taille du téléphone.
 */
class NiumiTypeTest {
    private val sizes = DesignValues.obj("tailles")

    /** Clé de `tailles` → style, et clé de graisse lue (`graisse` sauf variantes choisie/cochée). */
    private val styles: Map<String, List<Pair<String, TextStyle>>> =
        mapOf(
            "heureReveil" to listOf("graisse" to NiumiType.clock112),
            "chiffresCadran" to listOf("graisse" to NiumiType.dial42),
            "valeurRoueHeure" to listOf("graisse" to NiumiType.wheelSelected34),
            "voisineRoueHeure" to listOf("graisse" to NiumiType.wheelNear22),
            "eloigneeRoueHeure" to listOf("graisse" to NiumiType.wheelFar18),
            "ongletAccueil" to listOf("graisse" to NiumiType.tab14, "graisseChoisie" to NiumiType.tabSelected14),
            "titreReussite" to listOf("graisse" to NiumiType.title40),
            "consigneReveil" to listOf("graisse" to NiumiType.instruction30),
            // D19 (tranché le 2026-10-19) : 28 partout, écran 2 compris.
            "titreEcran" to listOf("graisse" to NiumiType.screenTitle28),
            "titreFeuilleScan" to listOf("graisse" to NiumiType.scanSheetTitle26),
            "titreFeuille" to listOf("graisse" to NiumiType.sheetTitle24),
            "titreAlerte" to listOf("graisse" to NiumiType.alertTitle22),
            "consigneEcran3" to listOf("graisse" to NiumiType.instruction22),
            "titreCarte" to listOf("graisse" to NiumiType.cardTitle20),
            "sousTitre" to listOf("graisse" to NiumiType.subtitle20),
            "boutonPrincipal" to listOf("graisse" to NiumiType.button17),
            "heureAutourDuCadran" to listOf("graisse" to NiumiType.dialHour17),
            "texteSousTitre" to listOf("graisse" to NiumiType.lead17),
            "libelleFort" to listOf("graisse" to NiumiType.labelStrong16),
            "libelleMoyen" to listOf("graisse" to NiumiType.label16),
            "texteCourant" to listOf("graisse" to NiumiType.body16),
            "pastille" to listOf("graisse" to NiumiType.chip15, "graisseChoisie" to NiumiType.chipSelected15),
            "texteSecondaire" to listOf("graisse" to NiumiType.secondary14),
            "legendeEtMention" to listOf("graisse" to NiumiType.caption13),
            "nomSousIcone" to listOf("graisse" to NiumiType.appLabel12, "graisseCochee" to NiumiType.appLabelChecked12),
            "ongletBarre" to
                listOf("graisse" to NiumiType.tabBarLabel12, "graisseChoisie" to NiumiType.tabBarLabelSelected12),
            "badgeBientot" to listOf("graisse" to NiumiType.badge9),
        )

    @Test
    fun everyTextSizeOfTheDesignValuesHasAStyle() {
        assertThat(sizes.keys).containsExactlyElementsIn(styles.keys)
    }

    @Test
    fun everyStyleHasTheSizeWeightLineHeightAndLetterSpacingOfTheDesign() {
        styles.forEach { (key, variants) ->
            val spec = sizes.getValue(key).jsonObject
            val size = requireNotNull(DesignValues.number(spec, "taille"))
            variants.forEach { (weightKey, style) ->
                val label = "$key/$weightKey"
                assertTextUnit("$label taille", style.fontSize, size)
                assertWithMessage("$label graisse")
                    .that(style.fontWeight)
                    .isEqualTo(FontWeight(requireNotNull(DesignValues.number(spec, weightKey)).toInt()))
                assertTextUnit(
                    "$label interligne",
                    style.lineHeight,
                    DesignValues.number(spec, "interligne")?.times(size),
                )
                assertTextUnit("$label interlettrage", style.letterSpacing, DesignValues.number(spec, "interlettrage"))
            }
        }
    }

    /** VALEURS §3 : « Désactivé » au centre du cadran, 30/500, interlettrage −0,5 (absent du JSON). */
    @Test
    fun theDisabledDialLabelIsThirtyMedium() {
        assertTextUnit("dialDisabled30 taille", NiumiType.dialDisabled30.fontSize, 30.0)
        assertThat(NiumiType.dialDisabled30.fontWeight).isEqualTo(FontWeight.Medium)
        assertTextUnit("dialDisabled30 interlettrage", NiumiType.dialDisabled30.letterSpacing, -0.5)
    }

    @Test
    fun everyStyleIsSetInTheEmbeddedPlexSans() {
        (allStyles() + NiumiType.dialDisabled30).forEach { style ->
            assertThat(style.fontFamily).isSameInstanceAs(NiumiFont.PlexSans)
        }
    }

    /** Trois graisses, un fichier embarqué chacune, pas d'italique (VALEURS §3, `police/LISEZ-MOI.md`). */
    @Test
    fun plexSansEmbedsRegularMediumAndSemiBold() {
        val fonts = (NiumiFont.PlexSans as FontListFontFamily).fonts.map { it as ResourceFont }

        assertThat(fonts.map { it.weight to it.resId })
            .containsExactly(
                FontWeight.Normal to R.font.ibm_plex_sans_regular,
                FontWeight.Medium to R.font.ibm_plex_sans_medium,
                FontWeight.SemiBold to R.font.ibm_plex_sans_semibold,
            )
        assertThat(fonts.map { it.style.toString() }.distinct()).containsExactly("Normal")
    }

    /** Les composants Material résiduels n'écrivent qu'avec l'échelle Niumi. */
    @Test
    fun theMaterialTypographyIsBuiltOnlyFromNiumiStyles() {
        val typography = NiumiType.material()
        val material =
            listOf(
                typography.displayLarge,
                typography.displayMedium,
                typography.displaySmall,
                typography.headlineLarge,
                typography.headlineMedium,
                typography.headlineSmall,
                typography.titleLarge,
                typography.titleMedium,
                typography.titleSmall,
                typography.bodyLarge,
                typography.bodyMedium,
                typography.bodySmall,
                typography.labelLarge,
                typography.labelMedium,
                typography.labelSmall,
            )
        val niumi = allStyles() + NiumiType.dialDisabled30

        material.forEach { style -> assertThat(niumi).contains(style) }
    }

    /**
     * Le `TimePicker` Material (écran 5, jusqu'à l'étape 45) écrit heures et minutes en
     * `displayLarge` dans des champs de 96 × 80 dp coupés à leur forme : un style plus grand que
     * celui de Material (57 sp) y serait tronqué. `clock112` n'est lu que directement par l'écran
     * de réveil.
     */
    @Test
    fun theMaterialDisplayLargeFitsTheTimePickerFields() {
        assertThat(
            NiumiType
                .material()
                .displayLarge.fontSize.value,
        ).isAtMost(MATERIAL_DISPLAY_LARGE_SP)
    }

    private fun allStyles(): List<TextStyle> = styles.values.flatten().map { it.second }

    private fun assertTextUnit(
        label: String,
        actual: TextUnit,
        expected: Double?,
    ) {
        if (expected == null) {
            assertWithMessage(label).that(actual).isEqualTo(TextUnit.Unspecified)
        } else {
            assertWithMessage("$label est en sp").that(actual.isSp).isTrue()
            assertWithMessage("$label (attendu $expected, obtenu ${actual.value})")
                .that(abs(actual.value - expected) < TOLERANCE)
                .isTrue()
        }
    }

    private companion object {
        const val TOLERANCE = 0.001
        const val MATERIAL_DISPLAY_LARGE_SP = 57f
    }
}
