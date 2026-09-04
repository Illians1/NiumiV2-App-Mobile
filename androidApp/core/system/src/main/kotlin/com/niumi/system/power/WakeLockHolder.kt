package com.niumi.system.power

/** Acquiert et libère un `PARTIAL_WAKE_LOCK` avec délai de sécurité renouvelable (§10.2). */
interface WakeLockHolder {
    fun acquire()

    fun renew()

    fun release()
}
