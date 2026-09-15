package com.niumi.system.session

import com.niumi.core.interop.SessionStateDto

/**
 * Les deux écarts entre l'état métier et les sous-systèmes Android que [SessionRuntimeReconciler]
 * traite, distincts des six que `SessionReadinessMonitor` surveille déjà (SPEC_ANDROID §13.1).
 */
enum class RuntimeGap {
    /** Une alarme métier `ARMED` sans `PendingIntent` réel (§13, `EXACT_ALARM`, ne teste que la
     * permission, jamais l'existence effective de l'alarme). */
    ALARM_NOT_SCHEDULED,

    /** NFC coupé alors que le scan reste la seule sortie de session (§11.2). */
    NFC_DISABLED,
}

/**
 * Calcule [RuntimeGap] pour un [SessionRuntimeStatus] et un état donnés (SPEC_ANDROID §7.1, §18 ;
 * étape 20). Objet pur, sans dépendance Android : la décision d'un écart est séparée de sa
 * réparation ([SessionRuntimeReconciler]).
 */
object RuntimeStatusGaps {
    /**
     * Les cinq états où la session tient encore une obligation vis-à-vis du scan (SPEC_ANDROID
     * §11.2, §20 « NFC désactivé pendant la sonnerie »). `PREPARING` en est exclu : une activation
     * interrompue n'a jamais promis de scan à l'utilisateur.
     */
    val NFC_MONITORED_STATES: Set<SessionStateDto> =
        SessionStateDto.entries.toSet() - SESSION_FINAL_STATES - SessionStateDto.PREPARING

    fun of(
        status: SessionRuntimeStatus,
        state: SessionStateDto,
    ): Set<RuntimeGap> {
        val gaps = mutableSetOf<RuntimeGap>()
        if (state == SessionStateDto.ARMED && !status.alarmScheduled) gaps += RuntimeGap.ALARM_NOT_SCHEDULED
        if (state in NFC_MONITORED_STATES && !status.nfcReady) gaps += RuntimeGap.NFC_DISABLED
        return gaps
    }
}
