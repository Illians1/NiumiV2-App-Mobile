package com.niumi.system.apps

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.database.BlockedPackage
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SPEC_ANDROID §12.1 : la sélection courante survit au processus. Complément instrumenté de
 * `AppSelectionCodecTest`, qui ne couvre que la sérialisation : ici, DataStore écrit et relit
 * réellement. Nécessite un appareil ou un émulateur (`connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class DataStoreAppSelectionStoreInstrumentedTest {
    private lateinit var store: DataStoreAppSelectionStore

    @Before
    fun setUp() =
        runTest {
            store = DataStoreAppSelectionStore(ApplicationProvider.getApplicationContext<Context>())
            // Le DataStore est partagé par le processus de test : repartir d'une sélection vide.
            store.replace(emptyList())
        }

    @Test
    fun anEmptyStoreReportsNoSelection() =
        runTest {
            assertThat(store.selection()).isEmpty()
            assertThat(store.selectedCount()).isEqualTo(0)
        }

    @Test
    fun replaceThenSelectionReturnsTheSamePackagesAndLabels() =
        runTest {
            val selection =
                listOf(
                    BlockedPackage("com.example.chat", "Chat"),
                    BlockedPackage("com.example.social", "Réseau social"),
                )

            store.replace(selection)

            assertThat(store.selection()).containsExactlyElementsIn(selection).inOrder()
            assertThat(store.selectedCount()).isEqualTo(2)
        }

    @Test
    fun replaceOverwritesThePreviousSelectionInsteadOfMerging() =
        runTest {
            store.replace(listOf(BlockedPackage("com.example.first", "Première")))

            store.replace(listOf(BlockedPackage("com.example.second", "Seconde")))

            assertThat(store.selection()).containsExactly(BlockedPackage("com.example.second", "Seconde"))
        }
}
