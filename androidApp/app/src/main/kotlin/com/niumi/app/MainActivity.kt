package com.niumi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.niumi.app.navigation.NiumiNavHost
import com.niumi.app.navigation.NiumiRoute
import com.niumi.app.navigation.deepLinkDestinationFor
import com.niumi.app.navigation.requiresAlarmScreen
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.feature.ringing.AlarmActivity
import com.niumi.system.intent.NiumiDeepLink
import com.niumi.system.readiness.SessionReadinessWatcher
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sessionReadinessWatcher: SessionReadinessWatcher

    @Inject
    lateinit var sessionSnapshotPublisher: SessionSnapshotPublisher

    /**
     * Destination demandée par l'`Intent` qui a ouvert l'activité (SPEC_ANDROID §13.1). Un `State`
     * plutôt qu'une valeur lue une fois : un second avertissement tapé alors que Niumi est déjà au
     * premier plan arrive par [onNewIntent], pas par `onCreate`.
     *
     * Ce chemin **exige `android:launchMode="singleTop"`** au manifeste. Mesuré sur appareil à
     * l'étape 16 : en `launchMode` standard, Android ramène simplement la tâche au premier plan
     * sans jamais appeler [onNewIntent], et le tap restait sans effet.
     */
    private var deepLinkDestination by mutableStateOf<NiumiRoute?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkDestination = destinationOf(intent)
        // SPEC_ANDROID §13.1, « à chaque passage de l'application au premier plan ». L'évaluation
        // est lancée sans être attendue : elle traverse le verrou du coordinateur et ne doit pas
        // retarder l'affichage. Elle ne fait rien si aucune session n'est armée.
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    sessionReadinessWatcher.evaluateAsync()
                }
            },
        )
        openAlarmScreenWhileSessionAwaitsScan()
        setContent {
            NiumiTheme {
                NiumiNavHost(deepLinkDestination = deepLinkDestination)
            }
        }
    }

    /**
     * §10.4 : tant que la session attend un scan, aucun autre écran de Niumi n'est atteignable.
     *
     * Porté par l'activité et **observé en continu**, pas seulement à `onResume`. Deux mesures sur
     * appareil à l'étape 17 ont conduit ici :
     *
     * - une redirection posée sur l'accueil ne s'exécutait jamais, le `NavHost` étant sur
     *   `ActiveSession` après l'armement ;
     * - une redirection posée sur le seul `onResume` ne se déclenchait pas quand l'alarme sonnait
     *   **pendant** que l'utilisateur était déjà dans Niumi : `onResume` ne se rejoue pas, et le
     *   plein écran de la notification n'est honoré par Android que si l'appareil est verrouillé
     *   ou l'écran éteint.
     *
     * `repeatOnLifecycle(RESUMED)` suspend la collecte dès que l'écran de réveil passe devant :
     * aucune relance en boucle. `distinctUntilChanged` n'agit qu'aux transitions, pour qu'une
     * simple révision de snapshot ne relance pas l'activité.
     */
    private fun openAlarmScreenWhileSessionAwaitsScan() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sessionSnapshotPublisher.snapshot
                    .map { requiresAlarmScreen(it?.state) }
                    .distinctUntilChanged()
                    .collect { required ->
                        if (required) startActivity(AlarmActivity.intent(this@MainActivity))
                    }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkDestination = destinationOf(intent)
    }

    /** §16 : tout extra reçu est validé — ici, une destination inconnue vaut « aucune ». */
    private fun destinationOf(intent: Intent?): NiumiRoute? =
        deepLinkDestinationFor(intent?.getStringExtra(NiumiDeepLink.EXTRA_DESTINATION))
}
