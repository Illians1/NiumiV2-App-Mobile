package com.niumi.system.session

import com.niumi.system.alarm.AlarmScheduler
import com.niumi.system.audio.AlarmVolumeSource
import com.niumi.system.blocking.AccessibilityServiceStatus
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader
import com.niumi.system.notification.NotificationAvailability
import com.niumi.system.readiness.UnlockSettling

/** Construit un [SessionRuntimeStatus] pour une session donnée. */
fun interface SessionRuntimeStatusProbe {
    fun probe(sessionId: String): SessionRuntimeStatus
}

class DefaultSessionRuntimeStatusProbe(
    private val alarmScheduler: AlarmScheduler,
    private val accessibilityServiceStatus: AccessibilityServiceStatus,
    private val notificationAvailability: NotificationAvailability,
    private val nfcReader: NfcReader,
    private val alarmVolumeSource: AlarmVolumeSource,
    private val nfcEvaluability: SessionNfcEvaluability,
) : SessionRuntimeStatusProbe {
    override fun probe(sessionId: String): SessionRuntimeStatus =
        SessionRuntimeStatus(
            alarmScheduled = alarmScheduler.isScheduled(sessionId),
            accessibilityReady = accessibilityServiceStatus.isEnabled(),
            notificationReady = notificationAvailability.areNotificationsEnabled(),
            fullScreenReady = notificationAvailability.canUseFullScreenIntent(),
            nfcReady = nfcReader.availability == NfcAvailability.ENABLED,
            audioReady = alarmVolumeSource.alarmStreamVolume() > 0,
            nfcEvaluable = nfcEvaluability.isEvaluable(),
        )
}

/**
 * Quand le NFC peut-il être jugé ? (SPEC_ANDROID §13.1, §18 ; étape 25.)
 *
 * **Défaut mesuré le 2026-09-24** sur Xiaomi 25080RABDG / Android 16 : au démarrage, le NFC se lit
 * « désactivé » alors qu'il est allumé. Deux passes de réconciliation tournaient pendant la fenêtre
 * verrouillée, chacune consignait un `NFC_DISABLED` `CRITICAL` — le dédoublonnage relit des
 * incidents illisibles avant déverrouillage —, et la session restait `DEGRADED` jusqu'à sa fin.
 *
 * Même réponse qu'à l'étape 19 pour l'accessibilité (point de vigilance 11) : ce qu'un contrôle ne
 * peut pas dire honnêtement, il ne le dit pas. Le NFC n'est jugé qu'une fois **la fenêtre qui suit
 * le déverrouillage refermée** ([UnlockSettling]) : le 27/09 à 19:38, le service NFC n'a démarré
 * qu'**après** le déverrouillage et ne s'est stabilisé que 13,2 s plus tard. Une première version
 * comptait 30 s depuis le démarrage : elle avait expiré avant même le déverrouillage, et un faux
 * `NFC_DISABLED` était consigné 1,8 s après lui. Le re-contrôle de fin de fenêtre
 * (`ReadinessRecheck`) rejoue ensuite ce jugement.
 */
class SessionNfcEvaluability(
    private val settling: UnlockSettling,
) {
    fun isEvaluable(): Boolean = settling.isSettled()
}
