package com.niumi.app

import com.niumi.feature.session.blocking.AndroidBlockingController
import com.niumi.feature.session.di.SessionBlockingModule
import com.niumi.system.blocking.AccessibilityServiceStatus
import com.niumi.system.blocking.BlockingController
import com.niumi.system.blocking.PersistedBlockedPackagesProjection
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Seul contrôle de §13.1 que l'instrumentation rend structurellement faux : sous instrumentation,
 * `accessibility_enabled` est remis à 0 et le service est débranché (SPEC_ANDROID §19.2). Or
 * `SessionReconciler.reconcileArmed` sort **avant** le début du blocage différé quand
 * `ACCESSIBILITY_SERVICE` échoue : sans cette doublure, un test de réconciliation d'une session
 * `ARMED` ne prouverait rien du tout, la passe s'arrêtant avant d'atteindre ce qu'il observe.
 *
 * Écarté : écrire `accessibility_enabled` et `enabled_accessibility_services` par `settings put
 * secure`. Cela n'achèterait rien — `AndroidAccessibilityServiceStatus` ne lit que ces deux clés,
 * déjà couvertes en JVM — et **lierait réellement** le service : §12.4 lui impose de rejouer
 * `BlockingDecision.decide` sur le dernier package vu quand la projection passe d'inactive à
 * active, donc un test pourrait renvoyer l'instrumentation à l'accueil et poser un overlay au
 * milieu de la passe suivante.
 *
 * **Ce que cette doublure ne prouve pas**, et qui est prouvé ailleurs : la garde de permission
 * elle-même, vérifiée en JVM par
 * `SessionReconcilerBlockingStartTest.aLostAccessibilityPermissionStopsThePassBeforeTheBlockingStart`
 * et sur appareil par la précondition d'accessibilité de `tools/validate_blocking.sh`.
 *
 * [BlockingController] reste le **vrai** [AndroidBlockingController] sur la **vraie**
 * [PersistedBlockedPackagesProjection] : c'est elle que les tests observent, et la remplacer
 * retirerait au test instrumenté ce qu'il apporte de plus que la JVM.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SessionBlockingModule::class])
object TestBlockingBindingsModule {
    @Provides
    @Singleton
    fun provideAccessibilityServiceStatus(): AccessibilityServiceStatus = FakeAccessibilityServiceStatus

    @Provides
    @Singleton
    fun provideBlockingController(
        projection: PersistedBlockedPackagesProjection,
        accessibilityServiceStatus: AccessibilityServiceStatus,
    ): BlockingController = AndroidBlockingController(projection, accessibilityServiceStatus)
}

/**
 * Valeur par défaut `false` : c'est l'état réel sous instrumentation (§19.2), donc les classes de
 * test qui ne touchent pas à ce drapeau se comportent exactement comme avant son introduction.
 * Un test qui le passe à `true` le remet à `false` dans son `@After` — l'objet est partagé par tout
 * le processus d'instrumentation, comme le journal technique et la base.
 */
object FakeAccessibilityServiceStatus : AccessibilityServiceStatus {
    @Volatile
    var enabled: Boolean = false

    override fun isEnabled(): Boolean = enabled
}
