package com.niumi.system.blocking.guard

import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.system.session.LoadResult
import com.niumi.system.session.isBlockingInForce
import com.niumi.system.session.isSessionInProgress

/**
 * Phase de la session, qui choisit la notification de la garde (SPEC_ANDROID §12.5). Avant
 * l'alarme, la garde a sa propre notification ; de l'alarme au scan, elle reprend celle de l'alarme
 * (décisions de Mehdi du 2026-10-22).
 */
enum class BlockingGuardPhase {
    /** Session armée, blocage différé pas encore appliqué. */
    BEFORE_BLOCKING,

    /** Blocage en vigueur, du coucher jusqu'à l'alarme. */
    BLOCKING,

    /** `RINGING` : la garde partage la notification de sonnerie. */
    RINGING,

    /**
     * `AWAITING_NFC` ou `TRIGGERED_AWAITING_NFC` (alarme manquée, par exemple) : la garde partage
     * la notification de demande de scan, et la ramène si elle est écartée.
     */
    AWAITING_SCAN,

    /** `RELEASING`, moins d'une seconde après le scan : la garde garde la notification qu'elle tient. */
    RELEASING,
}

/**
 * Ce dont le service de garde a besoin pour sa notification (SPEC_ANDROID §12.5) : la session, la
 * phase, l'instant du réveil et celui du blocage, avec le fuseau de l'activation, pour écrire
 * « Réveil programmé à 7:00 » ou « Réveil prévu à 7:00 » comme l'écran de session, et
 * l'identifiant de session des intents de la sonnerie et du scan.
 */
data class BlockingGuardCommand(
    val sessionId: String,
    val phase: BlockingGuardPhase,
    val wakeAtEpochMillis: Long,
    val blockingStartsAtEpochMillis: Long?,
    val zoneId: String,
) {
    companion object {
        /**
         * Relit une commande depuis les extras d'un `Intent`. Un extra obligatoire manquant rend
         * `null` : le service n'invente rien, il affiche alors un titre sans heure.
         */
        fun fromExtras(
            sessionId: String?,
            phase: String?,
            wakeAtEpochMillis: Long?,
            blockingStartsAtEpochMillis: Long?,
            zoneId: String?,
        ): BlockingGuardCommand? {
            val parsedPhase = BlockingGuardPhase.entries.firstOrNull { it.name == phase }
            val validSession = sessionId?.takeIf { it.isNotBlank() }
            val validZone = zoneId?.takeIf { it.isNotBlank() }
            if (parsedPhase == null || validSession == null || validZone == null) return null
            return wakeAtEpochMillis?.let {
                BlockingGuardCommand(validSession, parsedPhase, it, blockingStartsAtEpochMillis, validZone)
            }
        }
    }
}

sealed interface BlockingGuardDecision {
    /**
     * Rien n'a encore été lu dans ce processus (`SessionSnapshotPublisher` repart à `null`) : ne rien
     * faire. Arrêter ici couperait un service relancé par le système avant la première lecture.
     */
    data object Unknown : BlockingGuardDecision

    data object Release : BlockingGuardDecision

    data class Keep(
        val command: BlockingGuardCommand,
    ) : BlockingGuardDecision
}

/**
 * Le service de garde tourne exactement pendant une session en cours ([isSessionInProgress]), de
 * l'armement à l'état final (SPEC_ANDROID §12.5 ; décision de Mehdi du 2026-10-19, soir).
 *
 * Il protège le processus du nettoyeur de HyperOS. Mesuré le 2026-10-19 sur Xiaomi 25080RABDG /
 * Android 16 : balayer la carte de Niumi dans les récents tuait le processus, et Android ne reliait
 * plus le service d'accessibilité (`A8-RECENTS.md`) ; avec un service de premier plan, le même geste
 * a épargné le processus, comme AOSP le fait explicitement (`killTaskProcessesIfPossible`). Avant
 * le coucher, une session sans ce service mourait de la même façon, et le blocage échouait à
 * l'heure prévue : d'où toute la session, et non la seule phase de blocage.
 */
object BlockingGuardPolicy {
    fun decide(snapshot: SessionSnapshotDto?): BlockingGuardDecision =
        when {
            snapshot == null -> BlockingGuardDecision.Unknown
            snapshot.state.isSessionInProgress() -> BlockingGuardDecision.Keep(commandFor(snapshot))
            else -> BlockingGuardDecision.Release
        }

    /**
     * Service relancé par le système après une mort du processus (`START_STICKY`, `intent` nul) : il
     * relit lui-même la persistance, parce que la réconciliation ne publie rien quand aucune
     * session n'existe plus. Un snapshot illisible rend [BlockingGuardDecision.Unknown] : le
     * service reste tel quel, parce que le blocage est conservé dans ce cas (§18) et que la
     * protection doit l'être avec lui.
     */
    fun afterRestart(load: LoadResult): BlockingGuardDecision =
        when (load) {
            is LoadResult.Present -> decide(load.snapshot)
            LoadResult.Absent -> BlockingGuardDecision.Release
            is LoadResult.Unreadable -> BlockingGuardDecision.Unknown
        }

    private fun commandFor(snapshot: SessionSnapshotDto): BlockingGuardCommand =
        BlockingGuardCommand(
            sessionId = snapshot.sessionId,
            phase = phaseOf(snapshot),
            wakeAtEpochMillis = snapshot.wakeSchedule.triggerAtEpochMillis,
            blockingStartsAtEpochMillis = snapshot.blockingSchedule.startsAtEpochMillis,
            zoneId = snapshot.wakeSchedule.zoneIdAtActivation,
        )

    private fun phaseOf(snapshot: SessionSnapshotDto): BlockingGuardPhase =
        when {
            !snapshot.isBlockingInForce() -> BlockingGuardPhase.BEFORE_BLOCKING
            snapshot.state == SessionStateDto.ARMED -> BlockingGuardPhase.BLOCKING
            snapshot.state == SessionStateDto.RINGING -> BlockingGuardPhase.RINGING
            snapshot.state == SessionStateDto.RELEASING -> BlockingGuardPhase.RELEASING
            else -> BlockingGuardPhase.AWAITING_SCAN
        }
}
