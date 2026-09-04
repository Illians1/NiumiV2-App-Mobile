package com.niumi.system.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SPEC_ANDROID §10.3 : la vraie `Notification` produite par [RingingNotificationFactory] est
 * `CATEGORY_ALARM`, `ongoing`, et surtout **sans aucune action** — le complément instrumenté du
 * garde-fou déjà couvert en JVM par `RingingNotificationSpecsTest` sur le spec pur. Nécessite
 * un appareil ou un émulateur (`connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class RingingNotificationFactoryInstrumentedTest {
    @Test
    fun realNotificationHasNoActionAndIsOngoingAlarmCategory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AndroidNotificationChannelRegistrar(context).registerAll()
        // L'icône réelle vit dans :core:designsystem, hors du graphe de dépendances de
        // :core:system : une icône de plateforme suffit ici, seule compte la présence d'un
        // identifiant valide (son absence fait rejeter la notification par le système).
        val factory =
            RingingNotificationFactory(context) { android.R.drawable.ic_lock_idle_alarm }
        val fullScreenIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, RingingNotificationFactoryInstrumentedTest::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )

        val notification = factory.create(fullScreenIntent)

        assertThat(notification.category).isEqualTo(Notification.CATEGORY_ALARM)
        assertThat(notification.actions).isNull()
        assertThat(notification.flags and Notification.FLAG_ONGOING_EVENT).isNotEqualTo(0)
    }
}
