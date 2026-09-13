package com.niumi.feature.ringing.ui

import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.database.PendingEffect

/** Une étape du nettoyage de fin de session, telle que l'écran de réveil la présente. */
data class ReleaseStep(
    val kind: SessionEffectKindDto,
    val label: String,
    val done: Boolean,
)

/**
 * Progression du nettoyage pendant `RELEASING` (SPEC_ANDROID §10.4 : « afficher la progression de
 * nettoyage »). Fonction pure : elle se déduit entièrement de l'outbox, sans nouvelle lecture.
 *
 * `SessionPersistenceGateway.pendingEffects` ne rend que les effets **rejouables** (`PENDING` et
 * `FAILED`) : un effet absent de cette liste est donc terminé, qu'il ait réussi ou que sa
 * précondition ait déjà été satisfaite (SPEC_CORE_KMP §6). Un effet `FAILED` reste « en cours » —
 * il sera rejoué, et l'annoncer terminé serait le « faux état de fiabilité » que §15 interdit.
 *
 * Seuls les trois effets **requis** de la phase de libération sont présentés (SPEC_CORE_KMP §6).
 * `PUBLISH_PLATFORM_SNAPSHOT` et `CLEAR_SCAN_REQUEST` sont best-effort : leur échec ne bloque
 * jamais la phase, et les afficher en attente ferait croire à un blocage inexistant.
 */
object ReleaseProgress {
    private val STEPS: Map<SessionEffectKindDto, String> =
        linkedMapOf(
            SessionEffectKindDto.CANCEL_ALARM to "Réveil annulé",
            SessionEffectKindDto.STOP_RINGING to "Sonnerie arrêtée",
            SessionEffectKindDto.REMOVE_BLOCKING to "Applications débloquées",
        )

    fun from(replayableEffects: List<PendingEffect>): List<ReleaseStep> {
        val stillRunning = replayableEffects.map { it.kind }.toSet()
        return STEPS.map { (kind, label) -> ReleaseStep(kind, label, done = kind !in stillRunning) }
    }
}
