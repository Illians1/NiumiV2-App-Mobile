package com.niumi.feature.session.home

import com.niumi.designsystem.drawing.DialHandle
import com.niumi.feature.session.ui.AlarmSoundSummary
import com.niumi.system.apps.InstalledApp

/**
 * Les états de l'accueil (`design-plan/SPECIFICATION.md` §9, décisions du 2026-10-25), par priorité :
 * la session d'abord (thème, verrouillage), puis le planning.
 */
enum class HomeMode {
    /** Du coucher au scan : sombre, tout est verrouillé sauf la sonnerie. */
    InSession,

    /** Interrupteur éteint : tout est grisé et reste modifiable. */
    Disabled,

    /** Interrupteur allumé mais l'armement a échoué (Q14) : jamais un faux « Réveil prévu ». */
    NothingArmed,

    /** Interrupteur allumé, réveil programmé. */
    Scheduled,
}

/** Onglets au-dessus des pastilles (maquette 18) : aucun jour coché, les sept, ou un choix libre. */
enum class DayMode { ONE_OFF, DAILY, CUSTOM }

/**
 * Ce que montre l'accueil. Heures en minutes depuis minuit. [cardLines] : les lignes sous le titre de
 * la carte ; [apps] : une icône par application choisie, l'icône manquante d'une application
 * désinstallée comprise. [message] : la bulle du bas (M10) ; [picker] : le sélecteur d'heure ouvert,
 * sur l'onglet de cette poignée.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val mode: HomeMode = HomeMode.Disabled,
    val bedtimeMinutes: Int = 0,
    val wakeMinutes: Int = 0,
    val nowMinutes: Int = 0,
    val enabledIsoDays: Set<Int> = emptySet(),
    val dayMode: DayMode = DayMode.ONE_OFF,
    val centerLabel: String = "",
    val centerValue: String = "",
    val cardTitle: String = "",
    val cardLines: List<String> = emptyList(),
    val switchChecked: Boolean = false,
    val apps: List<InstalledApp> = emptyList(),
    val ringtone: AlarmSoundSummary? = null,
    val message: String? = null,
    val picker: DialHandle? = null,
) {
    /** Du coucher au scan, heures, jours et applications sont verrouillés (§2). */
    val isLocked: Boolean get() = mode == HomeMode.InSession
}
