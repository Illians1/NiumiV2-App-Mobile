package com.niumi.designsystem.component

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.ui.theme.LocalNiumiPalette
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.NiumiType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Aide à la validation visuelle de l'étape 34, pas une vérification : compose chaque famille de
 * composants comme dans les maquettes 01, 13 à 16, 19, 23 et 37, en clair puis en sombre, et
 * enregistre une image par famille dans `files/captures` de l'APK de test, à tirer par
 * `adb pull` et à comparer aux images de `design-plan/maquettes/images`.
 */
@RunWith(AndroidJUnit4::class)
class NiumiComponentsCaptureTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val directory: File =
        File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "captures",
        ).apply { mkdirs() }

    private val samples: List<Pair<String, @Composable (Boolean) -> Unit>> =
        listOf(
            "boutons" to { _ -> ButtonsSample() },
            "pilules" to { _ -> PillsSample() },
            "jours" to { dark -> DaysSample(dark) },
            "rangees" to { dark -> RowsSample(dark) },
            "cartes" to { _ -> CardsSample() },
            "cartes-de-section" to { _ -> SectionCardsSample() },
            "haut" to { _ -> TopSample() },
        )

    @Test
    fun captureTheLightComponents() = captureAll(dark = false)

    @Test
    fun captureTheDarkComponents() = captureAll(dark = true)

    @Test
    fun captureTheLightSheet() = captureSheet(dark = false)

    @Test
    fun captureTheDarkSheet() = captureSheet(dark = true)

    private fun captureAll(dark: Boolean) {
        var current by mutableStateOf(samples.first())
        composeRule.setContent { Frame(dark) { current.second(dark) } }
        samples.forEach { sample ->
            current = sample
            composeRule.waitForIdle()
            save("${sample.first}-${theme(dark)}", composeRule.onRoot().captureToImage().asAndroidBitmap())
        }
    }

    private fun captureSheet(dark: Boolean) {
        composeRule.setContent {
            Frame(dark) {
                Text("Écran derrière la feuille", style = NiumiType.screenTitle28)
                if (dark) {
                    NiumiBottomSheet(onDismiss = {}, title = "Annuler le réveil ?") {
                        Text(
                            "Scanne ton Niumi Spot pour annuler.",
                            style = NiumiType.body16,
                            color = LocalNiumiPalette.current.textSecondary,
                        )
                        NiumiPrimaryButton(text = "Scanner le Niumi Spot", onClick = {})
                        NiumiDiscreetLink(text = "Je n’ai pas accès à mon Niumi Spot", onClick = {})
                    }
                } else {
                    NiumiBottomSheet(onDismiss = {}, title = "Sonnerie", closeLabel = "Fermer") {
                        NiumiListRow(title = "Volume du téléphone", onClick = null, bottomSeparator = true) {
                            Text("80 %")
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        // La feuille vit dans sa propre fenêtre, que `captureToImage` ne voit pas : capture de
        // l'écran entier par le système, voile et écran derrière compris.
        val screen = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        save("feuille-${theme(dark)}", screen)
    }

    private fun theme(dark: Boolean) = if (dark) "sombre" else "clair"

    private fun save(
        name: String,
        bitmap: Bitmap,
    ) {
        val file = File(directory, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        assertThat(file.length()).isGreaterThan(0L)
    }

    private companion object {
        const val PNG_QUALITY = 100
    }
}

@Composable
private fun Frame(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    NiumiTheme(dark = dark) {
        CompositionLocalProvider(LocalContentColor provides LocalNiumiPalette.current.ink) { FrameColumn(content) }
    }
}

@Composable
private fun FrameColumn(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(LocalNiumiPalette.current.background)
                .padding(NiumiDimens.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(NiumiDimens.rowGap),
    ) { content() }
}

@Composable
private fun ButtonsSample() {
    NiumiPrimaryButton(text = "Continuer", onClick = {})
    NiumiPrimaryButton(text = "Continuer", onClick = {}, enabled = false)
    NiumiOutlinedButton(text = "Recopier le texte", onClick = {})
    NiumiDiscreetLink(text = "Je n’ai pas accès à mon Niumi Spot", onClick = {})
}

@Composable
private fun PillsSample() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NiumiDimens.trailingGap)) {
        listOf("Non", "1 min", "2 min", "5 min", "10 min").forEach { label ->
            NiumiPillButton(text = label, selected = label == "2 min", onClick = {}, modifier = Modifier.weight(1f))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(NiumiDimens.trailingGap)) {
        NiumiActionPill(text = "Modifier", onClick = {})
        NiumiActionPill(
            text = "Choisir les applications",
            onClick = {},
            filled = true,
            height = NiumiDimens.pillButtonLarge,
        )
    }
}

private val days = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

@Composable
private fun Week(state: NiumiFieldState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { index, day ->
            NiumiDayChip(
                label = day.take(1),
                accessibilityLabel = day,
                checked = index < days.size - 2,
                onToggle = {},
                state = state,
            )
        }
    }
}

@Composable
private fun DaysSample(dark: Boolean) {
    Week(NiumiFieldState.Normal)
    Week(if (dark) NiumiFieldState.Locked else NiumiFieldState.Greyed)
}

@Composable
private fun RowsSample(dark: Boolean) {
    val other = if (dark) NiumiFieldState.Locked else NiumiFieldState.Greyed
    Column {
        NiumiListRow(title = "Niumi Spots", onClick = {}) { Text("2") }
        NiumiListRow(title = "Autorisations", onClick = {}) { Text("3 sur 3") }
        NiumiListRow(title = "Aide", onClick = {}, bottomSeparator = true)
    }
    Column {
        NiumiListRow(title = "Applications bloquées", onClick = {}, state = other) { Text("6") }
        NiumiListRow(title = "Sonnerie", onClick = {}, state = other, bottomSeparator = true) { Text("Carillon") }
    }
}

/** Cartes de section de l'accueil (maquettes du 2026-10-30) : ombre en clair, liseré en sombre. */
@Composable
private fun SectionCardsSample() {
    NiumiSectionCard(contentPadding = NiumiDimens.sectionCardDaysPadding) {
        Text("Jours de réveil", style = NiumiType.body16)
    }
    NiumiSectionCard(contentPadding = NiumiDimens.sectionCardDaysPadding) {
        Text("Applications bloquées", style = NiumiType.body16)
    }
}

@Composable
private fun CardsSample() {
    val palette = LocalNiumiPalette.current
    NiumiCard(filled = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Réveil activé", style = NiumiType.label16)
                Text("Blocage des apps dans 56 min", style = NiumiType.secondary14, color = palette.onInkSecondary)
                Text("8 h 30 de déconnexion", style = NiumiType.secondary14, color = palette.onInkSecondary)
            }
            NiumiSwitch(checked = true, onCheckedChange = {})
        }
    }
    NiumiCard(filled = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Réveil désactivé", style = NiumiType.label16)
                Text("Aucun réveil ni blocage prévu", style = NiumiType.secondary14, color = palette.textSecondary)
            }
            NiumiSwitch(checked = false, onCheckedChange = {})
        }
    }
}

@Composable
private fun TopSample() {
    NiumiTopBar(onBack = {}, backLabel = "Retour à l’accueil", title = "Réglages")
    NiumiLogo()
    NiumiLogo(height = NiumiDimens.logoLargeHeight)
    NiumiPageIndicator(count = 2, current = 0, label = "Panneau 1 sur 2")
}
