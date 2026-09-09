package com.niumi.database.mapping

import com.niumi.core.interop.IncidentEffectPayloadDto
import com.niumi.core.interop.SessionEffectDto
import com.niumi.core.interop.SessionEffectPayloadDto
import com.niumi.database.EffectStatus
import com.niumi.database.PendingEffect
import kotlinx.serialization.json.Json

/**
 * `SessionDecisionDto.effects → List<PendingEffect>` (SPEC_CORE_KMP §6, §6.1). `SessionEffectDto`
 * ne porte pas d'`ordinal` : c'est l'index dans la liste de la décision, `SessionEffectBuilder`
 * (`:shared:core`, `internal`) l'affectant dans cet ordre à la construction (`ordinal = effects.size`
 * au moment de l'ajout).
 *
 * `payloadJson` utilise un `Json` dédié (`classDiscriminator = "type"`), distinct de celui
 * d'`EventFingerprint` : les deux formats ont des contraintes indépendantes et ne doivent pas
 * dériver ensemble. Le discriminant polymorphe est fixé par `@SerialName` dans `:shared:core`
 * (jamais le nom de classe qualifié par défaut, qui romprait au moindre renommage de package).
 */
object SessionEffectMapper {
    private val persistenceJson =
        Json {
            classDiscriminator = "type"
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun List<SessionEffectDto>.toPendingEffects(): List<PendingEffect> =
        mapIndexed { ordinal, effect ->
            PendingEffect(
                effectId = effect.effectId,
                sessionId = effect.sessionId,
                revision = effect.revision,
                kind = effect.kind,
                ordinal = ordinal,
                payloadJson = effect.payload?.let(::encodePayload),
                status = EffectStatus.PENDING,
                lastError = null,
            )
        }

    fun encodePayload(payload: SessionEffectPayloadDto): String =
        persistenceJson.encodeToString(SessionEffectPayloadDto.serializer(), payload)

    fun decodeIncidentPayload(json: String): IncidentEffectPayloadDto =
        persistenceJson.decodeFromString(SessionEffectPayloadDto.serializer(), json) as IncidentEffectPayloadDto

    /**
     * Introspection du descripteur du sérialiseur scellé (`kotlinx.serialization`, sans
     * `kotlin-reflect`) : l'élément "value" d'un descripteur `PolymorphicKind.SEALED` énumère un
     * sous-type par élément, nommé par son discriminant `@SerialName`. Un futur variant de
     * `SessionEffectPayloadDto` ajouté sans discriminant explicite fait échouer ce test plutôt que
     * de silencieusement écrire un nom de classe qualifié en base.
     */
    fun knownPayloadDiscriminants(): Set<String> {
        val valueDescriptor = SessionEffectPayloadDto.serializer().descriptor.getElementDescriptor(1)
        return (0 until valueDescriptor.elementsCount).map { valueDescriptor.getElementName(it) }.toSet()
    }
}
