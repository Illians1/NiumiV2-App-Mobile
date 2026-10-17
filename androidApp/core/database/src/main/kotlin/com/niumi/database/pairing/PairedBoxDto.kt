package com.niumi.database.pairing

import com.niumi.core.interop.PairedBoxCredentialDto

/** Un boîtier associé, avec son nom (SPEC_ANDROID §11.1, Lot 8). */
data class PairedBoxDto(
    val credential: PairedBoxCredentialDto,
    val name: String,
)
