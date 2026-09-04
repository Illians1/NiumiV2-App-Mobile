@file:OptIn(ExperimentalUnsignedTypes::class)

package com.niumi.core.nfc

/**
 * Implémentation Kotlin pure de SHA-256 (FIPS 180-4). Aucune API de plateforme : la même
 * implémentation sert Android et iOS depuis `commonMain`. Les types non signés (`UInt`,
 * `UIntArray`) sont encore marqués expérimentaux par la stdlib bien que stables en pratique;
 * l'opt-in est local à ce fichier.
 *
 * `MagicNumber` est supprimé pour l'ensemble de l'objet : les décalages de bits (8, 16, 24…), le
 * masque d'octet `0xff` et l'octet de bourrage `0x80` sont les constantes de la norme FIPS 180-4
 * elle-même — les nommer (`BYTE_MASK`, `SHIFT_BYTE_0`…) n'ajouterait aucune clarté par rapport à
 * la lecture directe de l'algorithme de référence.
 */
@Suppress("MagicNumber")
public object Sha256 {
    private const val BLOCK_BYTES = 64
    private const val WORD_COUNT = 64
    private const val HASH_WORD_COUNT = 8
    private const val LENGTH_SUFFIX_BYTES = 8

    private val initialHash =
        uintArrayOf(
            0x6a09e667u,
            0xbb67ae85u,
            0x3c6ef372u,
            0xa54ff53au,
            0x510e527fu,
            0x9b05688cu,
            0x1f83d9abu,
            0x5be0cd19u,
        )

    private val roundConstants =
        uintArrayOf(
            0x428a2f98u,
            0x71374491u,
            0xb5c0fbcfu,
            0xe9b5dba5u,
            0x3956c25bu,
            0x59f111f1u,
            0x923f82a4u,
            0xab1c5ed5u,
            0xd807aa98u,
            0x12835b01u,
            0x243185beu,
            0x550c7dc3u,
            0x72be5d74u,
            0x80deb1feu,
            0x9bdc06a7u,
            0xc19bf174u,
            0xe49b69c1u,
            0xefbe4786u,
            0x0fc19dc6u,
            0x240ca1ccu,
            0x2de92c6fu,
            0x4a7484aau,
            0x5cb0a9dcu,
            0x76f988dau,
            0x983e5152u,
            0xa831c66du,
            0xb00327c8u,
            0xbf597fc7u,
            0xc6e00bf3u,
            0xd5a79147u,
            0x06ca6351u,
            0x14292967u,
            0x27b70a85u,
            0x2e1b2138u,
            0x4d2c6dfcu,
            0x53380d13u,
            0x650a7354u,
            0x766a0abbu,
            0x81c2c92eu,
            0x92722c85u,
            0xa2bfe8a1u,
            0xa81a664bu,
            0xc24b8b70u,
            0xc76c51a3u,
            0xd192e819u,
            0xd6990624u,
            0xf40e3585u,
            0x106aa070u,
            0x19a4c116u,
            0x1e376c08u,
            0x2748774cu,
            0x34b0bcb5u,
            0x391c0cb3u,
            0x4ed8aa4au,
            0x5b9cca4fu,
            0x682e6ff3u,
            0x748f82eeu,
            0x78a5636fu,
            0x84c87814u,
            0x8cc70208u,
            0x90befffau,
            0xa4506cebu,
            0xbef9a3f7u,
            0xc67178f2u,
        )

    /** Calcule l'empreinte SHA-256 de [message], sur 32 octets. */
    public fun hash(message: ByteArray): ByteArray {
        val padded = pad(message)
        val h = initialHash.copyOf()
        val w = UIntArray(WORD_COUNT)

        var offset = 0
        while (offset < padded.size) {
            fillMessageSchedule(padded, offset, w)
            compress(h, w)
            offset += BLOCK_BYTES
        }

        return wordsToBytes(h)
    }

    /** Représentation hexadécimale minuscule de [bytes]. */
    public fun hexOf(bytes: ByteArray): String {
        val digits = "0123456789abcdef"
        val builder = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val value = byte.toInt() and 0xff
            builder.append(digits[value ushr 4])
            builder.append(digits[value and 0x0f])
        }
        return builder.toString()
    }

    private fun pad(message: ByteArray): ByteArray {
        val bitLength = message.size.toULong() * 8u
        val withOneBit = message.size + 1
        val zeroPadding = ((BLOCK_BYTES - LENGTH_SUFFIX_BYTES) - withOneBit % BLOCK_BYTES + BLOCK_BYTES) % BLOCK_BYTES
        val totalSize = withOneBit + zeroPadding + LENGTH_SUFFIX_BYTES

        val padded = ByteArray(totalSize)
        message.copyInto(padded)
        padded[message.size] = 0x80.toByte()
        for (i in 0 until LENGTH_SUFFIX_BYTES) {
            val shift = (LENGTH_SUFFIX_BYTES - 1 - i) * 8
            padded[totalSize - LENGTH_SUFFIX_BYTES + i] = ((bitLength shr shift) and 0xffu).toByte()
        }
        return padded
    }

    private fun fillMessageSchedule(
        padded: ByteArray,
        blockOffset: Int,
        w: UIntArray,
    ) {
        for (t in 0 until 16) {
            val byteOffset = blockOffset + t * 4
            w[t] =
                (padded[byteOffset].toUInt() and 0xffu shl 24) or
                (padded[byteOffset + 1].toUInt() and 0xffu shl 16) or
                (padded[byteOffset + 2].toUInt() and 0xffu shl 8) or
                (padded[byteOffset + 3].toUInt() and 0xffu)
        }
        for (t in 16 until WORD_COUNT) {
            val s0 = w[t - 15].rotateRight(7) xor w[t - 15].rotateRight(18) xor (w[t - 15] shr 3)
            val s1 = w[t - 2].rotateRight(17) xor w[t - 2].rotateRight(19) xor (w[t - 2] shr 10)
            w[t] = w[t - 16] + s0 + w[t - 7] + s1
        }
    }

    private fun compress(
        h: UIntArray,
        w: UIntArray,
    ) {
        var a = h[0]
        var b = h[1]
        var c = h[2]
        var d = h[3]
        var e = h[4]
        var f = h[5]
        var g = h[6]
        var hh = h[7]

        for (t in 0 until WORD_COUNT) {
            val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = hh + s1 + ch + roundConstants[t] + w[t]
            val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = s0 + maj

            hh = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }

        h[0] += a
        h[1] += b
        h[2] += c
        h[3] += d
        h[4] += e
        h[5] += f
        h[6] += g
        h[7] += hh
    }

    private fun wordsToBytes(h: UIntArray): ByteArray {
        val out = ByteArray(HASH_WORD_COUNT * 4)
        for (i in 0 until HASH_WORD_COUNT) {
            out[i * 4] = (h[i] shr 24).toByte()
            out[i * 4 + 1] = (h[i] shr 16).toByte()
            out[i * 4 + 2] = (h[i] shr 8).toByte()
            out[i * 4 + 3] = h[i].toByte()
        }
        return out
    }
}
