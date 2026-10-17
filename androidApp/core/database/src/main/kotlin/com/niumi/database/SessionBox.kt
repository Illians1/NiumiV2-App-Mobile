package com.niumi.database

/**
 * Un boîtier de la liste figée dans une session à l'activation (SPEC_ANDROID §7.2, Lot 8) :
 * `boxId` et `tokenSha256Hex` comme [com.niumi.core.interop.PairedBoxCredentialDto], `name` pour
 * l'export du diagnostic (§17) et le récapitulatif — jamais lu par le vérificateur commun.
 */
data class SessionBox(
    val boxId: String,
    val tokenSha256Hex: String,
    val name: String,
)
