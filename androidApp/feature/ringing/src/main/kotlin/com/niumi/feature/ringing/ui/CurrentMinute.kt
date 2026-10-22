package com.niumi.feature.ringing.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * L'heure actuelle, relue au début de chaque minute (écran 10 : « L'heure actuelle, en très
 * grand »), et à chaque retour de l'écran au premier plan : `delay` compte un temps qui s'arrête
 * quand le téléphone dort, et un écran rallumé après dix minutes de veille montrerait sinon
 * l'heure de la mise en veille (relecture de l'étape 38). Relue à l'horloge du téléphone à chaque
 * fois : un changement d'heure ou de fuseau est pris en compte au plus tard à la minute suivante.
 */
@Composable
internal fun rememberCurrentMinute(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val current = LocalDateTime.now()
                now = current
                delay(ChronoUnit.MILLIS.between(current, current.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)))
            }
        }
    }
    return now
}
