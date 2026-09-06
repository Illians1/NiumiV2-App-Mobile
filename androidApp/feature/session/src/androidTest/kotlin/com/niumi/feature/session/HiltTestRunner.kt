package com.niumi.feature.session

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * `NiumiBlockingAccessibilityService` est `@AndroidEntryPoint` : les tests instrumentés ont
 * besoin d'une `Application` Hilt pour que le système puisse l'injecter au démarrage. Même
 * motif que `:feature:ringing`.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
