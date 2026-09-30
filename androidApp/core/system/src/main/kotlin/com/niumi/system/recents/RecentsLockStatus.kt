package com.niumi.system.recents

import android.content.Context
import android.provider.Settings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Le verrou de Niumi dans le panneau des applications récentes de HyperOS (SPEC_ANDROID §13 ;
 * étape 25).
 *
 * **Mesuré le 2026-09-28** sur Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.302.0 : « Tout
 * effacer » (`OneKeyClean`) tue le processus de Niumi même sans tâche, désactive son service
 * d'accessibilité et coupe le blocage jusqu'à réactivation manuelle — **sauf si Niumi est
 * verrouillé** (appui long sur sa carte, cadenas), auquel cas il est épargné. Le verrou est rangé
 * par paquet dans le réglage système `locked_apps`, et survit aux mises à jour comme aux
 * redémarrages (mesuré). Il est donc à poser une fois, et vérifiable ici.
 *
 * [RecentsLockState.UNSUPPORTED] quand le réglage n'existe pas : l'appareil n'a pas ce mécanisme, le
 * contrôle est sans objet. Aucune détection de constructeur : c'est la présence du réglage qui dit
 * si la question se pose.
 */
fun interface RecentsLockStatus {
    fun read(): RecentsLockState
}

enum class RecentsLockState {
    LOCKED,
    UNLOCKED,
    UNSUPPORTED,
}

class AndroidRecentsLockStatus(
    private val context: Context,
) : RecentsLockStatus {
    override fun read(): RecentsLockState =
        LockedAppsParser.stateOf(
            Settings.System.getString(context.contentResolver, LOCKED_APPS_SETTING),
            context.packageName,
        )

    private companion object {
        const val LOCKED_APPS_SETTING = "locked_apps"
    }
}

/**
 * Lecture pure du réglage `locked_apps` de HyperOS, testable en JVM. Forme relevée sur appareil :
 * `[{"u":0,"pkgs":["com.niumi.app"]},{"u":-100,"pkgs":["…"]}]`, une entrée par utilisateur.
 *
 * Un réglage absent vaut [RecentsLockState.UNSUPPORTED] ; un réglage présent mais illisible aussi —
 * ce qu'un contrôle ne peut pas lire honnêtement, il ne le juge pas (point de vigilance 11 du plan),
 * et le journal d'un diagnostic bloqué à tort serait pire qu'un contrôle muet.
 */
object LockedAppsParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun stateOf(
        raw: String?,
        packageName: String,
    ): RecentsLockState {
        val entries =
            raw
                ?.takeUnless { it.isBlank() }
                ?.let { runCatching { json.parseToJsonElement(it).jsonArray }.getOrNull() }
                ?: return RecentsLockState.UNSUPPORTED
        val locked = entries.any { entry -> lockedPackagesOf(entry).contains(packageName) }
        return if (locked) RecentsLockState.LOCKED else RecentsLockState.UNLOCKED
    }

    /** Une entrée mal formée ne verrouille rien : elle n'a pas à faire échouer les autres. */
    private fun lockedPackagesOf(entry: JsonElement): List<String> =
        runCatching {
            entry.jsonObject["pkgs"]
                ?.jsonArray
                .orEmpty()
                .map { it.jsonPrimitive.content }
        }.getOrDefault(emptyList())
}
