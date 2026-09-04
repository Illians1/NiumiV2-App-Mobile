package com.niumi.core.domain

/**
 * Version du schéma de données échangé par la façade [com.niumi.core.interop.NiumiCoreFacade].
 * Incrémentée à chaque changement incompatible des DTO exposés aux plateformes natives.
 */
public object NiumiCoreVersion {
    public const val SCHEMA_VERSION: Int = 1
}
