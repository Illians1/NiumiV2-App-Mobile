package com.niumi.system.apps

import com.niumi.database.BlockedPackage
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Sérialisation de la sélection courante pour DataStore, qui ne stocke nativement qu'un
 * `Set<String>` — insuffisant pour un couple package/libellé. Le libellé est figé avec le package
 * (SPEC_ANDROID §12.2) : `PackageManager` ne sait plus nommer une application désinstallée ou
 * masquée, et le texte d'overlay imposé afficherait alors un nom de package.
 *
 * Un `MapSerializer` suffit : aucune classe `@Serializable`, donc pas de plugin de sérialisation
 * sur ce module. L'ordre d'insertion est conservé dans les deux sens (`LinkedHashMap`), ce qui
 * garde la sélection affichée telle que l'utilisateur l'a laissée.
 *
 * [decode] ne lève jamais : une préférence illisible (écriture interrompue, rétrogradation)
 * devient une sélection vide, que le diagnostic de §13 signale comme bloquante — jamais une
 * activation avec une sélection fantôme.
 */
object AppSelectionCodec {
    private val json = Json
    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

    fun encode(selection: List<BlockedPackage>): String =
        json.encodeToString(
            mapSerializer,
            selection.associate { it.packageName to it.displayNameSnapshot },
        )

    fun decode(raw: String?): List<BlockedPackage> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json
                .decodeFromString(mapSerializer, raw)
                .map { (packageName, label) -> BlockedPackage(packageName, label) }
        }.getOrDefault(emptyList())
    }
}
