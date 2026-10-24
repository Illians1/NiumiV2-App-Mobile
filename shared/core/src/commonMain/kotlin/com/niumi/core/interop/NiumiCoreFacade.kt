package com.niumi.core.interop

import com.niumi.core.diagnostics.ActivationPolicy
import com.niumi.core.domain.SessionEngine
import com.niumi.core.emergency.EmergencyExitTexts
import com.niumi.core.emergency.EmergencyTranscription
import com.niumi.core.insight.TimeToGetUpCalculator
import com.niumi.core.nfc.BoxPayloadParser
import com.niumi.core.nfc.BoxVerifier
import com.niumi.core.schedule.BlockingScheduleCalculator
import com.niumi.core.schedule.NextOccurrenceCalculator
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
     * Compare [payload] à la liste de Niumi Spots [credentials], l'ensemble enregistré au moment du scan
     * (contrat 1.5, SPEC_CORE_KMP §9.2). [context] est fourni pour un scan de fin de session ; `null` ne produit
     * aucune preuve. L'association n'appelle plus cette méthode : elle parse et déduplique au dépôt.
     */
    public fun verifyBox(
        payload: BoxPayloadDto,
        credentials: List<PairedBoxCredentialDto>,
        context: NfcVerificationContextDto?,
    ): BoxVerificationResultDto =
        BoxVerifier
            .verify(payload.toDomain(), credentials.map { it.toDomain() }, context?.toDomain())
            .toDto()

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

    /**
     * Calcule l'instant de début d'un blocage différé (SPEC_CORE_KMP §8.3). Septième méthode,
     * ajoutée par le contrat 1.3 (§14) : un `localTimeIso` nul décrit un blocage immédiat et renvoie
     * `VALID` avec un schedule aux trois champs nuls ; sinon l'instant est résolu comme un réveil
     * puis comparé à `triggerAtEpochMillis`.
     */
    public fun computeBlockingSchedule(input: BlockingScheduleInputDto): BlockingScheduleResultDto =
        BlockingScheduleCalculator.compute(input.toDomain()).toDto()

    /**
     * Calcule la prochaine occurrence d'un planning récurrent (SPEC_CORE_KMP §8.4). Huitième
     * méthode, ajoutée par le contrat 1.5 (§14) : réveil et coucher prêts pour une activation, ou
     * le statut qui empêche d'armer (aucun jour, heure ou fuseau invalide, période vide).
     */
    public fun computeNextOccurrence(input: NextOccurrenceInputDto): NextOccurrenceResultDto =
        NextOccurrenceCalculator.compute(input.toDomain()).toDto()

    /**
     * Textes de la sortie de secours (contrat 1.5, SPEC_CORE_KMP §12, §14). Un seul texte validé
     * aujourd'hui (D8) ; la liste est prête pour d'autres.
     */
    public fun emergencyExitTexts(): List<EmergencyExitTextDto> =
        EmergencyExitTexts.ids.mapNotNull { id ->
            EmergencyExitTexts.text(id)?.let { text ->
                EmergencyExitTextDto(id = id, text = text, charCount = text.length)
            }
        }

    /**
     * Compare une recopie au texte modèle (contrat 1.5, SPEC_CORE_KMP §12). [context] est fourni par
     * le coordinateur pour la confirmation de sortie, comme pour [verifyBox] ; `null` (saisie en
     * cours) ne produit aucune preuve.
     */
    public fun verifyEmergencyTranscription(
        input: EmergencyTranscriptionInputDto,
        context: NfcVerificationContextDto?,
    ): EmergencyTranscriptionResultDto =
        EmergencyTranscription.verify(input.textId, input.typed, context?.toDomain()).toDto()

    /** Temps mis à se lever et minutes gagnées de l'écran 11 (contrat 1.5, `SPECIFICATION.md` §15). */
    public fun computeTimeToGetUp(input: TimeToGetUpInputDto): TimeToGetUpResultDto =
        TimeToGetUpCalculator.compute(input.toDomain()).toDto()
}
