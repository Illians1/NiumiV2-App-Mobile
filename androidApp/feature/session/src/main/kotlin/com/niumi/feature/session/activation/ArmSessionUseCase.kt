package com.niumi.feature.session.activation

import com.niumi.core.interop.ActivationPolicyResultDto
import com.niumi.core.interop.BlockingScheduleInputDto
import com.niumi.core.interop.BlockingScheduleResultDto
import com.niumi.core.interop.BlockingScheduleStatusDto
import com.niumi.core.interop.NextOccurrenceDto
import com.niumi.core.interop.NiumiCoreFacade
import com.niumi.core.interop.WakeScheduleInputDto
import com.niumi.core.interop.WakeScheduleResultDto
import com.niumi.database.BlockedPackage
import com.niumi.system.plan.ActivationFailure
import com.niumi.system.plan.ArmOccurrenceUseCase
import com.niumi.system.plan.ArmSessionResult
import com.niumi.system.readiness.DeviceReadinessChecker
import com.niumi.system.readiness.ReadinessInput
import com.niumi.system.readiness.toActivationPolicyInput
import com.niumi.system.session.SessionCoordinator
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
 * Aperçu affiché par le récapitulatif (écran 6, SPEC_ANDROID §15). [boxNames] et [blockedPackages]
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
    /** Un nom par Niumi Spot enregistré, dans l'ordre de `PairedBoxStore.all()` : la liste courante (étape 43). */
    val boxNames: List<String>,
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
 * Activation ponctuelle de l'écran 6 transitoire (SPEC_ANDROID §9.2), jusqu'à l'accueil de
 * l'étape 45 qui arme depuis le planning. Ce use case garde le diagnostic qui alimente l'aperçu et
 * refuse encore sur un contrôle bloquant, comme l'écran le montre ; depuis l'étape 44, l'armement
 * lui-même — Niumi Spots, sélection et sonnerie lus au moment d'armer, `ACTIVATION_REQUESTED` — est
 * délégué à [ArmOccurrenceUseCase], sous le verrou du coordinateur. Le planning n'est jamais écrit
 * ici : il reste éteint, et une session ponctuelle terminée ne réarme donc rien. Les points 4 à 9
 * (transaction Room, alarme, blocage, vérification, `ACTIVATION_SUCCEEDED`, persistance `ARMED`)
 * sont internes au coordinateur et déjà prouvés par `SessionCoordinatorActivationTest`,
 * `PhaseCompletionTest` et les instrumentés Room/Direct Boot des étapes 9 à 11.
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
        private val armOccurrence: ArmOccurrenceUseCase,
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
                boxNames = sources.pairedBoxStore.all().map { it.name },
                nowEpochMillis = diagnosis.nowEpochMillis,
            )
        }

        /**
         * Clauses de garde séquentielles (horaire invalide, début de blocage refusé, refus de la
         * politique) avant le corps principal — même motif que `DefaultSessionCoordinator.dispatch`
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

            // Le reste — Niumi Spots, sélection et sonnerie lus au moment d'armer, bornes communes,
            // dispatch — est celui de l'armement depuis le planning (étape 44), sous le même verrou.
            val occurrence =
                NextOccurrenceDto(
                    wakeSchedule = schedule,
                    // L'instant **calculé**, jamais l'heure saisie : c'est lui qui est contractuel et
                    // immuable pour la durée de la session (SPEC_CORE_KMP §8.3).
                    blockingSchedule = blockingSchedule,
                    blockingStartsImmediately = blockingSchedule.startsAtEpochMillis == null,
                )
            return coordinator.withLock { armOccurrence.arm(occurrence, it) }
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
