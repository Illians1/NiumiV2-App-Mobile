package com.niumi.app.poc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.nfc.NfcReader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Association d'un tag NFC pour la route POC (debug uniquement, SPEC_ANDROID §22 Lot 0,
 * §11.1). Le Reader Mode n'est actif que pendant que cette activité est au premier plan.
 * Accessible uniquement en dehors d'une session active (§11.1) : avant la Phase C, aucune
 * session n'existe côté Android, donc aucune garde supplémentaire n'est nécessaire ici.
 */
@AndroidEntryPoint
class PocPairingActivity : ComponentActivity() {
    @Inject
    lateinit var nfcReader: NfcReader

    private val viewModel: PocPairingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NiumiTheme {
                PocPairingScreen(state = viewModel.state, onDone = ::finish)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcReader.start(this, onUri = viewModel::onUriRead, onUnreadable = {})
    }

    override fun onPause() {
        super.onPause()
        nfcReader.stop(this)
    }
}
