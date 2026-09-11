package com.niumi.database.logging.di

import javax.inject.Qualifier

/**
 * Qualifie le `CoroutineScope` de [com.niumi.database.logging.RoomTechnicalEventLog]. Miroir de
 * `com.niumi.system.common.DefaultDispatcher` (`:core:system`, inaccessible ici : `:core:database`
 * ne dépend pas de `:core:system`, SPEC_ANDROID §6). Le scope est fourni par
 * [com.niumi.database.logging.di.LoggingModule], jamais construit dans une classe de production —
 * même règle que le dispatcher qualifié de `:core:system`.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TechnicalEventLogScope
