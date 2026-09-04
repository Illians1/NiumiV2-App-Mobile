package com.niumi.system.intent

/**
 * Description pure d'un `PendingIntent` à créer, sans dépendre d'aucune classe Android.
 * Testable en JVM : `AndroidPendingIntentFactory` traduit ce spec en `PendingIntent` réel,
 * seule partie qui ne peut pas être vérifiée hors d'un appareil (les classes `Intent` et
 * `PendingIntent` de `android.jar` lèvent `Stub!` en test unitaire).
 */
data class PendingIntentSpec(
    val kind: Kind,
    val target: NiumiComponent,
    val requestCode: Int,
    val flags: Int,
    val extras: Map<String, IntentExtraValue> = emptyMap(),
) {
    enum class Kind { BROADCAST, ACTIVITY }
}
