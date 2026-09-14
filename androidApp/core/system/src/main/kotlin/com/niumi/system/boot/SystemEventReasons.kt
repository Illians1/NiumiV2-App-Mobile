package com.niumi.system.boot

import android.content.Intent
import com.niumi.system.session.ReconcileReason

/**
 * Traduction d'une action de broadcast système en [ReconcileReason] (SPEC_ANDROID §9.3).
 *
 * Objet sans état, séparé de [SystemEventsReceiver] parce que c'est la seule partie décidable de
 * celui-ci : le dépôt n'utilise pas Robolectric et un `BroadcastReceiver` n'est pas instanciable en
 * test JVM. Même partage des rôles qu'entre `AlarmReceiver` et `AlarmTriggerHandler` (étape 17).
 *
 * Les constantes `Intent.ACTION_*` sont des constantes de compilation Java, donc inlinées par le
 * compilateur : les référencer ici ne charge pas la classe `Intent` et reste testable en JVM.
 *
 * **`USER_UNLOCKED` n'est volontairement pas dans cette table.** Android ne délivre
 * `android.intent.action.USER_UNLOCKED` qu'aux receivers enregistrés à chaud, jamais à un receiver
 * déclaré dans le manifeste : il est enregistré par [SystemEventsRegistrar], qui fournit sa raison
 * directement.
 */
object SystemEventReasons {
    /**
     * Les cinq actions déclarées par le manifeste de `:core:system`. Exposées pour que le receiver
     * et le test lisent la même liste que le manifeste.
     */
    val manifestActions: List<String> =
        listOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )

    /**
     * `null` pour toute action non reconnue : le receiver ne réconcilie alors rien. Une action
     * inconnue ne peut venir que d'un filtre mal déclaré, jamais d'un tiers — le receiver est
     * `exported="false"` et les cinq actions sont des broadcasts protégés du système.
     */
    fun reasonOf(action: String?): ReconcileReason? =
        when (action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> ReconcileReason.LOCKED_BOOT
            Intent.ACTION_BOOT_COMPLETED -> ReconcileReason.BOOT
            Intent.ACTION_MY_PACKAGE_REPLACED -> ReconcileReason.PACKAGE_REPLACED
            Intent.ACTION_TIME_CHANGED -> ReconcileReason.TIME_CHANGED
            Intent.ACTION_TIMEZONE_CHANGED -> ReconcileReason.TIMEZONE_CHANGED
            else -> null
        }
}
