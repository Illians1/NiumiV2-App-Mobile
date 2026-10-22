package com.niumi.system.readiness

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Traduit une [ReadinessAction] en `Intent` de réglages (SPEC_ANDROID §13).
 *
 * Vit dans `:core:system`, à côté du type qu'elle traduit, depuis l'étape 16. Elle était dans
 * `:feature:setup` tant que l'écran 2 en était le seul consommateur ; l'écran 7 en a désormais
 * besoin pour la remédiation des incidents (§15), et `:feature:session` ne peut pas dépendre de
 * `:feature:setup` (§6 n'autorise que `feature:* → core:*`). La règle de §13 est inchangée sur le
 * fond : **`DeviceReadinessChecker` ne construit aucun `Intent`**, il ne fait que décrire le
 * recours — c'est cette fonction, appelée par un écran, qui le traduit. Les **textes** affichés
 * sont dans `ReadinessMessages`, à côté, depuis l'étape 36 de la refonte.
 *
 * `null` signifie « pas de réglage à ouvrir » : l'association et le sélecteur sont des
 * destinations internes, la permission de notification passe par un lanceur de permission, et
 * `ShowExactAlarmDiagnostic` comme `Unsupported` n'affichent qu'un texte.
 *
 * **Aucune branche ne produit `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.** Niumi déclare
 * `USE_EXACT_ALARM` et jamais `SCHEDULE_EXACT_ALARM` : présenter l'accès aux alarmes exactes
 * comme une permission utilisateur ordinaire contredirait §13 et §14.
 */
fun settingsIntentFor(
    action: ReadinessAction,
    packageName: String,
): Intent? =
    when (action) {
        ReadinessAction.OpenNfcSettings -> {
            Intent(Settings.ACTION_NFC_SETTINGS)
        }

        is ReadinessAction.OpenChannelSettings -> {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, action.channelId)
        }

        ReadinessAction.OpenSoundSettings -> {
            Intent(Settings.ACTION_SOUND_SETTINGS)
        }

        // Aucune action publique n'ouvre l'interrupteur « Ne pas déranger » lui-même ;
        // ACTION_ZEN_MODE_PRIORITY_SETTINGS est la plus proche que le SDK expose. Niumi ne
        // demande pas ACCESS_NOTIFICATION_POLICY et ne modifie jamais le mode (§13) : l'écran
        // amène l'utilisateur devant le réglage, il décide seul.
        ReadinessAction.OpenDndSettings -> {
            Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
        }

        ReadinessAction.OpenAccessibilitySettings -> {
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }

        // Exemption d'énergie demandée directement (décision de Mehdi du 2026-10-21) : une fenêtre
        // système, un toucher. La liste système en demandait trois sur Android 16 (liste, fiche de
        // l'app, « Sans restriction »). Les deux inscrivent Niumi dans la même liste blanche, celle
        // qui empêche le gel sur HyperOS et que le diagnostic relit au retour (mesuré le
        // 2026-09-28). Exige `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, déclarée (§14).
        ReadinessAction.OpenBatterySettings -> {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.fromParts("package", packageName, null))
        }

        ReadinessAction.StartPairing,
        ReadinessAction.OpenAppPicker,
        ReadinessAction.RequestNotificationPermission,
        ReadinessAction.ShowExactAlarmDiagnostic,
        ReadinessAction.FixTime,
        ReadinessAction.LockInRecents,
        ReadinessAction.Unsupported,
        -> {
            null
        }
    }

/**
 * Recours de repli quand l'`Intent` de [settingsIntentFor] n'a pas d'activité pour le recevoir :
 * un constructeur peut retirer la fenêtre de demande d'exemption. La liste système des
 * optimisations reste alors le chemin, plus long mais existant partout.
 */
fun fallbackSettingsIntentFor(action: ReadinessAction): Intent? =
    when (action) {
        ReadinessAction.OpenBatterySettings -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        else -> null
    }

/**
 * Ouvre le recours de [action], puis son repli si le premier n'a pas d'activité. Rend `false`
 * quand rien n'a pu s'ouvrir : l'écran ne promet alors rien de plus (SPEC_ANDROID §15). Appelé
 * depuis une activité ; aucun réglage n'est modifié à la place de l'utilisateur (§12.3).
 */
fun Context.openRecourse(action: ReadinessAction): Boolean =
    listOfNotNull(settingsIntentFor(action, packageName), fallbackSettingsIntentFor(action)).any { intent ->
        try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
