package com.niumi.feature.ringing

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.feature.ringing.ui.AlarmRingingPhase
import com.niumi.feature.ringing.ui.AlarmScreen
import com.niumi.feature.ringing.ui.AlarmScreenState
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Écran de réveil plein écran (SPEC_ANDROID §10.4). Ne touche jamais au service, ni dans
 * `onStop()`, ni dans `onDestroy()`, ni via le retour prédictif : la seule façon d'arrêter
 * l'alarme est le scan NFC, câblé à l'étape 4. Aucun bouton d'arrêt.
 */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {
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
                val keyguardManager = getSystemService(KeyguardManager::class.java)
                val deviceLocked = keyguardManager?.isDeviceLocked == true
                val state =
                    AlarmScreenState.from(phase = AlarmRingingPhase.RINGING, deviceLocked = deviceLocked)
                AlarmScreen(state = state, currentTimeText = currentTimeText())
            }
        }
    }

    // Le Reader Mode NFC démarre ici à l'étape 4 (onResume) et s'arrête dans onPause.
    // Volontairement absent à cette étape : SPEC_ANDROID §4 place le NFC hors périmètre de
    // l'étape 3.

    override fun onStop() {
        super.onStop()
        // N'arrête rien : le service continue même si l'activité n'est plus visible.
    }

    override fun onDestroy() {
        super.onDestroy()
        // N'arrête rien : cf. onStop().
    }

    private fun currentTimeText(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(System.currentTimeMillis())

    companion object {
        fun intent(context: Context): Intent = Intent(context, AlarmActivity::class.java)
    }
}
