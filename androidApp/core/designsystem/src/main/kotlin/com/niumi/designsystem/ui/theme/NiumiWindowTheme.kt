package com.niumi.designsystem.ui.theme

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.toArgb

/**
 * Fenêtre d'une activité accordée au thème de l'app, jamais au réglage du téléphone (étape 33) :
 * barres système ([enableNiumiEdgeToEdge]) et fond de fenêtre. Le fond compte parce que
 * `MainActivity` démarre sur le thème de lancement sombre (`Theme.Niumi.Launch`) : sans ce
 * remplacement, une transition ou un écran clair laisserait voir du sombre derrière lui. À rappeler
 * à chaque changement de thème.
 */
fun ComponentActivity.applyNiumiWindowTheme(dark: Boolean) {
    enableNiumiEdgeToEdge(dark)
    val palette = if (dark) NiumiPalettes.Dark else NiumiPalettes.Light
    window.setBackgroundDrawable(ColorDrawable(palette.background.toArgb()))
}

/**
 * Affichage bord à bord (SPEC_ANDROID §15, « Règles UI »), avec des barres système accordées au
 * thème de l'app et non au réglage du téléphone : `enableEdgeToEdge()` sans argument choisit la
 * couleur des icônes d'après le mode sombre du système, ce qui laisserait des icônes blanches
 * invisibles sur un écran clair quand le téléphone est en mode sombre (étape 33).
 *
 * Barres transparentes : le fond de l'écran passe dessous.
 */
fun ComponentActivity.enableNiumiEdgeToEdge(dark: Boolean) {
    val style =
        if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
}

/**
 * Fenêtre claire tant que l'écran qui l'appelle est affiché, quel que soit le thème de la session,
 * puis rendue au thème [sessionDark] (étape 46, écran 11 · Réussite, toujours clair) : sans cela,
 * une session suivante déjà sombre (période de 24 h en Quotidien) laisserait des icônes blanches
 * sur la page blanche. Relancé si le thème de la session change pendant l'affichage.
 */
@Composable
fun NiumiLightWindowWhileShown(sessionDark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    DisposableEffect(activity, sessionDark) {
        activity.applyNiumiWindowTheme(dark = false)
        onDispose { activity.applyNiumiWindowTheme(dark = sessionDark) }
    }
}
