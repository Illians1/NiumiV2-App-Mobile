package com.niumi.system.session.fakes

import com.niumi.system.common.Clock
import com.niumi.system.common.IdGenerator

class FakeClock(
    var now: Long,
) : Clock {
    override fun nowEpochMillis(): Long = now
}

/**
 * UUID canoniques déterministes (`CanonicalUuid.isCanonical` : 8-4-4-4-12 hexadécimal minuscule).
 * Le suffixe commence toujours par `a` : les eventId choisis à la main dans les fixtures de test
 * n'utilisent que des chiffres décimaux (ex. `...0001`, `...0002`) — ce préfixe garantit qu'un
 * événement de suivi généré par `SessionEventFactory` ne collisionne jamais avec eux.
 */
class SequentialIdGenerator : IdGenerator {
    private var counter = 0

    override fun newId(): String {
        val suffix = "a" + (counter++).toString().padStart(11, '0')
        return "00000000-0000-0000-0000-$suffix"
    }
}
