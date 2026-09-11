package com.niumi.system.session.fakes

import com.niumi.system.blocking.BlockedPackagesProjection
import com.niumi.system.blocking.BlockedPackagesState

class FakeBlockedPackagesProjection : BlockedPackagesProjection {
    var state: BlockedPackagesState = BlockedPackagesState.Inactive

    override fun current(): BlockedPackagesState = state
}
