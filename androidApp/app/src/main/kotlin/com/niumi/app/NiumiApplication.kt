package com.niumi.app

import android.app.Application
import com.niumi.system.notification.AndroidNotificationChannelRegistrar
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NiumiApplication : Application() {
    @Inject
    lateinit var notificationChannelRegistrar: AndroidNotificationChannelRegistrar

    override fun onCreate() {
        super.onCreate()
        // Les canaux doivent exister avant toute notification : poster sur un canal inconnu
        // fait rejeter la notification par le système (et, pour un service de premier plan,
        // tue le service). Les créer ici les rend aussi visibles dans les réglages Android
        // avant même la première session. `createNotificationChannel` est idempotent.
        notificationChannelRegistrar.registerAll()
    }
}
