package com.niumi.system.blocking

/**
 * Parseur pur de `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` (SPEC_ANDROID §13, §19.1) :
 * une liste de composants `pkg/classe` séparés par `:`. Android accepte, pour la partie
 * classe, un nom qualifié complet ou un nom relatif préfixé par un point (`pkg/.Classe`
 * équivaut à `pkg/pkg.Classe`).
 */
object EnabledAccessibilityServicesParser {
    fun isEnabled(
        rawEnabledServices: String?,
        expectedComponent: String,
    ): Boolean {
        val expected = splitComponent(expectedComponent)
        if (rawEnabledServices.isNullOrEmpty() || expected == null) return false
        return rawEnabledServices.split(":").any { entry ->
            val component = splitComponent(entry)
            component != null &&
                component.packageName == expected.packageName &&
                resolvedClassName(component) == expected.className
        }
    }

    private fun resolvedClassName(component: Component): String =
        if (component.className.startsWith(".")) {
            component.packageName + component.className
        } else {
            component.className
        }

    private fun splitComponent(value: String): Component? {
        val separatorIndex = value.indexOf('/')
        if (separatorIndex <= 0 || separatorIndex == value.lastIndex) return null
        return Component(
            packageName = value.substring(0, separatorIndex),
            className = value.substring(separatorIndex + 1),
        )
    }

    private data class Component(
        val packageName: String,
        val className: String,
    )
}
