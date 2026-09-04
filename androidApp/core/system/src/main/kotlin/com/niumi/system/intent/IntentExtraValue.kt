package com.niumi.system.intent

/**
 * Valeur typée d'un extra d'`Intent`. Un `Map<String, String>` ne suffit pas : un extra écrit
 * comme chaîne et relu avec `getLongExtra` renvoie silencieusement la valeur par défaut, sans
 * exception ni avertissement. Ce défaut a réellement empêché le service de sonnerie de démarrer
 * (alarme déclenchée, receiver exécuté, commande rejetée en silence — voir `ETAPE-03.md`) ;
 * le type porté jusqu'à `putExtra` rend cette classe d'erreur impossible.
 */
sealed interface IntentExtraValue {
    data class Text(
        val value: String,
    ) : IntentExtraValue

    data class Number(
        val value: Long,
    ) : IntentExtraValue
}
