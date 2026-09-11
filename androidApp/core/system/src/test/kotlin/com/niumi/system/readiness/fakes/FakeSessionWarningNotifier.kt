package com.niumi.system.readiness.fakes

import com.niumi.system.common.OperationResult
import com.niumi.system.notification.SessionWarningNotifier
import com.niumi.system.readiness.ReadinessCheckId

class FakeSessionWarningNotifier : SessionWarningNotifier {
    val presented = mutableListOf<ReadinessCheckId>()
    val cleared = mutableListOf<ReadinessCheckId>()
    var clearAllCallCount: Int = 0
        private set

    override fun present(id: ReadinessCheckId): OperationResult {
        presented += id
        return OperationResult.Success
    }

    override fun clear(id: ReadinessCheckId): OperationResult {
        cleared += id
        return OperationResult.Success
    }

    override fun clearAll(): OperationResult {
        clearAllCallCount++
        return OperationResult.Success
    }
}
