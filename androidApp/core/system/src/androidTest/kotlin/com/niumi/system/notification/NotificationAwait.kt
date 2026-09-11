package com.niumi.system.notification

/**
 * Attend qu'une lecture de `NotificationManager.activeNotifications` satisfasse [until].
 *
 * `notify()` et `cancel()` ne sont pas appliqués de façon synchrone : ils confient le travail à
 * `NotificationManagerService`, qui l'empile sur son propre handler et rend la main avant de
 * l'avoir appliqué, tandis qu'`activeNotifications` lit l'état déjà appliqué. Lire juste après
 * avoir écrit peut donc observer l'état d'avant — c'est inhérent à l'API, pas propre à un
 * constructeur, et c'est ce qui rendait ces tests intermittents (mesuré à l'étape 13 : sur six
 * exécutions, deux en échec, avec un test différent à chaque fois).
 *
 * Le sondage est borné et rapide dans le cas nominal : le premier tour suffit presque toujours,
 * donc aucun ralentissement mesurable. Passé [timeoutMillis], l'échec porte [description] et le
 * dernier état observé, plutôt qu'une assertion cryptique sur une liste vide.
 *
 * Les assertions d'**absence** passent aussi par ici : « plus aucune notification » est une
 * condition à atteindre, pas un état immédiat.
 *
 * Même motif que `waitForRingingService` dans `:feature:ringing` (étape 3).
 */
internal fun <T> awaitNotifications(
    description: String,
    timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    read: () -> T,
    until: (T) -> Boolean,
): T {
    val deadline = System.currentTimeMillis() + timeoutMillis
    var observed = read()
    while (!until(observed) && System.currentTimeMillis() < deadline) {
        Thread.sleep(POLL_INTERVAL_MILLIS)
        observed = read()
    }
    if (!until(observed)) {
        throw AssertionError(
            "Condition jamais atteinte en $timeoutMillis ms — attendu : $description. " +
                "Dernier état observé : $observed",
        )
    }
    return observed
}

private const val DEFAULT_TIMEOUT_MILLIS = 2_000L
private const val POLL_INTERVAL_MILLIS = 25L
