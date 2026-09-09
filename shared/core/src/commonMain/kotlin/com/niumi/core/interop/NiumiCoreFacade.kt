package com.niumi.core.interop

import com.niumi.core.diagnostics.ActivationPolicy
import com.niumi.core.domain.SessionEngine
import com.niumi.core.nfc.BoxPayloadParser
import com.niumi.core.nfc.BoxVerifier
import com.niumi.core.schedule.TriggerDelayPolicy
import com.niumi.core.schedule.WakeScheduleCalculator

/**
 * API publique exposée aux plateformes natives (SPEC_CORE_KMP §14), complétée à l'étape 8 avec la
 * machine à états et les deux politiques communes. `evaluateTriggerDelay` étend §14 (§8.2 : la
 * fenêtre de grâce Android de 15 minutes n'a pas d'équivalent iOS, cf. `ETAPE-08.md`). [engine]
 * est sans horloge ni aléa ; aucune exception ne doit traverser cette frontière.
 */
public class NiumiCoreFacade {
    private val engine = SessionEngine()

    /** Transforme l'URI brute lue sur le tag NFC en payload typé (SPEC_ANDROID §11.2). */
    public fun parseBoxPayload(uri: String): BoxPayloadResultDto = BoxPayloadParser.parse(uri).toDto()

    /**
     * Compare [payload] au boîtier associé [credential]. [context] est fourni pour un scan de fin
     * de session, `null` lors d'une association (SPEC_CORE_KMP §14, dernier alinéa).
     */
    public fun verifyBox(
        payload: BoxPayloadDto,
        credential: PairedBoxCredentialDto,
        context: NfcVerificationContextDto?,
    ): BoxVerificationResultDto =
        BoxVerifier.verify(payload.toDomain(), credential.toDomain(), context?.toDomain()).toDto()

    /** Unique autorité de transition de la machine à états commune (SPEC_CORE_KMP §5, §6). */
    public fun reduce(
        snapshot: SessionSnapshotDto?,
        event: SessionEventDto,
    ): SessionDecisionDto = engine.reduce(snapshot?.toDomain(), event.toDomain()).toDto()

    /** Calcule l'heure de réveil à partir d'une heure locale choisie (SPEC_CORE_KMP §8.1). */
    public fun computeWakeSchedule(input: WakeScheduleInputDto): WakeScheduleResultDto =
        WakeScheduleCalculator.compute(input.toDomain()).toDto()

    /** Décide si une activation est permise (SPEC_CORE_KMP §7.4, SPEC_ANDROID §13). */
    public fun evaluateActivation(input: ActivationPolicyInputDto): ActivationPolicyResultDto =
        ActivationPolicy.evaluate(input.toDomain()).toDto()

    /**
     * Politique de reprogrammation Android de 15 minutes (SPEC_CORE_KMP §8.2). Sixième méthode,
     * en extension de §14 : iOS peut l'ignorer, faute d'équivalent AlarmKit à `setAlarmClock()`.
     */
    public fun evaluateTriggerDelay(input: TriggerDelayInputDto): TriggerDelayResultDto =
        TriggerDelayResultDto(TriggerDelayPolicy.evaluate(input.triggerAtEpochMillis, input.nowEpochMillis))
}
