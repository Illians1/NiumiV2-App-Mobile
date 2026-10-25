package com.niumi.designsystem.component

/**
 * Répartition des emplacements d'icônes : [shown] icônes, puis une case « +[hidden] » si [hidden]
 * n'est pas nul.
 */
data class AppSlotLayout(
    val shown: Int,
    val hidden: Int,
)

/** Six emplacements au plus (`design-plan/SPECIFICATION.md` §8, décision du 2026-10-22). */
const val MAX_APP_SLOTS = 6

/**
 * Nombre d'emplacements de [slot] séparés de [gap] qui tiennent dans [width], entre un et
 * [MAX_APP_SLOTS]. Toutes les mesures dans la même unité.
 */
fun appSlotCapacity(
    width: Float,
    slot: Float,
    gap: Float,
): Int = ((width + gap) / (slot + gap)).toInt().coerceIn(1, MAX_APP_SLOTS)

/**
 * Règle de l'écran 4, reprise par l'accueil (étape 45) : toutes les icônes si elles tiennent ;
 * sinon, toutes les cases sauf la dernière, qui compte les autres.
 */
fun appSlotLayout(
    count: Int,
    capacity: Int,
): AppSlotLayout =
    if (count <=
        capacity
    ) {
        AppSlotLayout(shown = count, hidden = 0)
    } else {
        AppSlotLayout(
            shown = capacity - 1,
            hidden =
                count - (capacity - 1),
        )
    }
