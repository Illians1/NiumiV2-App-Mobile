package com.niumi.app

import com.google.common.truth.Truth.assertThat
import com.niumi.core.interop.SessionEffectKindDto
import com.niumi.system.session.EffectExecutor
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Chaque valeur de `SessionEffectKindDto` a un exécuteur lié (SPEC_ANDROID §19.1, Lot 6).
 *
 * **Ce test existe à cause d'un défaut réel.** `EffectDispatcher` lit sa table par `Map.getValue`,
 * qui lève `NoSuchElementException` sur une clé absente — et aucun test JVM ne le voit, chacun
 * montant sa propre table dans `TestCoordinatorHarness`. À l'étape 22, l'ajout de
 * `CANCEL_BLOCKING_START` au contrat commun a fait tomber quinze tests de `:core:system` pour cette
 * seule raison, et il a fallu conditionner l'effet côté moteur en attendant cette étape.
 *
 * Il est instrumenté et vit dans `:app` parce que c'est le seul module dont le graphe Hilt est
 * complet — même raison qu'`AlarmChainInstrumentedTest`. Il n'a besoin d'aucun appareil particulier
 * ni d'aucune permission : il n'exécute aucun effet, il vérifie une table.
 */
@HiltAndroidTest
class EffectExecutorCoverageTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var executors: Map<SessionEffectKindDto, @JvmSuppressWildcards EffectExecutor>

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun everyEffectKindOfTheSharedContractHasABoundExecutor() {
        assertThat(executors.keys).containsExactlyElementsIn(SessionEffectKindDto.entries)
    }
}
