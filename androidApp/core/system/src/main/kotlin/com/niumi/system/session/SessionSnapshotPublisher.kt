package com.niumi.system.session

import com.niumi.core.interop.SessionSnapshotDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Diffuse le dernier snapshot publié à l'UI (effet `PUBLISH_PLATFORM_SNAPSHOT`, SPEC_CORE_KMP §6). */
class SessionSnapshotPublisher {
    private val state = MutableStateFlow<SessionSnapshotDto?>(null)
    val snapshot: StateFlow<SessionSnapshotDto?> = state

    fun publish(snapshot: SessionSnapshotDto?) {
        state.value = snapshot
    }
}
