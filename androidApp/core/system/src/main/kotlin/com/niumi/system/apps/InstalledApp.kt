package com.niumi.system.apps

import android.graphics.drawable.Drawable

/**
 * Une application lançable proposée au sélecteur (SPEC_ANDROID §12.1 : icône, libellé, et nom de
 * package en petit texte quand plusieurs applications partagent le même libellé).
 *
 * [icon] est un `Drawable` du framework et non un type Compose : `:core:system` reste sans
 * interface, l'écran convertit lui-même. Nullable parce que `loadIcon` peut échouer pour une
 * application dont les ressources sont momentanément indisponibles (profil verrouillé, mise à
 * jour en cours) — un sélecteur sans icône vaut mieux qu'un sélecteur vide.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)
