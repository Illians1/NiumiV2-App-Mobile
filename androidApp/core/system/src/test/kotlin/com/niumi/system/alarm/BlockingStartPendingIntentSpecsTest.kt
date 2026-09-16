package com.niumi.system.alarm

import android.app.PendingIntent
import com.google.common.truth.Truth.assertThat
import com.niumi.system.intent.IntentExtraValue
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.PendingIntentSpec
import org.junit.Test

/**
 * SPEC_ANDROID §9.1 (seconde dérogation), §12.4 (Lot 6) : le `PendingIntent` du début de blocage
 * doit toujours pouvoir être distingué de ceux du réveil **et** du watchdog, pour une même session.
 * Specs pures, aucune classe Android réelle instanciée — même style qu'`AlarmPendingIntentSpecsTest`.
 */
class BlockingStartPendingIntentSpecsTest {
    private val sessionId = "3f8e9a2b-8c1d-4e5f-9a0b-1c2d3e4f5a6b"

    @Test
    fun theBlockingStartTargetsItsOwnReceiverAsAnExplicitBroadcast() {
        val spec = BlockingStartPendingIntentSpecs.blockingStart(sessionId, revision = 2L)

        assertThat(spec.kind).isEqualTo(PendingIntentSpec.Kind.BROADCAST)
        assertThat(spec.target).isEqualTo(NiumiComponent.BLOCKING_START_RECEIVER)
        assertThat(spec.flags and PendingIntent.FLAG_IMMUTABLE).isNotEqualTo(0)
        assertThat(spec.flags and PendingIntent.FLAG_UPDATE_CURRENT).isNotEqualTo(0)
    }

    /**
     * Le point qui compte : trois alarmes coexistent pour une même session (réveil, watchdog, début
     * du blocage), et `AlarmManager.cancel()` sur l'une ne doit jamais pouvoir atteindre les autres.
     * Annuler le réveil en croyant annuler le début du blocage rendrait Niumi muet au matin.
     */
    @Test
    fun theRequestCodeNeverCollidesWithTheWakeAlarmNorTheWatchdogForTheSameSession() {
        val blockingStart = BlockingStartPendingIntentSpecs.blockingStart(sessionId, revision = 1L)
        val wakeAlarm = AlarmPendingIntentSpecs.alarm(sessionId, revision = 1L)
        val watchdog = RingingWatchdogSpecs.pendingIntent(sessionId)

        assertThat(blockingStart.requestCode).isNotEqualTo(wakeAlarm.requestCode)
        assertThat(blockingStart.requestCode).isNotEqualTo(watchdog.requestCode)
    }

    /**
     * `revision` doit être un **nombre** : `BlockingStartReceiver` la relit avec `getLongExtra`, qui
     * renverrait sa valeur par défaut si l'extra était écrit comme texte. La commande serait alors
     * rejetée et le blocage ne commencerait jamais — même défaut que celui mesuré sur le réveil à
     * l'étape 3.
     */
    @Test
    fun revisionExtraIsNumericNotText() {
        val spec = BlockingStartPendingIntentSpecs.blockingStart(sessionId, revision = 7L)

        assertThat(spec.extras["sessionId"]).isEqualTo(IntentExtraValue.Text(sessionId))
        assertThat(spec.extras["revision"]).isEqualTo(IntentExtraValue.Number(7L))
    }

    /**
     * Le code de requête ne dépend **pas** de la révision : `cancel()` et `isScheduled()` doivent
     * retrouver le `PendingIntent` posé par `schedule()` sans connaître la révision qui l'a écrit.
     */
    @Test
    fun theRequestCodeIsStableForAGivenSessionWhateverTheRevision() {
        val first = BlockingStartPendingIntentSpecs.blockingStart(sessionId, revision = 1L)
        val second = BlockingStartPendingIntentSpecs.blockingStart(sessionId, revision = 42L)

        assertThat(first.requestCode).isEqualTo(second.requestCode)
    }

    @Test
    fun differentSessionsProduceDifferentRequestCodes() {
        val other = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"

        val first = BlockingStartPendingIntentSpecs.blockingStart(sessionId, revision = 1L)
        val second = BlockingStartPendingIntentSpecs.blockingStart(other, revision = 1L)

        assertThat(first.requestCode).isNotEqualTo(second.requestCode)
    }
}
