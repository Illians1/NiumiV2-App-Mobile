package com.niumi.system.session.fakes

import com.niumi.system.blocking.AccessibilityServiceStatus

class FakeAccessibilityServiceStatus(
    var enabled: Boolean = true,
) : AccessibilityServiceStatus {
    override fun isEnabled(): Boolean = enabled
}
