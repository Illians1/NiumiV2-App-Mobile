package com.niumi.system.session

import com.niumi.core.interop.SessionEventDto
import com.niumi.database.AndroidSessionExtras
import javax.inject.Provider

/**
 * Enveloppe [DefaultSessionCoordinator] pour appeler [SessionPassHook] sous son verrou (étape 44).
 * Décorateur plutôt que septième dépendance du coordinateur : `LongParameterList` de detekt, et le
 * coordinateur reste ignorant du planning. [hook] est un [Provider] : le `WakePlanScheduler` qui
 * l'implémente dépend lui-même du coordinateur, et Hilt ne résout le cycle que par lui.
 */
class HookedSessionCoordinator(
    private val inner: SessionCoordinator,
    private val hook: Provider<SessionPassHook>,
) : SessionCoordinator {
    override suspend fun dispatch(
        event: SessionEventDto,
        extras: AndroidSessionExtras?,
    ): DispatchResult =
        inner.withLock { session ->
            session.dispatch(event, extras).also { hook.get().afterDispatch(it, session) }
        }

    override suspend fun reconcile(reason: ReconcileReason): ReconcileResult =
        inner.withLock { session ->
            val result = session.reconcile(reason)
            result.copy(actions = result.actions + hook.get().afterReconcile(reason, result, session))
        }

    override suspend fun updateAlarmSound(
        ringtoneKey: String,
        volumeRampSeconds: Int?,
    ): AlarmSoundUpdateResult = inner.updateAlarmSound(ringtoneKey, volumeRampSeconds)

    override suspend fun <T> withLock(block: suspend (LockedSession) -> T): T = inner.withLock(block)
}
