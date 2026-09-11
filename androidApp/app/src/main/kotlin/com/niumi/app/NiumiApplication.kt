package com.niumi.app

import android.app.Application
import com.niumi.system.notification.AndroidNotificationChannelRegistrar
import com.niumi.system.readiness.SessionReadinessWatcher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NiumiApplication : Application() {
    @Inject
    lateinit var notificationChannelRegistrar: AndroidNotificationChannelRegistrar

    @Inject
    lateinit var sessionReadinessWatcher: SessionReadinessWatcher

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
    }
}
