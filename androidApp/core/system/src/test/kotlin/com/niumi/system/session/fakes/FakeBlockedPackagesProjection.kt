package com.niumi.system.session.fakes

import com.niumi.database.blocking.BlockedPackagesState
import com.niumi.system.blocking.BlockedPackagesProjection

class FakeBlockedPackagesProjection : BlockedPackagesProjection {
    var state: BlockedPackagesState = BlockedPackagesState.Inactive

    override fun current(): BlockedPackagesState = state
}
