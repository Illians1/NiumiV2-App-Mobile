package com.niumi.feature.ringing

import android.content.ComponentName
import android.content.Context
import com.niumi.system.intent.NiumiComponent
import com.niumi.system.intent.NiumiComponentResolver
import com.niumi.system.notification.NotificationIconResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * L'APK de test instrumenté d'un module `library` est sa propre application : il ne contient
 * pas `:app`, donc pas `AppModule`, donc aucune liaison pour [NiumiComponentResolver] — dont
 * `SystemModule` a besoin pour construire les `PendingIntent`. Ce module comble ce trou, dans
 * `androidTest` uniquement (CLAUDE.md : les doublures restent dans les tests).
 *
 * Les deux composants de `:feature:ringing` sont résolus réellement : c'est `ALARM_ACTIVITY`
 * que `AlarmRingingService` utilise pour son full-screen intent, donc la partie qui compte ici
 * est bien la vraie. `MAIN_ACTIVITY` vit dans `:app`, hors de portée : elle pointe vers
 * `AlarmActivity` faute de mieux, et n'est empruntée par aucun test de ce module.
 */
@Module
@InstallIn(SingletonComponent::class)
object TestComponentResolverModule {
    @Provides
    @Singleton
    fun provideNiumiComponentResolver(
        @ApplicationContext context: Context,
    ): NiumiComponentResolver =
        NiumiComponentResolver { component ->
            when (component) {
                NiumiComponent.ALARM_RECEIVER -> ComponentName(context, AlarmReceiver::class.java)
                NiumiComponent.ALARM_ACTIVITY -> ComponentName(context, AlarmActivity::class.java)
                NiumiComponent.MAIN_ACTIVITY -> ComponentName(context, AlarmActivity::class.java)
            }
        }

    /**
     * Même trou de graphe : la vraie icône vit dans `:core:designsystem`, résolue par `:app`,
     * absent de l'APK de test. On fournit l'icône réelle : `:feature:ringing` dépend bien de
     * `:core:designsystem`, donc l'asset packagé dans l'APK de test est exactement celui de
     * production — ce qui compte ici, puisque c'est son absence qui faisait échouer
     * `startForeground()`.
     */
    @Provides
    @Singleton
    fun provideNotificationIconResolver(): NotificationIconResolver =
        NotificationIconResolver { com.niumi.designsystem.R.drawable.ic_niumi_notification }
}
