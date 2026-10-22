package com.niumi.feature.setup.permissions

import com.niumi.system.permissions.NiumiPermission
import com.niumi.system.readiness.ReadinessAction

/** Une tuile de l'écran 2 : accordée (coche) ou non (« Autoriser »). */
data class PermissionTileState(
    val permission: NiumiPermission,
    val granted: Boolean,
)

/**
 * Fenêtres centrées ouvertes par « Autoriser » quand le manque ne se règle pas d'un seul geste
 * système (décision de Mehdi du 2026-10-21 : une fenêtre, pas une page ni une feuille). Leurs
 * textes sont ceux de SPEC_ANDROID §12.3 et §13 : aucun texte nouveau.
 */
enum class PermissionDialog {
    /** Divulgation Google Play (§12.3) : les cinq points, puis le bouton vers les réglages d'accessibilité. */
    CONSENT,

    /** Verrou de Niumi dans les récents (HyperOS) : le geste se fait hors de Niumi, puis relecture. */
    RECENTS_LOCK,

    /** Alarmes exactes indisponibles : rien à demander (`USE_EXACT_ALARM`, §13), seulement expliquer. */
    EXACT_ALARM,
}

/** Ce que l'écran doit lancer après un « Autoriser » ; une fenêtre, elle, est portée par l'état. */
sealed interface PermissionStep {
    /**
     * Fenêtre système `POST_NOTIFICATIONS`, ou les réglages si elle ne peut plus s'ouvrir
     * ([notificationRequestFor]).
     */
    data object RequestNotifications : PermissionStep

    /** Un réglage système, traduit par `settingsIntentFor` (et son repli). */
    data class OpenSettings(
        val action: ReadinessAction,
    ) : PermissionStep

    data object None : PermissionStep
}

/**
 * [canContinue] n'est vrai qu'une fois toutes les tuiles lues et accordées : avant la première
 * lecture, rien n'est affirmé (SPEC_ANDROID §15, ne jamais afficher un faux état).
 */
data class PermissionsUiState(
    val tiles: List<PermissionTileState> = emptyList(),
    val isLoading: Boolean = true,
    val dialog: PermissionDialog? = null,
    /** La fenêtre système des notifications a déjà été proposée (persisté, `SetupPreferences`). */
    val notificationRequestedBefore: Boolean = false,
) {
    val canContinue: Boolean get() = !isLoading && tiles.isNotEmpty() && tiles.all { it.granted }
}
