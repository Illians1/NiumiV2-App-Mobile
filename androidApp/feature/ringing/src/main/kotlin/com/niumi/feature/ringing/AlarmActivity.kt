package com.niumi.feature.ringing

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.niumi.database.logging.TechnicalEventLog
import com.niumi.database.logging.TechnicalEventType
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.feature.ringing.ui.AlarmRingingPhase
import com.niumi.feature.ringing.ui.AlarmScreen
import com.niumi.feature.ringing.ui.AlarmScreenState
import com.niumi.system.audio.VibrationController
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
 * l'alarme est le scan NFC (SPEC_ANDROID §11). Aucun bouton d'arrêt.
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

    private val scanCoordinator by lazy { AlarmNfcScanCoordinator(vibrationController, technicalEventLog) }

    private var screenState by
        mutableStateOf(AlarmScreenState.from(phase = AlarmRingingPhase.RINGING, deviceLocked = false))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Retour prédictif : renvoie à l'accueil sans modifier la session
                    // (SPEC_ANDROID §10.4). MainActivity vit dans :app, hors de portée directe
                    // de :feature:ringing : on quitte simplement cette activité, ce qui
                    // découvre l'accueil si Niumi n'a pas d'autre activité au-dessus.
                    finish()
                }
            },
        )

        setContent {
            NiumiTheme {
                val current = screenState
                LaunchedEffect(current.lastScanOutcome) {
                    if (current.lastScanOutcome != null) {
                        delay(TRANSIENT_OUTCOME_DISPLAY_MS)
                        refreshState(lastScanOutcome = null)
                    }
                }
                AlarmScreen(state = current, currentTimeText = currentTimeText(), onOpenNfcSettings = ::openNfcSettings)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState(lastScanOutcome = null)
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
                        // Le titre de l'étape le dit : « arrêt du POC après scan associé ».
                        // Un scan accepté a déjà arrêté le son via RingingController ; rien ne
                        // reste à décider sur cet écran, qui n'a pas de raison de persister.
                        ScanOutcome.Accepted -> finish()

                        null -> Unit

                        else -> refreshState(lastScanOutcome = outcome)
                    }
                }
            },
            onUnreadable = {
                lifecycleScope.launch(Dispatchers.Main.immediate) {
                    refreshState(lastScanOutcome = scanCoordinator.handleUnreadable())
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
        // N'arrête rien : le service continue même si l'activité n'est plus visible.
    }

    override fun onDestroy() {
        super.onDestroy()
        // N'arrête rien : cf. onStop().
    }

    private fun refreshState(lastScanOutcome: ScanOutcome?) {
        screenState =
            AlarmScreenState.from(
                phase = AlarmRingingPhase.RINGING,
                deviceLocked = isDeviceLocked(),
                nfcAvailability = nfcReader.availability,
                lastScanOutcome = lastScanOutcome,
            )
    }

    private fun isDeviceLocked(): Boolean = getSystemService(KeyguardManager::class.java)?.isDeviceLocked == true

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
