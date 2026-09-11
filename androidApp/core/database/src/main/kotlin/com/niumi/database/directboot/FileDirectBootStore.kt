package com.niumi.database.directboot

import android.content.Context
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException
import javax.inject.Inject

private const val SNAPSHOT_FILE_NAME = "niumi_session.json"

/**
 * Implémentation [DirectBootStore] (SPEC_ANDROID §7.3) : le fichier vit sous
 * `createDeviceProtectedStorageContext().filesDir`, accessible avant le premier déverrouillage
 * après un redémarrage, contrairement à Room. Écriture atomique via `android.util.AtomicFile`
 * (fichier temporaire + renommage, gérée par le framework — pas de logique à réimplémenter) ;
 * lecture tolérante : jamais d'exception ne traverse [read], une corruption devient
 * [DirectBootSnapshot.Corrupted] sans jamais effacer le fichier existant (SPEC_CORE_KMP §13).
 */
class FileDirectBootStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : DirectBootStore {
        private val file =
            AtomicFile(File(context.createDeviceProtectedStorageContext().filesDir, SNAPSHOT_FILE_NAME))

        override fun read(): DirectBootSnapshot? {
            if (!file.baseFile.exists()) return null
            return try {
                val json = file.readFully().decodeToString()
                directBootJson.decodeFromString(DirectBootSnapshot.Active.serializer(), json)
            } catch (exception: IOException) {
                DirectBootSnapshot.Corrupted(exception.message ?: "DIRECT_BOOT_IO_ERROR")
            } catch (exception: SerializationException) {
                DirectBootSnapshot.Corrupted(exception.message ?: "DIRECT_BOOT_MALFORMED_JSON")
            }
        }

        override fun write(snapshot: DirectBootSnapshot.Active): DirectBootWriteResult {
            val decision = decideWrite(read(), snapshot)
            if (decision != DirectBootWriteResult.Written) return decision

            val stream = file.startWrite()
            return try {
                stream.write(
                    directBootJson.encodeToString(DirectBootSnapshot.Active.serializer(), snapshot).toByteArray(),
                )
                file.finishWrite(stream)
                DirectBootWriteResult.Written
            } catch (exception: IOException) {
                file.failWrite(stream)
                DirectBootWriteResult.Failed(exception.message ?: "DIRECT_BOOT_WRITE_FAILED")
            }
        }

        override fun clear() {
            file.delete()
        }
    }
