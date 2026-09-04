package com.niumi.system.intent

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Traduit un [PendingIntentSpec] en `PendingIntent` réel. Seule la partie non testable en JVM
 * (`Intent` et `PendingIntent` lèvent `Stub!` hors d'un appareil) ; la décision testable vit
 * dans le spec lui-même. Couverte par les tests instrumentés de l'étape.
 */
class AndroidPendingIntentFactory(
    private val context: Context,
    private val resolver: NiumiComponentResolver,
) {
    fun create(spec: PendingIntentSpec): PendingIntent {
        val intent =
            Intent().apply {
                component = resolver.componentName(spec.target)
                spec.extras.forEach { (key, value) ->
                    when (value) {
                        is IntentExtraValue.Text -> putExtra(key, value.value)
                        is IntentExtraValue.Number -> putExtra(key, value.value)
                    }
                }
            }
        return when (spec.kind) {
            PendingIntentSpec.Kind.BROADCAST -> {
                PendingIntent.getBroadcast(context, spec.requestCode, intent, spec.flags)
            }

            PendingIntentSpec.Kind.ACTIVITY -> {
                PendingIntent.getActivity(context, spec.requestCode, intent, spec.flags)
            }
        }
    }
}
