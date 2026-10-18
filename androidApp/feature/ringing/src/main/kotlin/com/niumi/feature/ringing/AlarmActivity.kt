package com.niumi.feature.ringing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.niumi.system.nfc.nfcAdapterSettledChanges
import com.niumi.system.readiness.ForegroundReadinessTrigger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

// SPEC_ANDROID §11.2 : un résultat de scan transitoire (illisible, boîtier inconnu) ne doit
// pas rester affiché indéfiniment une fois qu'il n'est plus pertinent.
private const val TRANSIENT_OUTCOME_DISPLAY_MS = 3_000L

/**
 * Écran de réveil (SPEC_ANDROID §10.4). Ne touche jamais au service, ni dans
 * `onStop()`, ni dans `onDestroy()`, ni via le retour prédictif : la seule façon d'arrêter
 * l'alarme est le scan NFC (§11). Aucun bouton d'arrêt.
 *
 * Depuis l'étape 17, l'état vient de [AlarmViewModel] — donc du moteur — et non plus d'une phase
 * codée en dur. Un état final renvoie vers l'écran 10 ou 11 : l'activité vit dans sa propre tâche
 * (`launchMode="singleTask"`), hors du `NavHost`, et rouvre donc `MainActivity` par un extra de
 * destination plutôt que de naviguer.
 *
 * [scanHandler] est `HandleValidNfcUseCase` (`:core:system`) depuis l'étape 18, liaison unique et
 * non optionnelle : c'est lui qui termine la session, et l'écran ne fait que suivre le snapshot
 * qu'il produit.
 *
 * **Jamais affiché par-dessus le verrouillage (Lot 9).** Ni affichage sur l'écran verrouillé, ni
 * allumage de l'écran : le scan ne fonctionne pas tant que le téléphone est verrouillé (§4.4), et
 * l'écran forcé gênait le déverrouillage. Il s'ouvre par le tap de la notification de sonnerie ou par l'icône de Niumi,
 * une fois le téléphone déverrouillé ; d'où l'absence de tout texte ou receveur de déverrouillage.
 */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {
    @Inject
    lateinit var nfcReader: NfcReader

    @Inject
    lateinit var scanHandler: NfcScanHandler

    @Inject
    lateinit var vibrationController: VibrationController

    @Inject
    lateinit var technicalEventLog: TechnicalEventLog

    @Inject
    lateinit var componentResolver: NiumiComponentResolver

    /**
     * §10.5, décision de l'étape 19. Dans un état de scan, l'utilisateur qui rouvre Niumi atterrit
     * **ici** et non sur `MainActivity` : le task est ramené au premier plan avec cet écran au
     * sommet (`launchMode="singleTask"`), et `MainActivity.onResume` n'est jamais rejoué — mesuré
     * sur appareil le 2026-09-14. Le déclencheur doit donc être posé sur les deux écrans, sans quoi
     * la notification d'attente de scan balayée ne revient qu'au prochain démarrage de processus.
     */
    @Inject
    lateinit var foregroundReadinessTrigger: ForegroundReadinessTrigger

    private val viewModel: AlarmViewModel by viewModels()

    private val scanCoordinator by lazy { AlarmNfcScanCoordinator(vibrationController, technicalEventLog) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

        // §11.2 : au premier plan seulement — le Reader Mode n'y vit que là, et `onResume` relit
        // de toute façon au retour. Le volet rapide ne met pas l'activité en pause.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                nfcAdapterSettledChanges().collect { onAdapterSettled() }
            }
        }

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
            onOpenNfcSettings = { openNfcSettings() },
        )
    }

    override fun onResume() {
        super.onResume()
        foregroundReadinessTrigger.evaluateAsync()
        viewModel.onNfcAvailabilityChanged(nfcReader.availability)
        if (nfcReader.availability == NfcAvailability.DISABLED) {
            technicalEventLog.log(TechnicalEventType.NFC_DISABLED)
        }
        startReader()
    }

    /**
     * Relecture sur un état stable de l'adaptateur (SPEC_ANDROID §11.2, étape 31). Mesuré à
     * l'étape 30 (essai manuel 2) : après un redémarrage, la pile NFC revient environ 13 s après le
     * déverrouillage, et l'écran lu une seule fois au `ON_RESUME` gardait « Le NFC est désactivé… »
     * jusqu'à un nouvel essai de l'utilisateur. Le lecteur est relancé avec la disponibilité : un
     * message effacé sur un écran qui n'écoute pas serait un faux état pire que le message. Aucun
     * journal ici, ni fenêtre de grâce : un NFC réellement coupé reste affiché coupé.
     */
    private fun onAdapterSettled() {
        viewModel.onNfcAvailabilityChanged(nfcReader.availability)
        if (nfcReader.availability == NfcAvailability.ENABLED) startReader()
    }

    /**
     * Appelé à chaque `onResume` et à chaque état stable « activé ». Android tient une seule
     * inscription Reader Mode par activité : un second appel la remplace au lieu d'en empiler une
     * autre, d'où l'absence de garde depuis [onAdapterSettled] (à confirmer sur appareil, ligne §20
     * de l'étape 31).
     */
    private fun startReader() {
        // Invoqués sur le thread binder du Reader Mode (voir ReaderModeNfcReader) : toute
        // lecture ou écriture d'état passe par le dispatcher principal.
        nfcReader.start(
            activity = this,
            onUri = { uri ->
                lifecycleScope.launch(Dispatchers.Main.immediate) {
                    when (val outcome = scanCoordinator.handleUri(scanHandler, uri)) {
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
        // N'arrête rien : le service continue même si l'activité n'est plus visible.
    }

    override fun onDestroy() {
        super.onDestroy()
        // N'arrête rien : cf. onStop().
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

    companion object {
        fun intent(context: Context): Intent = Intent(context, AlarmActivity::class.java)
    }
}

// Hors de la classe, déjà au plafond `TooManyFunctions` de detekt (11) depuis la relecture NFC de
// l'étape 31 : ni l'une ni l'autre ne lit l'état de l'activité.

// Pas une action d'arrêt : la sonnerie et le service ne sont pas touchés, seul un accès
// aux réglages système est ouvert (SPEC_ANDROID §11.2).
private fun Activity.openNfcSettings() {
    startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
}

private fun currentTimeText(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(System.currentTimeMillis())
