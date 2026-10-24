package com.niumi.system.notification

import android.app.Notification
import android.app.PendingIntent
import com.niumi.system.intent.IntentExtraValue
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.NiumiDeepLink
import com.niumi.system.intent.PendingIntentSpec
import com.niumi.system.readiness.ReadinessCheckId

/**
 * Textes des avertissements de SPEC_ANDROID §13.1 : chacun nomme le réglage en cause **et** sa
 * conséquence, pour que l'utilisateur sache ce qu'il perd s'il ne fait rien. Aucun n'est
 * `ongoing` : l'utilisateur doit pouvoir les balayer, contrairement à la notification de
 * sonnerie.
 *
 * `CATEGORY_ERROR` plutôt que `CATEGORY_ALARM` : ces notifications signalent un état de
 * permission ou de réglage dégradé, jamais une alarme en cours — les confondre ferait croire
 * que le réveil sonne.
 */
object SessionWarningNotificationSpecs {
    private const val TITLE = "Vérifie ton réveil Niumi"

    private val texts: Map<ReadinessCheckId, String> =
        mapOf(
            ReadinessCheckId.EXACT_ALARM to
                "L'accès aux alarmes exactes a été perdu. Ton réveil ne peut plus être programmé à l'heure.",
            ReadinessCheckId.NOTIFICATIONS to
                "Les notifications de Niumi sont désactivées. Au réveil, tu devras ouvrir Niumi " +
                "toi-même pour scanner ton Niumi Spot.",
            ReadinessCheckId.ALARM_VOLUME to
                "Le volume des alarmes est à zéro. Ton réveil ne sera pas audible.",
            ReadinessCheckId.DND_TOTAL_SILENCE to
                "Ton réveil ne sonnera pas tant que le silence total est activé.",
            ReadinessCheckId.ACCESSIBILITY_SERVICE to
                "Le service d'accessibilité de Niumi est désactivé. Tes applications ne sont plus bloquées.",
            ReadinessCheckId.BATTERY_OPTIMIZATION to
                "Les restrictions de batterie sont revenues sur Niumi. Le blocage peut s'arrêter tout seul.",
        )

    fun forCheck(id: ReadinessCheckId): NotificationSpec =
        NotificationSpec(
            channelId = NiumiNotificationChannels.sessionWarning.id,
            category = Notification.CATEGORY_ERROR,
            title = TITLE,
            text = requireNotNull(texts[id]) { "Contrôle non surveillé par SPEC_ANDROID §13.1 : $id" },
            ongoing = false,
            actions = emptyList(),
        )

    /**
     * Un identifiant par contrôle : deux réglages cassés en même temps produisent deux
     * avertissements distincts, aucun n'écrase l'autre. 1 est pris par la sonnerie, 2 par la
     * demande de scan.
     *
     * **Table figée (Lot 9)**, et non plus l'index dans `MonitoredReadinessChecks` : retirer le
     * contrôle du plein écran (4) aurait décalé les suivants, et un avertissement publié par une
     * version antérieure n'aurait plus jamais été retiré. Un identifiant ne se réattribue pas.
     */
    private val notificationIds: Map<ReadinessCheckId, Int> =
        mapOf(
            ReadinessCheckId.EXACT_ALARM to 3,
            ReadinessCheckId.NOTIFICATIONS to 5,
            ReadinessCheckId.ALARM_VOLUME to 6,
            ReadinessCheckId.DND_TOTAL_SILENCE to 7,
            ReadinessCheckId.ACCESSIBILITY_SERVICE to 8,
            ReadinessCheckId.BATTERY_OPTIMIZATION to 9,
        )

    /** 4 : l'ancien avertissement « plein écran », retiré avec son contrôle au Lot 9. */
    val retiredNotificationIds: Set<Int> = setOf(4)

    fun notificationId(id: ReadinessCheckId): Int =
        requireNotNull(notificationIds[id]) { "Contrôle non surveillé par SPEC_ANDROID §13.1 : $id" }

    /**
     * Ce que `clearAll()` annule : les avertissements des contrôles surveillés **et** les
     * identifiants retirés, pour qu'un avertissement publié avant la mise à jour ne reste pas
     * affiché pour toujours.
     */
    fun idsToClear(): Set<Int> = (notificationIds.values + retiredNotificationIds).toSortedSet()

    /**
     * §13.1 : « Le tap de la notification ouvre `MainActivity`, qui redirige vers le diagnostic
     * d'incident. » L'écran 12 existe depuis l'étape 16 : l'extra porte désormais la destination,
     * là où le tap se contentait de ramener dans l'application.
     *
     * Un code de requête par contrôle : deux avertissements simultanés ne doivent pas partager
     * l'identité d'un `PendingIntent` (même raison qu'à l'étape 11). `FLAG_IMMUTABLE` — §16.
     */
    fun tap(id: ReadinessCheckId): PendingIntentSpec =
        PendingIntentSpec(
            kind = PendingIntentSpec.Kind.ACTIVITY,
            target = NiumiComponent.MAIN_ACTIVITY,
            requestCode = notificationId(id),
            flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            extras =
                mapOf(
                    NiumiDeepLink.EXTRA_DESTINATION to
                        IntentExtraValue.Text(NiumiDeepLink.DESTINATION_INCIDENT_DIAGNOSTIC),
                ),
        )
}
