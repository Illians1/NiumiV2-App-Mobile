package com.niumi.feature.session.activation

import com.niumi.core.diagnostics.ActivationReasonCode
import com.niumi.core.interop.ActivationPolicyResultDto
import com.niumi.core.interop.ActivationReasonDto
import com.niumi.core.interop.ActivationRequestDto
import com.niumi.core.interop.AppSelectionSummaryDto
import com.niumi.core.interop.BlockingScheduleInputDto
import com.niumi.core.interop.BlockingScheduleResultDto
import com.niumi.core.interop.BlockingScheduleStatusDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleInputDto
import com.niumi.core.interop.WakeScheduleResultDto
import com.niumi.database.AndroidSessionExtras
import com.niumi.database.BlockedPackage
import com.niumi.system.audio.NiumiRingtones
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.readiness.ReadinessInput
import com.niumi.system.readiness.toActivationPolicyInput
import com.niumi.system.session.DispatchResult
import com.niumi.system.session.SessionCoordinator
import com.niumi.system.session.SessionEventFactory
import javax.inject.Inject

/**
 * Résultat des points 1 et 2 de §9.2, partagé par [ArmSessionUseCase.preview] et
 * [ArmSessionUseCase.arm] : les deux appliquent le même verdict, calculé par le même code.
 * [policy] est `null` quand [scheduleResult] n'a pas produit d'horaire `VALID` — la politique
 * commune n'a alors rien à évaluer.
 */
private data class Diagnosis(
    val nowEpochMillis: Long,
    val scheduleResult: WakeScheduleResultDto,
    val blockingResult: BlockingScheduleResultDto?,
    val policy: ActivationPolicyResultDto?,
)

/**
 * Aperçu affiché par le récapitulatif (écran 6, SPEC_ANDROID §15). [boxId] et [blockedPackages]
 * sont lus indépendamment du verdict d'activation : l'écran doit pouvoir montrer ce qui est
 * choisi même quand un contrôle bloque encore (§19.1, « impossibilité de confirmer si un
 * contrôle bloquant échoue » — encore faut-il pouvoir afficher pourquoi).
 */
data class ActivationPreview(
    val scheduleResult: WakeScheduleResultDto,
    /**
     * Résultat du calcul du début de blocage (Lot 6), `null` quand l'horaire de réveil lui-même n'est
     * pas valide — il n'y a alors rien à comparer. Un blocage immédiat donne `VALID` avec un schedule
     * `IMMEDIATE`, jamais `null`.
     */
    val blockingResult: BlockingScheduleResultDto?,
    val policy: ActivationPolicyResultDto?,
    val blockedPackages: List<BlockedPackage>,
    val boxId: String?,
    val nowEpochMillis: Long,
) {
    /**
     * L'écran 5 refuse de continuer sur un début non antérieur au réveil **avant même** le diagnostic
     * (SPEC_ANDROID §13, point 4) : la politique commune le refuserait aussi, mais l'utilisateur doit
     * l'apprendre de la phrase de confirmation, pas d'un message d'appareil non prêt.
     */
    val canActivate: Boolean
        get() = policy?.allowed == true && blockingResult?.status == BlockingScheduleStatusDto.VALID
}

/**
 * Orchestre les points 1 à 6 de l'activation en deux phases (SPEC_ANDROID §9.2). Les points 4 à 9
 * (transaction Room, alarme, blocage, vérification, `ACTIVATION_SUCCEEDED`, persistance `ARMED`)
 * sont internes à [SessionCoordinator.dispatch] et déjà prouvés par
 * `SessionCoordinatorActivationTest`, `PhaseCompletionTest` et les instrumentés Room/Direct Boot
 * des étapes 9 à 11 : ce use case ne dispatche que `ACTIVATION_REQUESTED` et n'observe que le
 * résultat.
 *
 * | Points §9.2 | Prouvé par |
 * |---|---|
 * | 1, 2, 3 | `ArmSessionUseCaseTest` (journal) + `SessionEventFactoryActivationTest` |
 * | 4 à 9 | `:core:system` (étapes 9 à 11) |
 * | 10 | `SummaryViewModelTest` + câblage du `NavHost` |
 *
 * `preview()` rejoue le diagnostic à l'identique de `arm()`, mais ne dispatche jamais : l'écran et
 * l'activation appliquent le même verdict sans jamais réutiliser un aperçu figé (SPEC_CORE_KMP
 * §8.1, recalcul du fuseau entre saisie et confirmation).
 */
class ArmSessionUseCase
    @Inject
    constructor(
        private val readinessChecker: DeviceReadinessChecker,
        private val facade: NiumiCoreFacade,
        private val coordinator: SessionCoordinator,
        private val eventFactory: SessionEventFactory,
        private val sources: ActivationSources,
    ) {
        suspend fun preview(
            localTimeIso: String,
            blockingLocalTimeIso: String?,
        ): ActivationPreview {
            val diagnosis = diagnose(localTimeIso, blockingLocalTimeIso)
            return ActivationPreview(
                scheduleResult = diagnosis.scheduleResult,
                blockingResult = diagnosis.blockingResult,
                policy = diagnosis.policy,
                blockedPackages = sources.appSelectionStore.selection(),
                boxId = sources.pairedBoxStore.current()?.boxId,
                nowEpochMillis = diagnosis.nowEpochMillis,
            )
        }

        /**
         * Clauses de garde séquentielles (horaire invalide, refus de la politique, boîtier
         * disparu) avant le corps principal — même motif que `DefaultSessionCoordinator.dispatch`
         * (`:core:system`) et `NfcReducer.onValidScan` (`:shared:core`). Chaque refus est une
         * issue distincte de §9.2 point 2, que fondre dans une seule sortie rendrait moins
         * lisible, pas plus.
         */
        @Suppress("ReturnCount")
        suspend fun arm(
            localTimeIso: String,
            blockingLocalTimeIso: String?,
        ): ArmSessionResult {
            val diagnosis = diagnose(localTimeIso, blockingLocalTimeIso)
            val schedule = diagnosis.scheduleResult.schedule
            val policy = diagnosis.policy
            if (schedule == null || policy == null) {
                return ArmSessionResult.Failed(ActivationFailure.InvalidSchedule(diagnosis.scheduleResult.status))
            }
            // Avant le refus de la politique : un début de blocage mal choisi est une erreur de
            // saisie, pas un défaut de l'appareil, et l'annoncer comme tel renverrait l'utilisateur
            // vers des réglages système qui n'y peuvent rien (SPEC_ANDROID §13, point 4).
            val blockingSchedule =
                diagnosis.blockingResult
                    ?.takeIf { it.status == BlockingScheduleStatusDto.VALID }
                    ?.schedule
                    ?: return ArmSessionResult.Failed(
                        ActivationFailure.InvalidBlockingSchedule(
                            diagnosis.blockingResult?.status ?: BlockingScheduleStatusDto.INVALID_TIME,
                        ),
                    )
            if (!policy.allowed) {
                return ArmSessionResult.Failed(ActivationFailure.Blocked(policy.blockingReasons))
            }

            // Point 5 de §9.2 côté commun (SPEC_CORE_KMP §10) : le credential est capturé et figé
            // ici, à l'étape 1 de la transaction — jamais relu depuis le dépôt pendant la session.
            val credential =
                sources.pairedBoxStore.current()
                    ?: return ArmSessionResult.Failed(
                        ActivationFailure.Blocked(
                            listOf(ActivationReasonDto(ActivationReasonCode.NO_PAIRED_BOX, null)),
                        ),
                    )
            val selection = sources.appSelectionStore.selection()

            val request =
                ActivationRequestDto(
                    wakeSchedule = schedule,
                    appSelection = AppSelectionSummaryDto(count = selection.size),
                    // L'instant **calculé**, jamais l'heure saisie : c'est lui qui est contractuel et
                    // immuable pour la durée de la session (SPEC_CORE_KMP §8.3).
                    blockingSchedule = blockingSchedule,
                )
            val extras =
                AndroidSessionExtras(
                    boxId = credential.boxId,
                    boxTokenSha256Hex = credential.tokenSha256Hex,
                    ringtoneKey = NiumiRingtones.DEFAULT_KEY,
                    vibrationEnabled = true,
                    blockedPackages = selection,
                )

            return when (val result = coordinator.dispatch(eventFactory.activationRequested(request), extras)) {
                is DispatchResult.Applied -> interpretApplied(result)
                is DispatchResult.Rejected -> ArmSessionResult.Failed(ActivationFailure.Rejected(result.violations))
                is DispatchResult.Duplicate -> ArmSessionResult.Failed(ActivationFailure.Duplicate)
            }
        }

        private fun interpretApplied(applied: DispatchResult.Applied): ArmSessionResult {
            val snapshot = applied.snapshot
            return if (snapshot != null && snapshot.state == SessionStateDto.ARMED) {
                ArmSessionResult.Armed(snapshot)
            } else {
                ArmSessionResult.Failed(ActivationFailure.CoordinatorFailed(snapshot?.failureCode, snapshot?.state))
            }
        }

        /**
         * Points 1 et 2 de §9.2. Deux appels à [DeviceReadinessChecker.check] : le premier, sans
         * candidat, ne sert qu'à lire `nowEpochMillis` sur la même horloge que le reste du
         * diagnostic ; le second, avec le candidat calculé, est seul transmis à
         * `evaluateActivation`. Un diagnostic lancé avant tout choix d'heure recevrait
         * `triggerAtEpochMillis = nowEpochMillis` et serait refusé par `TRIGGER_NOT_IN_FUTURE` —
         * impossible ici puisque [schedule] est déjà connu au second appel.
         */
        private suspend fun diagnose(
            localTimeIso: String,
            blockingLocalTimeIso: String?,
        ): Diagnosis {
            val nowEpochMillis = readinessChecker.check(ReadinessInput()).nowEpochMillis
            val zoneId = sources.timeZoneProvider.currentZoneId()
            val scheduleResult =
                facade.computeWakeSchedule(WakeScheduleInputDto(localTimeIso, zoneId, nowEpochMillis))
            val schedule = scheduleResult.schedule
            // Même zone et même `nowEpochMillis` que le réveil : une session n'a qu'un fuseau
            // d'activation, et les deux instants doivent être calculés sur la même horloge, faute de
            // quoi l'antériorité vérifiée ne serait pas celle qui sera persistée (SPEC_CORE_KMP §8.3).
            val blockingResult =
                schedule?.let {
                    facade.computeBlockingSchedule(
                        BlockingScheduleInputDto(
                            localTimeIso = blockingLocalTimeIso,
                            zoneId = zoneId,
                            nowEpochMillis = nowEpochMillis,
                            triggerAtEpochMillis = it.triggerAtEpochMillis,
                        ),
                    )
                }
            val policy =
                schedule?.let {
                    val report =
                        readinessChecker.check(
                            ReadinessInput(
                                candidateTriggerAtEpochMillis = it.triggerAtEpochMillis,
                                candidateBlockingStartsAtEpochMillis =
                                    blockingResult?.schedule?.startsAtEpochMillis,
                            ),
                        )
                    facade.evaluateActivation(report.toActivationPolicyInput())
                }
            return Diagnosis(nowEpochMillis, scheduleResult, blockingResult, policy)
        }
    }
