package com.niumi.feature.session.home

import com.niumi.system.common.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Battement de l'accueil (Q13, décision du 2026-10-25) : une émission tout de suite, puis à chaque
 * changement de minute de l'horloge, pour faire avancer le point de l'heure actuelle et les comptes
 * à rebours. Une interface pour que les tests émettent eux-mêmes.
 */
fun interface MinuteTicker {
    fun ticks(): Flow<Unit>
}

class ClockMinuteTicker
    @Inject
    constructor(
        private val clock: Clock,
    ) : MinuteTicker {
        override fun ticks(): Flow<Unit> =
            flow {
                while (true) {
                    emit(Unit)
                    delay(MINUTE_MILLIS - clock.nowEpochMillis().mod(MINUTE_MILLIS))
                }
            }

        private companion object {
            const val MINUTE_MILLIS = 60_000L
        }
    }
