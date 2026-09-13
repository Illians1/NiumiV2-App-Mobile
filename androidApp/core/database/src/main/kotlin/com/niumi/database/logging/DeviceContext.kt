package com.niumi.database.logging

/**
 * Contexte d'appareil porté par chaque événement du journal technique (SPEC_ANDROID §17 :
 * « Chaque événement contient seulement l'heure, le type, l'identifiant de session, le modèle de
 * l'appareil, la version Android, la version de l'application et un code d'erreur contrôlé »).
 *
 * Trois chaînes déjà formatées plutôt que les valeurs brutes de `Build` : le journal est lu par un
 * humain dans l'export (§17), jamais interprété par du code, et la mise en forme n'a donc pas à
 * être refaite à chaque lecture. Aucun identifiant matériel n'y figure — `Build.MODEL` nomme un
 * modèle commercial, jamais un appareil précis (§16 : « masquer […] tout identifiant matériel »).
 *
 * Porté par entrée et non mis en facteur en tête d'export : `appVersion` change lors d'une mise à
 * jour de l'application, et un journal de 200 événements peut enjamber cette mise à jour. Un
 * en-tête unique attribuerait alors la nouvelle version à des événements antérieurs.
 */
data class DeviceContext(
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
)
