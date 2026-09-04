package com.niumi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.niumi.app.navigation.NavGraphContributor
import com.niumi.app.ui.NiumiNavHost
import com.niumi.designsystem.ui.theme.NiumiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navGraphContributors: Set<@JvmSuppressWildcards NavGraphContributor>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NiumiTheme {
                NiumiNavHost(contributors = navGraphContributors)
            }
        }
    }
}
