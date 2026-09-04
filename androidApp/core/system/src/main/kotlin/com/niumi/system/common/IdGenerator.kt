package com.niumi.system.common

import java.util.UUID

/** Générateur d'identifiants : UUID v4 canonique minuscule (« Interfaces transverses »). */
interface IdGenerator {
    fun newId(): String
}

class UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
