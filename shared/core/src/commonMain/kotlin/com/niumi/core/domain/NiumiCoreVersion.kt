package com.niumi.core.domain

/**
 * Version du schéma de données échangé par la façade [com.niumi.core.interop.NiumiCoreFacade].
 * Incrémentée à chaque changement incompatible des DTO exposés aux plateformes natives. Passée à
 * `2` par le contrat 1.3 (blocage différé) : les DTO gagnent des champs, deux valeurs d'enum
 * apparaissent et la façade une septième méthode. Les valeurs par défaut Kotlin ne traversent pas
 * la frontière Swift, la rupture est donc réelle pour iOS.
 */
public object NiumiCoreVersion {
    public const val SCHEMA_VERSION: Int = 2
}
