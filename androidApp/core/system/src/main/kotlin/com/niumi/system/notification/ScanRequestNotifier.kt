package com.niumi.system.notification

import com.niumi.system.common.OperationResult

/**
 * Notification persistante « en attente de scan » (« Interfaces transverses » du plan MVP,
 * SPEC_ANDROID §10.5). [present] et [clear] sont idempotents.
 */
interface ScanRequestNotifier {
    fun present(sessionId: String): OperationResult

    fun clear(sessionId: String): OperationResult
}
