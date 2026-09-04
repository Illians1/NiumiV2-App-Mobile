package com.niumi.app.system

import android.content.ComponentName
import android.content.Context
import com.niumi.app.MainActivity
import com.niumi.feature.ringing.AlarmActivity
import com.niumi.feature.ringing.AlarmReceiver
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.NiumiComponentResolver

/**
 * Seul module qui voit l'ensemble du graphe de dépendances (SPEC_ANDROID §6) : traduit les
 * cibles symboliques de `:core:system` vers les vraies classes Android de `:app` et
 * `:feature:ringing`.
 */
class AppComponentResolver(
    private val context: Context,
) : NiumiComponentResolver {
    override fun componentName(component: NiumiComponent): ComponentName =
        when (component) {
            NiumiComponent.ALARM_RECEIVER -> ComponentName(context, AlarmReceiver::class.java)
            NiumiComponent.MAIN_ACTIVITY -> ComponentName(context, MainActivity::class.java)
            NiumiComponent.ALARM_ACTIVITY -> ComponentName(context, AlarmActivity::class.java)
        }
}
