package com.niumi.database

/**
 * Une application bloquée pour une session, avec son libellé figé au moment de l'activation
 * (SPEC_ANDROID §7.2, §12.2). Le libellé accompagne le package plutôt que d'être résolu par
 * `PackageManager` au moment de l'affichage de l'overlay : le texte imposé par §12.2
 * (« {Nom de l'application} reste bloquée… ») reste ainsi disponible même si l'application
 * bloquée devient invisible au `PackageManager` (désinstallée, profil désactivé). Réutilisé
 * tel quel par `AndroidSessionExtras` à l'étape 9 (« Interfaces transverses » du plan MVP).
 */
data class BlockedPackage(
    val packageName: String,
    val displayNameSnapshot: String,
)
