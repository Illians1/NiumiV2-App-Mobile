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
import androidx.lifecycle.LifecycleOwner
import com.niumi.app.navigation.NavGraphContributor
import com.niumi.app.navigation.NiumiNavHost
import com.niumi.app.navigation.NiumiRoute
import com.niumi.app.navigation.deepLinkDestinationFor
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.intent.NiumiDeepLink
import com.niumi.system.readiness.SessionReadinessWatcher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navGraphContributors: Set<@JvmSuppressWildcards NavGraphContributor>

    @Inject
    lateinit var sessionReadinessWatcher: SessionReadinessWatcher

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
        setContent {
            NiumiTheme {
                NiumiNavHost(
                    contributors = navGraphContributors,
                    deepLinkDestination = deepLinkDestination,
                )
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
