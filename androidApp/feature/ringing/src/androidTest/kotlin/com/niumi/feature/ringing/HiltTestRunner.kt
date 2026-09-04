package com.niumi.feature.ringing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * `AlarmReceiver` et `AlarmRingingService` sont `@AndroidEntryPoint` : les tests instrumentés
 * ont besoin d'une `Application` Hilt pour que le système puisse les injecter au démarrage,
 * même sans `@HiltAndroidTest` sur la classe de test elle-même (aucune injection dans le test).
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
