package com.niumi.system.readiness

/**
 * Déclencheur « passage de l'application au premier plan » de SPEC_ANDROID §13.1. Interface
 * plutôt qu'appel direct à [SessionReadinessWatcher] : celui-ci enregistre un `BroadcastReceiver`
 * et possède son propre scope, ce qui le rend inutilisable en test JVM, alors que l'écran de
 * session active — son seul appelant de production — doit pouvoir prouver qu'il le déclenche.
 *
 * Ajoutée à l'étape 15, en même temps que le premier appelant de ce déclencheur : il n'en avait
 * aucun depuis l'étape 12.
 */
fun interface ForegroundReadinessTrigger {
    fun evaluateAsync()
}
