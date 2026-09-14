package com.niumi.system.common

import javax.inject.Qualifier

/**
 * Qualifie le `Context` construit par `@ApplicationContext.createDeviceProtectedStorageContext()`
 * dans [com.niumi.system.di.SystemModule] (SPEC_ANDROID §7.3, §9.3).
 *
 * Utilisé en permanence — avant et après déverrouillage — par tout adaptateur qui doit rester
 * fonctionnel dans un composant `directBootAware` : `AlarmScheduler`, `AndroidPendingIntentFactory`,
 * `ScanRequestNotifier`, `AndroidNotificationChannelRegistrar`, `AndroidSessionWarningNotifier` et
 * `RingingController`. Aucun de ces adaptateurs n'accède au stockage chiffré par les identifiants
 * (`AlarmManager`, `NotificationManager`, `startForegroundService`), donc le contexte protégé se
 * comporte à l'identique après déverrouillage : pas d'aiguillage sur `UnlockState` ni de second
 * graphe de bindings.
 *
 * Ne jamais qualifier ainsi un adaptateur qui lit `SetupPreferences`, `AppSelectionStore` ou Room :
 * ces données vivent légitimement en stockage chiffré par les identifiants (SPEC_ANDROID §7.3,
 * dernier alinéa).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeviceProtected
