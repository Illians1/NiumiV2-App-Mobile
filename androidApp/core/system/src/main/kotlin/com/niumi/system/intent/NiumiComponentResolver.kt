package com.niumi.system.intent

import android.content.ComponentName

/**
 * Traduit une cible symbolique [NiumiComponent] en `ComponentName` réel. Implémenté dans `:app`,
 * seul module qui voit `MainActivity` et les composants de `:feature:ringing` à la fois.
 * `fun interface` comme [com.niumi.system.audio.RingtoneResourceResolver] : une seule méthode,
 * donc implémentable par lambda là où une classe dédiée n'apporterait rien (tests instrumentés).
 */
fun interface NiumiComponentResolver {
    fun componentName(component: NiumiComponent): ComponentName
}
