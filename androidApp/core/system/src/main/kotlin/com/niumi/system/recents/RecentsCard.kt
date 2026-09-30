package com.niumi.system.recents

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context

/**
 * La carte de Niumi dans le panneau des applications récentes (SPEC_ANDROID §15 ; étape 25).
 *
 * **Mesuré le 2026-09-28** sur Xiaomi 25080RABDG / Android 16 / HyperOS OS3.0.302.0 : balayer cette
 * carte déclenche `SwipeUpClean`, qui tue le processus **bien qu'il soit maintenu à `adj 200`** par le
 * service d'accessibilité lié ; HyperOS ne relie ensuite plus le service tant que l'utilisateur ne l'a
 * pas réactivé à la main. Le blocage était coupé par un geste ordinaire — même involontaire, le
 * 27/09 —, sans rien qui le signale avant la relance suivante de Niumi. Le verrou de HyperOS sur la
 * carte (cadenas) **ne protège pas** d'un balayage individuel ; en revanche, un processus sans carte
 * a survécu au « Tout effacer » du même panneau.
 *
 * D'où la parade décidée avec l'utilisateur : pendant une session, Niumi retire sa propre carte, et
 * la remet quand la session se termine. Pas de carte, pas de balayage possible.
 */
fun interface RecentsCard {
    fun setHidden(hidden: Boolean)
}

/**
 * Ne touche qu'à la tâche de l'activité principale : celle de l'écran de réveil est déjà exclue des
 * récents par son manifeste, et un `setHidden(false)` l'y réintroduirait.
 */
class AndroidRecentsCard(
    private val context: Context,
    private val mainActivity: ComponentName,
) : RecentsCard {
    override fun setHidden(hidden: Boolean) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.appTasks
            .filter { RecentsTasks.isMainTask(it.taskInfo?.baseActivity?.className, mainActivity.className) }
            .forEach { it.setExcludeFromRecents(hidden) }
    }
}

/**
 * Règle pure, testable en JVM : ni `ActivityManager.AppTask` ni `ComponentName` ne sont utilisables
 * hors appareil, d'où des noms de classe.
 */
object RecentsTasks {
    fun isMainTask(
        baseActivityClassName: String?,
        mainActivityClassName: String,
    ): Boolean = baseActivityClassName == mainActivityClassName
}
