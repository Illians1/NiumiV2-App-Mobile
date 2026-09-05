package com.niumi.app.poc

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.niumi.core.interop.PairedBoxCredentialDto
import com.niumi.system.pairing.PairedBoxStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pocPairedBoxDataStore by preferencesDataStore(name = "poc_paired_box")

/**
 * `PairedBoxStore` de la route POC (debug uniquement, SPEC_ANDROID §22 Lot 0). Implémentation
 * Room à l'étape 13 ; celle-ci ne stocke que ce que SPEC_ANDROID §11.1 autorise à persister :
 * `boxId` et l'empreinte SHA-256 du token, jamais le token en clair.
 */
@Singleton
class DebugPairedBoxStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : PairedBoxStore {
        override suspend fun current(): PairedBoxCredentialDto? {
            val preferences =
                context.pocPairedBoxDataStore.data
                    .catch { error ->
                        // Une IOException locale (fichier corrompu) ne doit jamais faire planter
                        // l'appelant : traitée comme « aucun boîtier associé ».
                        if (error is IOException) emit(emptyPreferences()) else throw error
                    }.first()
            val boxId = preferences[KEY_BOX_ID]
            val tokenSha256Hex = preferences[KEY_TOKEN_SHA256_HEX]
            val protocolVersion = preferences[KEY_PROTOCOL_VERSION]
            return if (boxId != null && tokenSha256Hex != null && protocolVersion != null) {
                PairedBoxCredentialDto(protocolVersion, boxId, tokenSha256Hex)
            } else {
                null
            }
        }

        override suspend fun replace(credential: PairedBoxCredentialDto) {
            context.pocPairedBoxDataStore.edit { preferences ->
                preferences[KEY_BOX_ID] = credential.boxId
                preferences[KEY_TOKEN_SHA256_HEX] = credential.tokenSha256Hex
                preferences[KEY_PROTOCOL_VERSION] = credential.protocolVersion
            }
        }

        override suspend fun clear() {
            context.pocPairedBoxDataStore.edit { it.clear() }
        }

        private companion object {
            val KEY_BOX_ID = stringPreferencesKey("box_id")
            val KEY_TOKEN_SHA256_HEX = stringPreferencesKey("token_sha256_hex")
            val KEY_PROTOCOL_VERSION = intPreferencesKey("protocol_version")
        }
    }
