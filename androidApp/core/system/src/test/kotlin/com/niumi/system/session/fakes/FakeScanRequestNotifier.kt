package com.niumi.system.session.fakes

import com.niumi.system.common.OperationResult
import com.niumi.system.notification.ScanRequestNotifier

class FakeScanRequestNotifier(
    private val journal: CallJournal? = null,
) : ScanRequestNotifier {
    var presentResult: OperationResult = OperationResult.Success
    var clearResult: OperationResult = OperationResult.Success
    var presentCallCount: Int = 0
        private set
    var clearCallCount: Int = 0
        private set

    override fun present(sessionId: String): OperationResult {
        presentCallCount++
        journal?.record("ScanRequestNotifier.present")
        return presentResult
    }

    override fun clear(sessionId: String): OperationResult {
        clearCallCount++
        journal?.record("ScanRequestNotifier.clear")
        return clearResult
    }
}
