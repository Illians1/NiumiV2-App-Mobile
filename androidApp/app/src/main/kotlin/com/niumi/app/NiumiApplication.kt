package com.niumi.app

import android.app.Application
import com.niumi.system.notification.AndroidNotificationChannelRegistrar
import com.niumi.system.readiness.SessionReadinessWatcher
import com.niumi.system.session.SessionStartupReconciler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NiumiApplication : Application() {
    @Inject
    lateinit var notificationChannelRegistrar: AndroidNotificationChannelRegistrar

    @Inject
    lateinit var sessionReadinessWatcher: SessionReadinessWatcher

    @Inject
    lateinit var sessionStartupReconciler: SessionStartupReconciler

    override fun onCreate() {
        super.onCreate()
        // Les canaux doivent exister avant toute notification : poster sur un canal inconnu
        // fait rejeter la notification par le système (et, pour un service de premier plan,
        // tue le service). Les créer ici les rend aussi visibles dans les réglages Android
        // avant même la première session. `createNotificationChannel` est idempotent.
        notificationChannelRegistrar.registerAll()
        // SPEC_ANDROID §13.1 : seul changement de réglage surveillé qu'Android diffuse
        // publiquement. Le receveur est enregistré à chaud et meurt avec le processus, ce que
        // §13.1 assume explicitement — aucune surveillance continue n'est promise.
        sessionReadinessWatcher.registerInterruptionFilterReceiver(this)
        // SPEC_ANDROID §9.2 (dernier alinéa) et §13.1 : au démarrage du processus, la
        // réconciliation reprend toute transaction restée incomplète et republie la session
        // persistée vers l'interface — `SessionSnapshotPublisher` vit en mémoire et repart à
        // `null` à chaque lancement. Ici plutôt que dans `MainActivity` : le processus peut être
        // réveillé sans interface, par le receveur d'alarme notamment.
        sessionStartupReconciler.reconcileAsync()
    }
}
