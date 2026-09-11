package com.niumi.feature.setup.readiness

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.niumi.system.readiness.ReadinessAction

/**
 * Traduit une [ReadinessAction] en `Intent` de réglages. Les `Intent` sont construits **ici** et
 * jamais dans `:core:system`, qui décrit les recours sans connaître Android côté interface
 * (SPEC_ANDROID §13).
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

        ReadinessAction.OpenFullScreenIntentSettings -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, appUri(packageName))
            } else {
                // Le contrôle est NOT_APPLICABLE sous Android 14 : ce cas n'est pas atteignable
                // depuis l'écran, la branche existe pour ne pas ouvrir un écran inexistant.
                null
            }
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

        // Exemption d'énergie sans demander REQUEST_IGNORE_BATTERY_OPTIMIZATIONS : cette
        // permission est restreinte par Google Play et §13 n'en a pas besoin, l'utilisateur
        // confirmant lui-même. Tant que la liste blanche AOSP manque, on ouvre la liste système ;
        // une fois acquise, le réglage qui commande réellement le gel est celui de la surcouche,
        // sur la fiche de l'application (mesure HyperOS de l'étape 5).
        is ReadinessAction.OpenBatterySettings -> {
            if (action.aospExemptionGranted) {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri(packageName))
            } else {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }
        }

        ReadinessAction.StartPairing,
        ReadinessAction.OpenAppPicker,
        ReadinessAction.RequestNotificationPermission,
        ReadinessAction.ShowExactAlarmDiagnostic,
        ReadinessAction.FixTime,
        ReadinessAction.Unsupported,
        -> {
            null
        }
    }

/** Texte affiché à la place d'un réglage quand il n'existe aucun recours système (§13). */
fun explanationFor(action: ReadinessAction): String? =
    when (action) {
        ReadinessAction.ShowExactAlarmDiagnostic -> ReadinessMessages.EXACT_ALARM_DIAGNOSTIC
        ReadinessAction.Unsupported -> ReadinessMessages.UNSUPPORTED
        else -> null
    }

private fun appUri(packageName: String): Uri = Uri.fromParts("package", packageName, null)
