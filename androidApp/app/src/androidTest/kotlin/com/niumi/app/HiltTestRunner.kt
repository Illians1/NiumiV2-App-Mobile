package com.niumi.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Substitue `HiltTestApplication` à `NiumiApplication` pour les tests instrumentés de `:app`
 * (étape 17). Copie des runners de `:feature:ringing` et `:feature:session`.
 *
 * Conséquence à connaître : `NiumiApplication.onCreate` ne s'exécute pas, donc ni le
 * `reconcile(PROCESS_START)` de démarrage ni l'enregistrement du receveur de filtre
 * d'interruption. C'est voulu — `AlarmChainInstrumentedTest` veut observer la chaîne du réveil
 * seule, sans réconciliation concurrente.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
