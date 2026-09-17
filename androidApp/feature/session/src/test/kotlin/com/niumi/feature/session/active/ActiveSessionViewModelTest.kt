package com.niumi.feature.session.active

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.BlockingScheduleDto
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.SessionHealthDto
import com.niumi.core.interop.SessionIncidentDto
import com.niumi.core.interop.SessionSnapshotDto
import com.niumi.core.interop.SessionStateDto
import com.niumi.core.interop.WakeScheduleDto
import com.niumi.database.BlockedPackage
import com.niumi.feature.session.active.fakes.FakeSessionIncidentsReader
import com.niumi.feature.session.active.fakes.FakeSessionPersistenceGateway
import com.niumi.feature.session.active.fakes.RecordingForegroundReadinessTrigger
import com.niumi.feature.session.active.fakes.presentSession
import com.niumi.feature.session.wake.fakes.FakeClock
import com.niumi.feature.session.wake.fakes.FakeTimeZoneProvider
import com.niumi.system.readiness.ReadinessAction
import com.niumi.system.session.LoadResult
import com.niumi.system.session.SessionSnapshotPublisher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Écran 7 complet (SPEC_ANDROID §15, §8). L'instant programmé ne change jamais après l'armement :
 * seule sa lecture locale suit le fuseau courant. S'y ajoutent, depuis l'étape 15, les
 * applications bloquées, la santé, les incidents triés par gravité, et le déclencheur de
 * surveillance de §13.1 au retour au premier plan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveSessionViewModelTest {
    private fun paris(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long =
        ZonedDateTime
            .of(year, month, day, hour, minute, 0, 0, ZoneId.of("Europe/Paris"))
            .toInstant()
            .toEpochMilli()

    private val now = paris(2026, 9, 3, 20, 0)
    private val clock = FakeClock(now)
    private val timeZoneProvider = FakeTimeZoneProvider(zoneId = "Europe/Paris")
    private val snapshotPublisher = SessionSnapshotPublisher()
    private val gateway = FakeSessionPersistenceGateway()
    private val incidentsReader = FakeSessionIncidentsReader()
    private val readinessTrigger = RecordingForegroundReadinessTrigger()

    private val blockedApps =
        listOf(
            BlockedPackage("com.exemple.reseau", "Réseau social"),
            BlockedPackage("com.exemple.jeu", "Jeu"),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        ActiveSessionViewModel(
            clock = clock,
            timeZoneProvider = timeZoneProvider,
            snapshotPublisher = snapshotPublisher,
            gateway = gateway,
            incidentsReader = incidentsReader,
            readinessTrigger = readinessTrigger,
        )

    private fun incident(
        code: String,
        severity: IncidentSeverityDto,
        occurredAtEpochMillis: Long = now,
    ) = SessionIncidentDto(code, severity, occurredAtEpochMillis, PlatformDto.ANDROID)

    private fun snapshot(state: SessionStateDto = SessionStateDto.ARMED) =
        SessionSnapshotDto(
            schemaVersion = 1,
            revision = 2,
            sessionId = "11111111-1111-1111-1111-111111111111",
            wakeSchedule =
                WakeScheduleDto(
                    localDateIso = "2026-09-04",
                    localTimeIso = "07:00",
                    zoneIdAtActivation = "Europe/Paris",
                    triggerAtEpochMillis = paris(2026, 9, 4, 7, 0),
                ),
            state = state,
            releaseTarget = null,
            health = SessionHealthDto.HEALTHY,
            createdAtEpochMillis = now,
            armedAtEpochMillis = now,
            ringingAtEpochMillis = null,
            alarmSoundStoppedAtEpochMillis = null,
            triggerElapsedAtEpochMillis = null,
            nfcVerifiedAtEpochMillis = null,
            releasingAtEpochMillis = null,
            completedAtEpochMillis = null,
            cancelledAtEpochMillis = null,
            failureCode = null,
        )

    /**
     * Session à blocage différé (Lot 6). `schemaVersion = 2` : c'est la version du contrat 1.3, et
     * un snapshot de version 1 ne peut pas porter ces champs (SPEC_CORE_KMP §7.1).
     */
    private fun deferredSnapshot(blockingAppliedAtEpochMillis: Long? = null) =
        snapshot().copy(
            schemaVersion = 2,
            blockingSchedule =
                BlockingScheduleDto(
                    localDateIso = "2026-09-03",
                    localTimeIso = "22:30",
                    startsAtEpochMillis = paris(2026, 9, 3, 22, 30),
                ),
            blockingAppliedAtEpochMillis = blockingAppliedAtEpochMillis,
        )

    @Test
    fun anArmedSnapshotShowsItsStateDateTimeAndZone() {
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.state).isEqualTo(SessionStateDto.ARMED)
        assertThat(viewModel.state.displayAtActivation?.timeLabel).isEqualTo("07:00")
        assertThat(viewModel.state.displayAtActivation?.zoneLabel).isEqualTo("Europe/Paris")
        assertThat(viewModel.state.hasSession).isTrue()
    }

    @Test
    fun sameZoneDoesNotAddASecondReading() {
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.displayInCurrentZone).isNull()
    }

    @Test
    fun aDifferentCurrentZoneAddsARecomputedLocalReadingWithoutChangingTheInstant() {
        timeZoneProvider.zoneId = "Pacific/Auckland"
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.displayAtActivation?.timeLabel).isEqualTo("07:00")
        assertThat(viewModel.state.displayInCurrentZone?.zoneLabel).isEqualTo("Pacific/Auckland")
        assertThat(viewModel.state.displayInCurrentZone?.timeLabel).isEqualTo("17:00")
    }

    /**
     * Défaut mesuré sur appareil à l'étape 14 : l'écran 7 affichait « 07:00 » sur un téléphone
     * réglé en 12 h, alors que les écrans 5 et 6 affichaient « 7:00 AM ». Trois écrans portant la
     * même information ne peuvent pas employer deux conventions (§15).
     */
    @Test
    fun aTwelveHourDeviceRendersAmPmOnTheActiveSessionScreen() {
        val viewModel = viewModel()

        viewModel.refresh(use24Hour = false)
        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.displayAtActivation?.timeLabel).isEqualTo("7:00 AM")
    }

    @Test
    fun switchingToTwelveHourReprojectsTheSnapshotAlreadyPublished() {
        val viewModel = viewModel()
        snapshotPublisher.publish(snapshot())
        assertThat(viewModel.state.displayAtActivation?.timeLabel).isEqualTo("07:00")

        viewModel.refresh(use24Hour = false)

        assertThat(viewModel.state.displayAtActivation?.timeLabel).isEqualTo("7:00 AM")
    }

    @Test
    fun noSnapshotShowsTheEmptyStateWithoutCrashing() {
        val viewModel = viewModel()

        assertThat(viewModel.state.hasSession).isFalse()
        assertThat(viewModel.state.isLoading).isFalse()
    }

    // Début du blocage (Lot 6, SPEC_ANDROID §15 « Écran 7 » ; point de vigilance 12 : c'est
    // `blockingAppliedAtEpochMillis` qui dit si les applications sont bloquées, jamais `ARMED`).

    @Test
    fun aPendingDeferredBlockingAnnouncesItsStartInstant() {
        val viewModel = viewModel()

        snapshotPublisher.publish(deferredSnapshot())

        assertThat(viewModel.state.isBlockingPending).isTrue()
        assertThat(viewModel.state.blockingDisplayAtActivation?.relativeDayLabel).isEqualTo("Aujourd'hui")
        assertThat(viewModel.state.blockingDisplayAtActivation?.timeLabel).isEqualTo("22:30")
        assertThat(viewModel.state.blockingDisplayAtActivation?.zoneLabel).isEqualTo("Europe/Paris")
        assertThat(viewModel.state.blockingDisplayInCurrentZone).isNull()
    }

    @Test
    fun aDifferentCurrentZoneAddsASecondReadingOfTheBlockingStart() {
        timeZoneProvider.zoneId = "Pacific/Auckland"
        val viewModel = viewModel()

        snapshotPublisher.publish(deferredSnapshot())

        // Europe/Paris 22:30 == Pacific/Auckland 08:30 le lendemain : l'instant ne bouge pas (§8).
        assertThat(viewModel.state.blockingDisplayAtActivation?.timeLabel).isEqualTo("22:30")
        assertThat(viewModel.state.blockingDisplayInCurrentZone?.zoneLabel).isEqualTo("Pacific/Auckland")
        assertThat(viewModel.state.blockingDisplayInCurrentZone?.timeLabel).isEqualTo("08:30")
    }

    @Test
    fun anAppliedBlockingHasNoStartLeftToAnnounce() {
        val viewModel = viewModel()

        snapshotPublisher.publish(deferredSnapshot(blockingAppliedAtEpochMillis = paris(2026, 9, 3, 22, 30)))

        assertThat(viewModel.state.isBlockingPending).isFalse()
        assertThat(viewModel.state.blockingDisplayAtActivation).isNull()
        assertThat(viewModel.state.blockingDisplayInCurrentZone).isNull()
    }

    @Test
    fun anImmediateSessionNeverAnnouncesABlockingStart() {
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.isBlockingPending).isFalse()
        assertThat(viewModel.state.blockingDisplayAtActivation).isNull()
    }

    @Test
    fun aTwelveHourDeviceAlsoRendersTheBlockingStartInAmPm() {
        val viewModel = viewModel()

        viewModel.refresh(use24Hour = false)
        snapshotPublisher.publish(deferredSnapshot())

        assertThat(viewModel.state.blockingDisplayAtActivation?.timeLabel).isEqualTo("10:30 PM")
    }

    @Test
    fun everySessionStateHasALabel() {
        SessionStateDto.entries.forEach { state ->
            assertThat(ActiveSessionTexts.stateLabel(state)).isNotEmpty()
        }
    }

    @Test
    fun theFrozenSelectionIsShownWithTheNamesCapturedAtActivation() {
        gateway.result = presentSession(snapshot(), blockedApps)
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.blockedApps.map { it.displayNameSnapshot })
            .containsExactly("Réseau social", "Jeu")
            .inOrder()
    }

    @Test
    fun aSessionWithoutAnySelectedApplicationShowsAnEmptyList() {
        gateway.result = presentSession(snapshot(), blockedPackages = emptyList())
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.blockedApps).isEmpty()
    }

    /**
     * SPEC_ANDROID §13 : un snapshot illisible ne doit pas se présenter comme une session sans
     * application bloquée. L'écran conserve ce qu'il affichait plutôt que d'affirmer « aucune ».
     */
    @Test
    fun anUnreadablePersistenceKeepsTheListAlreadyShown() {
        gateway.result = presentSession(snapshot(), blockedApps)
        val viewModel = viewModel()
        snapshotPublisher.publish(snapshot())

        gateway.result = LoadResult.Unreadable("json")
        viewModel.refresh(use24Hour = true)

        assertThat(viewModel.state.blockedApps).hasSize(2)
    }

    @Test
    fun aDegradedSessionSaysSoWithoutPromisingARecovery() {
        gateway.result = presentSession(snapshot(), blockedApps)
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot().copy(health = SessionHealthDto.DEGRADED))

        assertThat(viewModel.state.health).isEqualTo(SessionHealthDto.DEGRADED)
        assertThat(viewModel.state.isDegraded).isTrue()
    }

    @Test
    fun aHealthySessionIsNotReportedAsDegraded() {
        gateway.result = presentSession(snapshot(), blockedApps)
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.isDegraded).isFalse()
    }

    /** SPEC_CORE_KMP §7.3 : un `CRITICAL` doit être présenté explicitement, donc en tête. */
    @Test
    fun incidentsAreOrderedBySeverityThenByMostRecent() {
        gateway.result = presentSession(snapshot(), blockedApps)
        incidentsReader.incidents =
            listOf(
                incident("TIME_CHANGED", IncidentSeverityDto.WARNING, now - 3_000L),
                incident("RELEASE_PARTIAL_FAILURE", IncidentSeverityDto.DEGRADED, now - 2_000L),
                incident("BLOCKING_PERMISSION_REVOKED", IncidentSeverityDto.CRITICAL, now - 1_000L),
                incident("ALARM_PERMISSION_REVOKED", IncidentSeverityDto.CRITICAL, now),
            )
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.incidents.map { it.code })
            .containsExactly(
                "ALARM_PERMISSION_REVOKED",
                "BLOCKING_PERMISSION_REVOKED",
                "RELEASE_PARTIAL_FAILURE",
                "TIME_CHANGED",
            ).inOrder()
    }

    @Test
    fun criticalIncidentsArePresentedApartFromTheOthers() {
        gateway.result = presentSession(snapshot(), blockedApps)
        incidentsReader.incidents =
            listOf(
                incident("BLOCKING_PERMISSION_REVOKED", IncidentSeverityDto.CRITICAL),
                incident("TIME_CHANGED", IncidentSeverityDto.WARNING),
            )
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.criticalIncidents.map { it.code })
            .containsExactly("BLOCKING_PERMISSION_REVOKED")
    }

    /**
     * SPEC_ANDROID §15, « Remédiation des incidents sur l'écran 7 » : l'action minimale imposée
     * pour `BLOCKING_PERMISSION_REVOKED`. Report de l'étape 15, où l'incident était présenté sans
     * aucun recours.
     */
    @Test
    fun aRemediableIncidentCarriesItsRecourse() {
        gateway.result = presentSession(snapshot(), blockedApps)
        incidentsReader.incidents =
            listOf(incident("BLOCKING_PERMISSION_REVOKED", IncidentSeverityDto.CRITICAL))
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        val presented = viewModel.state.incidents.single()
        assertThat(presented.action).isEqualTo(ReadinessAction.OpenAccessibilitySettings)
        assertThat(presented.actionLabel).isEqualTo("Ouvrir les réglages d'accessibilité")
    }

    /**
     * Mesuré sur appareil à l'étape 16 : après deux morts du processus, le même fait produisait
     * deux incidents `BLOCKING_PERMISSION_REVOKED` en base — la déduplication de
     * `SessionReadinessMonitor` vit en mémoire (étape 12) et ne survit pas au redémarrage. L'écran 7
     * affichait alors deux fois le même texte **et deux boutons identiques**.
     *
     * L'écran 7 présente l'**état** courant, pas l'historique : un code n'y figure qu'une fois, dans
     * sa forme la plus récente. L'historique complet reste sur l'écran 12, dont c'est le rôle.
     */
    @Test
    fun theSameIncidentCodeIsPresentedOnlyOnceEvenIfRecordedTwice() {
        gateway.result = presentSession(snapshot(), blockedApps)
        incidentsReader.incidents =
            listOf(
                incident("BLOCKING_PERMISSION_REVOKED", IncidentSeverityDto.CRITICAL, now - 10_000L),
                incident("BLOCKING_PERMISSION_REVOKED", IncidentSeverityDto.CRITICAL, now),
                incident("TIME_CHANGED", IncidentSeverityDto.WARNING, now - 5_000L),
            )
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.incidents.map { it.code })
            .containsExactly("BLOCKING_PERMISSION_REVOKED", "TIME_CHANGED")
            .inOrder()
        assertThat(viewModel.state.criticalIncidents).hasSize(1)
        // Le plus récent des deux est celui qui reste.
        assertThat(
            viewModel.state.incidents
                .first()
                .incident.occurredAtEpochMillis,
        ).isEqualTo(now)
    }

    /** §15 : « ne jamais afficher un faux état de fiabilité » — donc aucun bouton sans recours. */
    @Test
    fun anIncidentWithoutAnyRecourseExposesNoAction() {
        gateway.result = presentSession(snapshot(), blockedApps)
        incidentsReader.incidents = listOf(incident("TIME_CHANGED", IncidentSeverityDto.WARNING))
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        val presented = viewModel.state.incidents.single()
        assertThat(presented.action).isNull()
        assertThat(presented.actionLabel).isNull()
    }

    /**
     * §13 : un accès aux alarmes exactes perdu n'ouvre aucun réglage — Niumi déclare
     * `USE_EXACT_ALARM`. Le libellé de l'incident porte déjà l'explication ; aucun bouton ne doit
     * promettre une destination qui n'existe pas.
     */
    @Test
    fun theExactAlarmIncidentShowsNoButtonBecauseNoSettingsScreenApplies() {
        gateway.result = presentSession(snapshot(), blockedApps)
        incidentsReader.incidents =
            listOf(incident("ALARM_PERMISSION_REVOKED", IncidentSeverityDto.CRITICAL))
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(
            viewModel.state.incidents
                .single()
                .actionLabel,
        ).isNull()
    }

    /**
     * §15 : « Cette action n'est pas une exception à §3 : elle ne termine ni ne modifie la
     * session. » Le ViewModel n'a ni coordinateur ni façade : il ne **peut** pas dispatcher
     * d'événement, et `FakeSessionPersistenceGateway` fait échouer toute écriture tentée depuis un
     * écran. Le test fige cette propriété — la session reste `ARMED` et le snapshot publié est
     * inchangé après la projection d'un incident remédiable.
     */
    @Test
    fun noRemediationEverCancelsOrModifiesTheSession() {
        val armed = snapshot()
        gateway.result = presentSession(armed, blockedApps)
        incidentsReader.incidents =
            listOf(incident("BLOCKING_PERMISSION_REVOKED", IncidentSeverityDto.CRITICAL))
        val viewModel = viewModel()

        snapshotPublisher.publish(armed)

        assertThat(
            viewModel.state.incidents
                .single()
                .action,
        ).isEqualTo(ReadinessAction.OpenAccessibilitySettings)
        assertThat(viewModel.state.state).isEqualTo(SessionStateDto.ARMED)
        assertThat(snapshotPublisher.snapshot.value).isEqualTo(armed)
    }

    @Test
    fun aSessionWithoutAnyIncidentShowsNone() {
        gateway.result = presentSession(snapshot(), blockedApps)
        val viewModel = viewModel()

        snapshotPublisher.publish(snapshot())

        assertThat(viewModel.state.incidents).isEmpty()
        assertThat(viewModel.state.criticalIncidents).isEmpty()
    }

    /**
     * SPEC_ANDROID §13.1 liste « passage de l'application au premier plan » parmi les
     * déclencheurs de la surveillance. C'est ce qui rend visible un service d'accessibilité coupé
     * pendant que Niumi était en arrière-plan.
     */
    @Test
    fun comingBackToTheForegroundTriggersTheReadinessSurveillance() {
        val viewModel = viewModel()

        viewModel.refresh(use24Hour = true)

        assertThat(readinessTrigger.evaluations).isEqualTo(1)
    }

    @Test
    fun noSessionDoesNotReadThePersistenceAtAll() {
        val viewModel = viewModel()

        viewModel.refresh(use24Hour = true)

        assertThat(viewModel.state.hasSession).isFalse()
        assertThat(viewModel.state.blockedApps).isEmpty()
        assertThat(viewModel.state.incidents).isEmpty()
    }
}
