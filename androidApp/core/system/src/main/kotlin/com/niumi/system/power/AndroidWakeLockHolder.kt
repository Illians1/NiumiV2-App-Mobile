package com.niumi.system.power

import android.content.Context
import android.os.PowerManager

/**
 * `PARTIAL_WAKE_LOCK` de 10 minutes, renouvelable (SPEC_ANDROID §10.2). Le service appelle
 * [renew] toutes les 8 minutes tant que la sonnerie est active, pour ne jamais laisser le
 * délai de sécurité expirer pendant un scan qui tarde.
 */
class AndroidWakeLockHolder(
    context: Context,
) : WakeLockHolder {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    override fun acquire() {
        if (wakeLock?.isHeld == true) return
        val lock =
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$WAKE_LOCK_TAG_PREFIX:AlarmRinging",
            )
        lock.acquire(WAKE_LOCK_TIMEOUT_MS)
        wakeLock = lock
    }

    override fun renew() {
        val lock = wakeLock ?: return
        if (!lock.isHeld) return
        lock.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    override fun release() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private companion object {
        const val WAKE_LOCK_TAG_PREFIX = "niumi"
        const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
    }
}
