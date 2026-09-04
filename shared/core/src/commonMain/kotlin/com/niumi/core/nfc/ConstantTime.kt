package com.niumi.core.nfc

/**
 * Comparaison d'octets à temps constant, indépendante des valeurs et de l'issue. Utilisée pour
 * comparer des empreintes de token NFC sans exposer d'information par canal auxiliaire.
 */
public object ConstantTime {
    /**
     * Vrai si et seulement si [a] et [b] ont le même contenu. Toujours parcourt la longueur
     * maximale des deux tableaux, sans retour anticipé sur une longueur différente, afin que la
     * durée d'exécution ne dépende pas de la position du premier octet différent ni de l'égalité
     * des longueurs.
     */
    public fun equals(
        a: ByteArray,
        b: ByteArray,
    ): Boolean {
        val maxLength = if (a.size > b.size) a.size else b.size
        var diff = a.size xor b.size
        for (i in 0 until maxLength) {
            val byteA = if (i < a.size) a[i].toInt() else 0
            val byteB = if (i < b.size) b[i].toInt() else 0
            diff = diff or (byteA xor byteB)
        }
        return diff == 0
    }
}
