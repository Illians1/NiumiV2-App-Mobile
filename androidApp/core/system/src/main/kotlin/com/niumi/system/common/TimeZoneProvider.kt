package com.niumi.system.common

import java.time.ZoneId

/**
 * Fuseau IANA courant, injectable : aucun composant ne lit `ZoneId.systemDefault()` ni
 * `TimeZone.getDefault()` directement, pour que les tests n'attendent jamais le fuseau réel de
 * la machine (même justification que [Clock], SPEC_ANDROID §19.2).
 *
 * `:feature:session` le recalcule à chaque écran (§8 : « l'interface recalcule l'heure
 * d'affichage dans le fuseau courant ») et `ArmSessionUseCase` le relit au moment de l'activation
 * pour ne jamais utiliser un fuseau périmé entre le choix de l'heure et la confirmation
 * (SPEC_CORE_KMP §8.1, §10).
 */
fun interface TimeZoneProvider {
    fun currentZoneId(): String
}

class SystemTimeZoneProvider : TimeZoneProvider {
    override fun currentZoneId(): String = ZoneId.systemDefault().id
}
