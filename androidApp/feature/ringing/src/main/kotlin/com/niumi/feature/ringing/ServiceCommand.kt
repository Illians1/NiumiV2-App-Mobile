package com.niumi.feature.ringing

/**
 * Extras bruts d'un `Intent` de commande, extraits en Kotlin pur avant toute validation.
 * `AlarmReceiver` et `AlarmRingingService` construisent cette valeur depuis leur `Intent` réel.
 */
data class ServiceCommandExtras(
    val sessionId: String?,
    val revision: Long?,
)

/**
 * Résultat validé d'une commande destinée au service de sonnerie (SPEC_ANDROID §16 : valider
 * tous les extras reçus par les receivers et services, jamais d'exception).
 */
sealed interface ServiceCommand {
    data class Valid(
        val sessionId: String,
        val revision: Long,
    ) : ServiceCommand

    data class Invalid(
        val reason: String,
    ) : ServiceCommand

    companion object {
        private val CANONICAL_UUID_REGEX =
            Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

        fun from(extras: ServiceCommandExtras): ServiceCommand {
            val sessionId = extras.sessionId
            val revision = extras.revision
            return when {
                sessionId == null -> Invalid("MISSING_SESSION_ID")
                !CANONICAL_UUID_REGEX.matches(sessionId) -> Invalid("INVALID_SESSION_ID")
                revision == null -> Invalid("MISSING_REVISION")
                revision <= 0L -> Invalid("INVALID_REVISION")
                else -> Valid(sessionId, revision)
            }
        }
    }
}
