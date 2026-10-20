package com.niumi.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.niumi.app.navigation.NiumiNavHost
import com.niumi.app.navigation.NiumiRoute
import com.niumi.app.navigation.deepLinkDestinationFor
import com.niumi.app.navigation.requiresAlarmScreen
import com.niumi.app.theme.SessionThemeViewModel
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.designsystem.ui.theme.applyNiumiWindowTheme
import com.niumi.feature.ringing.AlarmActivity
import com.niumi.system.blocking.guard.BlockingGuardWatcher
import com.niumi.system.intent.NiumiDeepLink
import com.niumi.system.readiness.SessionReadinessWatcher
import com.niumi.system.session.SessionSnapshotPublisher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sessionReadinessWatcher: SessionReadinessWatcher

    @Inject
    lateinit var sessionSnapshotPublisher: SessionSnapshotPublisher

    @Inject
    lateinit var blockingGuardWatcher: BlockingGuardWatcher

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

    /**
     * Thème clair ou sombre d'après la session persistée, jamais d'après le réglage du téléphone
     * (étape 33, `isSessionDark`). Inconnu (`null`) le temps de la première lecture.
     */
    private val themeViewModel: SessionThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fenêtre sombre, comme l'écran de démarrage (`Theme.Niumi.Launch`), le temps de la
        // première lecture ; puis celle du thème de la session à chaque changement.
        applyNiumiWindowTheme(dark = true)
        followSessionThemeWithWindow()
        deepLinkDestination = destinationOf(intent)
        // SPEC_ANDROID §13.1, « à chaque passage de l'application au premier plan ». L'évaluation
        // est lancée sans être attendue : elle traverse le verrou du coordinateur et ne doit pas
        // retarder l'affichage. Elle ne fait rien si aucune session n'est armée.
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    sessionReadinessWatcher.evaluateAsync()
                    // SPEC_ANDROID §12.5 : Android autorise toujours le démarrage d'un service de
                    // premier plan depuis une activité visible ; un démarrage refusé en arrière-plan
                    // est rejoué ici.
                    blockingGuardWatcher.retryRefused()
                }
            },
        )
        openAlarmScreenWhileSessionAwaitsScan()
        setContent {
            val dark by themeViewModel.dark.collectAsStateWithLifecycle()
            // Rien n'est composé avant que la persistance ait dit si une session est en cours :
            // l'écran de démarrage sombre reste affiché (voir keepLaunchScreenUntilThemeIsKnown).
            dark?.let { sessionDark ->
                NiumiTheme(dark = sessionDark) {
                    NiumiNavHost(deepLinkDestination = deepLinkDestination)
                }
            }
        }
        keepLaunchScreenUntilThemeIsKnown()
    }

    /**
     * Décision de Mehdi du 2026-10-19 : aucun fond clair ne précède un accueil sombre. L'écran de
     * démarrage (sombre, avec le logo sur Android 12 et plus) disparaît à la première image de
     * l'activité ; cette image est donc retenue tant que le thème de la session est inconnu.
     * Méthode officielle sans bibliothèque (`OnPreDrawListener` sur le contenu). Aucun minimum :
     * l'app s'ouvre dès que la session est lue ; au plus trois secondes, borne portée par
     * `SessionThemeSource`. Après une rotation, le ViewModel connaît déjà le thème : rien n'est
     * retenu.
     */
    private fun keepLaunchScreenUntilThemeIsKnown() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val known = themeViewModel.dark.value != null
                    if (known) content.viewTreeObserver.removeOnPreDrawListener(this)
                    return known
                }
            },
        )
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
     *   **pendant** que l'utilisateur était déjà dans Niumi : `onResume` ne se rejoue pas, et
     *   aucune notification n'ouvre l'écran de réveil sans un tap (sans plein écran depuis le
     *   Lot 9).
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

    /** Barres système et fond de fenêtre accordés au thème de la session (`NiumiWindowTheme`). */
    private fun followSessionThemeWithWindow() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                themeViewModel.dark
                    .filterNotNull()
                    .collect { dark -> applyNiumiWindowTheme(dark) }
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
