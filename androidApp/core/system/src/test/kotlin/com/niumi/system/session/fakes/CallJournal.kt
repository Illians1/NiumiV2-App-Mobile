package com.niumi.system.session.fakes

/** Journal d'appels partagé par les fakes, pour prouver un ordre (SPEC_CORE_KMP §6 : la
 * persistance précède toujours l'exécution des effets). */
class CallJournal {
    private val entries = mutableListOf<String>()
    val calls: List<String> get() = entries

    fun record(entry: String) {
        entries += entry
    }
}
