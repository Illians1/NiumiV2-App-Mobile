package com.niumi.system.plan

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.database.directboot.UnlockState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

/**
 * Complément instrumenté de `DataStoreWakePlanStore` (étape 43) : `niumi_wake_plan` écrit, relu et
 * réémis réellement. Nécessite un appareil ou un émulateur (`connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class DataStoreWakePlanStoreInstrumentedTest {
    private lateinit var store: DataStoreWakePlanStore

    @Before
    fun setUp() =
        runTest {
            store =
                DataStoreWakePlanStore(
                    Provider { ApplicationProvider.getApplicationContext<Context>() },
                    AlwaysUnlocked,
                )
            // Le DataStore est partagé par le processus de test : repartir du défaut.
            store.write(WakePlan.DEFAULT)
        }

    @Test
    fun writeThenReadReturnsTheSamePlan() =
        runTest {
            val plan =
                WakePlan(enabled = true, enabledIsoDays = setOf(6, 7), wakeTimeIso = "09:15", bedtimeIso = "01:00")

            store.write(plan)

            assertThat(store.read()).isEqualTo(plan)
        }

    @Test
    fun noDayCheckedIsKept() =
        runTest {
            store.write(WakePlan.DEFAULT.copy(enabledIsoDays = emptySet()))

            assertThat(store.read().enabledIsoDays).isEmpty()
        }

    @Test
    fun theFlowEmitsTheLatestWrite() =
        runTest {
            store.write(WakePlan.DEFAULT.copy(enabled = true))

            assertThat(store.plan.first()).isEqualTo(WakePlan.DEFAULT.copy(enabled = true))
        }
}

/** Ces tests portent sur le `DataStore`, pas sur la garde de déverrouillage (`DataStoreUnlockGuardTest`). */
private object AlwaysUnlocked : UnlockState {
    override val isUserUnlocked: Boolean = true
}
