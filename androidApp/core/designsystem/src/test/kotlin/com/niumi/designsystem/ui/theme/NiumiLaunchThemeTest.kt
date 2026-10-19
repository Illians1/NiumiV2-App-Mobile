package com.niumi.designsystem.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import kotlin.math.sqrt

/**
 * Écran de lancement (décision de Mehdi du 2026-10-19) : tant que Niumi ne sait pas s'il y a une
 * session, l'écran de démarrage du système reste affiché, sombre, avec le logo blanc cassé. Aucun
 * fond clair ne précède donc jamais un accueil sombre. Même règle sur toutes les versions prises en
 * charge : Android 10 et 11 affichent la fenêtre de démarrage avec `windowBackground` du thème de
 * l'activité, Android 12 et plus l'écran de démarrage système avec ses attributs `windowSplashScreen*`.
 */
class NiumiLaunchThemeTest {
    private val themes = DesignValues.moduleFile("src/main/res/values/themes.xml").readText()
    private val themesV31 = DesignValues.moduleFile("src/main/res/values-v31/themes.xml").readText()

    @Test
    fun theLaunchThemeIsDarkOnEveryAndroidVersion() {
        assertThat(themes).contains("""<style name="Theme.Niumi.Launch" parent="Theme.Niumi.Dark"""")
        assertThat(themesV31).contains("""<style name="Theme.Niumi.Launch" parent="Theme.Niumi.Dark"""")
        assertThat(
            themesV31,
        ).contains("""<item name="android:windowSplashScreenBackground">@color/niumi_background_dark</item>""")
    }

    @Test
    fun theSplashShowsTheLogoForDarkBackground() {
        assertThat(
            themesV31,
        ).contains("""<item name="android:windowSplashScreenAnimatedIcon">@drawable/niumi_splash_icon</item>""")
        val icon = DesignValues.moduleFile("src/main/res/drawable/niumi_splash_icon.xml").readText()
        assertThat(icon).contains("<inset")
        assertThat(icon).contains("""android:drawable="@drawable/niumi_logo_dark"""")
    }

    /**
     * Un `InsetDrawable` étire son contenu aux bornes restantes : les insets doivent laisser
     * exactement le rapport du logo (506,5 × 121), sinon il serait déformé. Et l'icône d'un écran
     * de démarrage sans fond est dessinée sur 288 dp puis coupée à un cercle de 192 dp (Android
     * Developers, « Splash screens ») : le logo doit tenir dans un cercle des deux tiers du carré.
     * Les marges sont en pourcentage : le système ne dessine pas l'icône à sa taille nominale, et
     * des marges fixes en dp y laissaient une zone aux mauvaises proportions. Mesuré le 2026-10-19
     * sur HyperOS : avec des marges en dp, le logo était rogné et étiré deux fois en hauteur.
     */
    @Test
    fun theSplashInsetsKeepTheLogoAspectRatio() {
        val icon = DesignValues.moduleFile("src/main/res/drawable/niumi_splash_icon.xml").readText()

        fun inset(side: String) =
            Regex("""android:inset$side="([0-9.]+)%"""")
                .find(icon)
                ?.groupValues
                ?.get(1)
                ?.toDouble()
                ?.div(PERCENT)
                ?: error("inset$side absent ou pas en pourcentage")

        assertThat(inset("Left")).isEqualTo(inset("Right"))
        assertThat(inset("Top")).isEqualTo(inset("Bottom"))
        val width = 1 - 2 * inset("Left")
        val height = 1 - 2 * inset("Top")
        assertThat(width / height).isWithin(ASPECT_TOLERANCE).of(LOGO_WIDTH / LOGO_HEIGHT)
        assertThat(sqrt(width * width + height * height)).isAtMost(VISIBLE_CIRCLE_FRACTION)
    }

    private companion object {
        const val PERCENT = 100.0
        const val LOGO_WIDTH = 506.5
        const val LOGO_HEIGHT = 121.0
        const val ASPECT_TOLERANCE = 0.01

        // 192 dp visibles sur 288 dp dessinés : un cercle des deux tiers du carré.
        const val VISIBLE_CIRCLE_FRACTION = 192.0 / 288
    }

    @Test
    fun theMainActivityStartsWithTheLaunchTheme() {
        val manifest = File(DesignValues.rootDir, "androidApp/app/src/main/AndroidManifest.xml").readText()
        val mainActivity =
            Regex(
                """<activity\s+android:name="\.MainActivity".*?/>""",
                RegexOption.DOT_MATCHES_ALL,
            ).find(manifest)?.value
                ?: error("MainActivity absente du manifeste")

        assertThat(mainActivity).contains("""android:theme="@style/Theme.Niumi.Launch"""")
    }
}
