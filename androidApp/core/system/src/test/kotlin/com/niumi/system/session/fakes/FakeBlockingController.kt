package com.niumi.system.session.fakes

import com.niumi.database.BlockedPackage
import com.niumi.system.blocking.BlockingController
import com.niumi.system.common.OperationResult

class FakeBlockingController(
    private val journal: CallJournal? = null,
) : BlockingController {
    var applyResult: OperationResult = OperationResult.Success
    var removeResult: OperationResult = OperationResult.Success
    var serviceEnabled: Boolean = true
    private var active: Set<String> = emptySet()

    override fun apply(
        sessionId: String,
        packages: Set<BlockedPackage>,
    ): OperationResult {
        journal?.record("BlockingController.apply")
        if (applyResult is OperationResult.Success) active = packages.map { it.packageName }.toSet()
        return applyResult
    }

    override fun remove(sessionId: String): OperationResult {
        journal?.record("BlockingController.remove")
        if (removeResult !is OperationResult.Failure) active = emptySet()
        return removeResult
    }

    override fun effectivePackages(): Set<String> = active

    override fun isServiceEnabled(): Boolean = serviceEnabled
}
