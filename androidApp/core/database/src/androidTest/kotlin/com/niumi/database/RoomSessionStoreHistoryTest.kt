package com.niumi.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.IncidentSeverityDto
import com.niumi.core.interop.PlatformDto
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.core.interop.SessionIncidentDto
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * L'historique d'une session survit aux décisions suivantes (SPEC_CORE_KMP §6.1 : « Les reçus sont
 * conservés avec l'historique de session », « les effets interrompus sont remis en attente et
 * rejoués » ; SPEC_ANDROID §7.1 : historique d'incidents du diagnostic).
 *
 * Régression trouvée sur appareil à l'étape 11 : `SessionDao.upsert` était un
 * `@Insert(onConflict = REPLACE)`, or `INSERT OR REPLACE` est un DELETE suivi d'un INSERT en
 * SQLite — il déclenchait les `ForeignKey(onDelete = CASCADE)` des cinq tables enfants et effaçait
 * tout l'historique de la session à *chaque* transition d'état. Aucun test antérieur ne committait
 * deux décisions pour une même session, et les tests JVM du coordinateur passent par des fakes en
 * mémoire, jamais par Room. Voir `ETAPE-11.md`.
 */
@RunWith(AndroidJUnit4::class)
class RoomSessionStoreHistoryTest {
    private lateinit var database: NiumiDatabase
    private lateinit var store: RoomSessionStore
    private val sessionId = "55555555-5555-5555-5555-555555555555"

    @Before
    fun setUp() {
        database = newInMemoryDatabase()
        store = RoomSessionStore(Provider { database }, AlwaysUnlockedState)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun commitRevision(
        revision: Long,
        effectKind: SessionEffectKindDto,
    ) {
        store.commitDecision(
            StoredDecision(
                snapshot = RoomTestFixtures.preparingSnapshot(sessionId, revision = revision),
                receipt = RoomTestFixtures.receipt("event-$revision", sessionId, appliedRevision = revision),
                effects =
                    listOf(
                        RoomTestFixtures.effect(sessionId, revision = revision, ordinal = 0, kind = effectKind),
                    ),
                androidExtras = RoomTestFixtures.extras(),
            ),
        )
    }

    @Test
    fun receiptsOfPreviousRevisionsSurviveTheNextDecision() =
        runTest {
            commitRevision(revision = 1, effectKind = SessionEffectKindDto.SCHEDULE_ALARM)
            commitRevision(revision = 2, effectKind = SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT)
            commitRevision(revision = 3, effectKind = SessionEffectKindDto.CANCEL_ALARM)

            assertThat(store.receipts(sessionId).map { it.eventId })
                .containsExactly("event-1", "event-2", "event-3")
        }

    @Test
    fun pendingEffectsOfPreviousRevisionsSurviveTheNextDecision() =
        runTest {
            commitRevision(revision = 1, effectKind = SessionEffectKindDto.SCHEDULE_ALARM)
            commitRevision(revision = 2, effectKind = SessionEffectKindDto.APPLY_BLOCKING)

            val replayable = store.pendingEffects(sessionId)

            assertThat(replayable.map { it.kind })
                .containsExactly(SessionEffectKindDto.SCHEDULE_ALARM, SessionEffectKindDto.APPLY_BLOCKING)
                .inOrder()
        }

    @Test
    fun recordedIncidentsSurviveTheNextDecision() =
        runTest {
            commitRevision(revision = 1, effectKind = SessionEffectKindDto.SCHEDULE_ALARM)
            store.recordIncident(
                sessionId,
                SessionIncidentDto(
                    code = "ALARM_PERMISSION_REVOKED",
                    severity = IncidentSeverityDto.CRITICAL,
                    occurredAtEpochMillis = 1_700_000_001_000L,
                    platform = PlatformDto.ANDROID,
                ),
            )

            commitRevision(revision = 2, effectKind = SessionEffectKindDto.PUBLISH_PLATFORM_SNAPSHOT)

            assertThat(database.incidentDao().forSession(sessionId).map { it.code })
                .containsExactly("ALARM_PERMISSION_REVOKED")
        }

    /** Le figeage de `boxId`/`ringtoneKey` (`freezeFrom`) doit continuer de fonctionner après la
     * correction de l'upsert : la session est mise à jour, jamais recréée. */
    @Test
    fun blockedAppsAreReplacedNotAccumulatedAcrossRevisions() =
        runTest {
            commitRevision(revision = 1, effectKind = SessionEffectKindDto.SCHEDULE_ALARM)
            commitRevision(revision = 2, effectKind = SessionEffectKindDto.APPLY_BLOCKING)

            val stored = store.activeSession()

            assertThat(stored?.extras?.blockedPackages?.map { it.packageName })
                .containsExactly("com.example.first")
        }
}
