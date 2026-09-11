package com.niumi.database.directboot

import android.content.Context
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Indique si l'utilisateur a déjà déverrouillé l'appareil depuis le dernier redémarrage
 * (SPEC_ANDROID §7.3) : Room et tout dépôt qui l'ouvre restent inaccessibles avant ce moment. Une
 * abstraction plutôt qu'un appel direct à `UserManager` : substituable en test (`RoomSessionStore`,
 * `UnlockAwareTechnicalEventLog`).
 */
interface UnlockState {
    val isUserUnlocked: Boolean
}

class UserManagerUnlockState
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : UnlockState {
        private val userManager get() = context.getSystemService(Context.USER_SERVICE) as UserManager

        override val isUserUnlocked: Boolean
            get() = userManager.isUserUnlocked
    }
