package com.niumi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.niumi.app.navigation.NavGraphContributor
import com.niumi.app.navigation.NiumiNavHost
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.readiness.SessionReadinessWatcher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navGraphContributors: Set<@JvmSuppressWildcards NavGraphContributor>

    @Inject
    lateinit var sessionReadinessWatcher: SessionReadinessWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                NiumiNavHost(contributors = navGraphContributors)
            }
        }
    }
}
