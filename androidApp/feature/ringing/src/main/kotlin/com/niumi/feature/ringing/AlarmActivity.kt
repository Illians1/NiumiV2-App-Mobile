package com.niumi.feature.ringing

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.feature.ringing.ui.AlarmExitDestination
import com.niumi.feature.ringing.ui.AlarmScreen
import com.niumi.feature.ringing.ui.AlarmScreenState
import com.niumi.feature.ringing.ui.AlarmUiState
import com.niumi.system.audio.VibrationController
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.NiumiComponentResolver
import com.niumi.system.intent.NiumiDeepLink
import com.niumi.system.nfc.NfcAvailability
import com.niumi.system.nfc.NfcReader
import com.niumi.system.nfc.NfcScanHandler
import com.niumi.system.nfc.ScanOutcome
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Optional
import javax.inject.Inject

// SPEC_ANDROID §11.2 : un résultat de scan transitoire (illisible, boîtier inconnu) ne doit
// pas rester affiché indéfiniment une fois qu'il n'est plus pertinent.
private const val TRANSIENT_OUTCOME_DISPLAY_MS = 3_000L

/**
 * Écran de réveil plein écran (SPEC_ANDROID §10.4). Ne touche jamais au service, ni dans
 * `onStop()`, ni dans `onDestroy()`, ni via le retour prédictif : la seule façon d'arrêter
 * l'alarme est le scan NFC (§11). Aucun bouton d'arrêt.
 *
 * Depuis l'étape 17, l'état vient de [AlarmViewModel] — donc du moteur — et non plus d'une phase
 * codée en dur. Un état final renvoie vers l'écran 10 ou 11 : l'activité vit dans sa propre tâche
 * (`launchMode="singleTask"`), hors du `NavHost`, et rouvre donc `MainActivity` par un extra de
 * destination plutôt que de naviguer.
 *
 * [scanHandler] est absent avant l'étape 18 en release (`@BindsOptionalOf`, `:core:system`) :
 * seule la route POC (`src/debug` de `:app`) le fournit avant cette étape. Un scan reçu sans
 * handler est silencieusement ignoré (voir [AlarmNfcScanCoordinator]) — il n'existe alors
 * aucune décision à prendre.
 */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {
    @Inject
    lateinit var nfcReader: NfcReader

    @Inject
    lateinit var scanHandler: Optional<NfcScanHandler>

    @Inject
    lateinit var vibrationController: VibrationController

    @Inject
    lateinit var technicalEventLog: TechnicalEventLog

    @Inject
    lateinit var componentResolver: NiumiComponentResolver

    private val viewModel: AlarmViewModel by viewModels()

    private val scanCoordinator by lazy { AlarmNfcScanCoordinator(vibrationController, technicalEventLog) }
    private val keyguardManager by lazy { getSystemService(KeyguardManager::class.java) }

    /**
     * §10.4 : « recalculer son état quand le verrouillage de l'appareil change, et pas seulement
     * dans `onResume()` ». Un écran affiché au-dessus du verrouillage reste visible après le
     * déverrouillage, sans qu'aucun cycle de vie ne soit rejoué.
     *
     * §10.4 laisse le choix entre `ACTION_USER_PRESENT` et
     * `KeyguardManager.addKeyguardLockedStateListener` (API 34+). **Le second est écarté** :
     * il exige `SUBSCRIBE_TO_KEYGUARD_LOCKED_STATE`, absente de la liste figée de §14, et
     * l'appeler sans elle lève une `SecurityException` qui tue le processus — donc la sonnerie —
     * au moment précis où le plein écran ouvre cet écran. Mesuré sur appareil à l'étape 17 :
     * l'alarme sonnait une seconde avant que le processus ne meure. `ACTION_USER_PRESENT` suffit
     * au besoin réel, qui est de faire disparaître le texte de déverrouillage.
     */
    private val userPresentReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) = publishDeviceLockState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // §10.4 : tant que la session attend un scan, le retour est **inerte**. Un
                    // appui distrait pendant que l'alarme sonne ne doit pas écarter le seul écran
                    // qui porte le Reader Mode (§11.2). Ce n'est pas une activité « impossible à
                    // quitter » au sens de §10.4 : Home, le geste de navigation, les récents et le
                    // volet de notifications restent tous disponibles, et rouvrir Niumi ramène
                    // ici. Hors état de scan — écran en cours de chargement, session absente ou
                    // terminée — le retour ferme normalement.
                    if (viewModel.state is AlarmUiState.Visible) return
                    finish()
                }
            },
        )

        setContent {
            NiumiTheme {
                when (val current = viewModel.state) {
                    // Le publisher d'un processus neuf vaut `null` : ne jamais conclure « pas de
                    // session » avant que la persistance ait répondu.
                    AlarmUiState.Loading -> Unit

                    AlarmUiState.Close -> LaunchedEffect(Unit) { finish() }

                    is AlarmUiState.Exit -> LaunchedEffect(current.destination) { leaveFor(current.destination) }

                    is AlarmUiState.Visible -> AlarmContent(current.screen)
                }
            }
        }
    }

    /**
     * Extrait de `setContent` pour que chaque branche du `when` tienne sur une ligne : un corps
     * multiligne y forcerait des accolades, et la branche `Loading` deviendrait une expression
     * inutilisée.
     */
    @Composable
    private fun AlarmContent(screen: AlarmScreenState) {
        LaunchedEffect(screen.lastScanOutcome) {
            if (screen.lastScanOutcome != null) {
                delay(TRANSIENT_OUTCOME_DISPLAY_MS)
                viewModel.onScanOutcome(null)
            }
        }
        AlarmScreen(
            state = screen,
            currentTimeText = currentTimeText(),
            onOpenNfcSettings = ::openNfcSettings,
        )
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(
            userPresentReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            Context.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onResume() {
        super.onResume()
        publishDeviceLockState()
        viewModel.onNfcAvailabilityChanged(nfcReader.availability)
        if (nfcReader.availability == NfcAvailability.DISABLED) {
            technicalEventLog.log(TechnicalEventType.NFC_DISABLED)
        }
        // Invoqués sur le thread binder du Reader Mode (voir ReaderModeNfcReader) : toute
        // lecture ou écriture d'état passe par le dispatcher principal.
        nfcReader.start(
            activity = this,
            onUri = { uri ->
                lifecycleScope.launch(Dispatchers.Main.immediate) {
                    when (val outcome = scanCoordinator.handleUri(scanHandler.orElse(null), uri)) {
                        // Un scan accepté fait avancer la session : l'écran suit le snapshot
                        // publié (RELEASING puis état final) au lieu de se fermer lui-même.
                        ScanOutcome.Accepted, null -> Unit

                        else -> viewModel.onScanOutcome(outcome)
                    }
                }
            },
            onUnreadable = {
                lifecycleScope.launch(Dispatchers.Main.immediate) {
                    viewModel.onScanOutcome(scanCoordinator.handleUnreadable())
                }
            },
        )
    }

    override fun onPause() {
        super.onPause()
        nfcReader.stop(this)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(userPresentReceiver)
        // N'arrête rien : le service continue même si l'activité n'est plus visible.
    }

    override fun onDestroy() {
        super.onDestroy()
        // N'arrête rien : cf. onStop().
    }

    private fun publishDeviceLockState() {
        viewModel.onDeviceLockChanged(keyguardManager?.isDeviceLocked == true)
    }

    /** §15 : l'écran 10 (terminée) ou 11 (annulée) vit dans le `NavHost` de `MainActivity`. */
    private fun leaveFor(destination: AlarmExitDestination) {
        val extra =
            when (destination) {
                AlarmExitDestination.COMPLETED -> NiumiDeepLink.DESTINATION_SESSION_COMPLETED
                AlarmExitDestination.CANCELLED -> NiumiDeepLink.DESTINATION_SESSION_CANCELLED
                AlarmExitDestination.HOME -> null
            }
        startActivity(
            Intent()
                .setComponent(componentResolver.componentName(NiumiComponent.MAIN_ACTIVITY))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .apply { extra?.let { putExtra(NiumiDeepLink.EXTRA_DESTINATION, it) } },
        )
        finish()
    }

    // Pas une action d'arrêt : la sonnerie et le service ne sont pas touchés, seul un accès
    // aux réglages système est ouvert (SPEC_ANDROID §11.2).
    private fun openNfcSettings() {
        startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
    }

    private fun currentTimeText(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(System.currentTimeMillis())

    companion object {
        fun intent(context: Context): Intent = Intent(context, AlarmActivity::class.java)
    }
}
