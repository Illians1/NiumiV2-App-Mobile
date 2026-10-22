package com.niumi.feature.setup.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niumi.designsystem.effect.WindowFocusRegainedEffect
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.readiness.openRecourse

/**
 * Point d'entrée réel de l'écran 2. Les tuiles sont relues à chaque `ON_RESUME` et à chaque retour
 * du focus de la fenêtre (le volet rapide ne met pas l'écran en pause, SPEC_ANDROID §13, écart 10).
 * Aucun réglage n'est ouvert ni modifié à la place de l'utilisateur (§12.3).
 *
 * Notifications : « Autoriser » ouvre la fenêtre système tant qu'Android peut la montrer, les
 * réglages de notification de Niumi sinon ([notificationRequestFor], décidé avant le lancement) ;
 * un refus ne fait rien de particulier (`SPECIFICATION.md` §5).
 */
@Composable
fun PermissionsRoute(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Le résultat de la fenêtre ne fait jamais foi pour la tuile : c'est la relecture qui décide.
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    WindowFocusRegainedEffect { viewModel.refresh() }

    PermissionsScreen(
        state = viewModel.state,
        actions =
            PermissionsActions(
                onBack = onBack,
                onAuthorize = { permission ->
                    when (val step = viewModel.authorize(permission)) {
                        PermissionStep.RequestNotifications -> {
                            val request =
                                notificationRequestFor(
                                    sdkInt = Build.VERSION.SDK_INT,
                                    requestedBefore = viewModel.state.notificationRequestedBefore,
                                    showRationale = context.findActivity()?.showsNotificationRationale() ?: false,
                                )
                            if (request == NotificationRequest.SYSTEM_DIALOG) {
                                viewModel.markNotificationPermissionRequested()
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.startActivity(appNotificationSettings(context.packageName))
                            }
                        }

                        is PermissionStep.OpenSettings -> {
                            context.openRecourse(step.action)
                        }

                        // Fenêtre ouverte par l'état, ou tuile déjà accordée : rien à lancer d'ici.
                        PermissionStep.None -> {}
                    }
                },
                onContinue = onContinue,
                onDismissDialog = viewModel::dismissDialog,
                // §12.3 : seul ce bouton ouvre les réglages d'accessibilité, après la divulgation.
                onOpenAccessibilitySettings = { context.openRecourse(ReadinessAction.OpenAccessibilitySettings) },
                onConfirmRecentsLock = viewModel::confirmRecentsLock,
            ),
    )
}

/** Réglages de notification de Niumi : avant Android 13, ou quand la fenêtre système ne s'ouvre plus. */
private fun appNotificationSettings(packageName: String): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

private fun Activity.showsNotificationRationale(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

/** `LocalContext` est un `ContextThemeWrapper` dans une `ComponentActivity` : déplier jusqu'à elle. */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
