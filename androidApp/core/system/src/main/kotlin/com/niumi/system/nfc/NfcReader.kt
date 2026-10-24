package com.niumi.system.nfc

import android.app.Activity
import com.niumi.system.common.OperationResult

/**
 * Lecteur NFC en Reader Mode (SPEC_ANDROID §4.4, §11.2). Reçoit l'URI brute d'un tag lu et la
 * transmet telle quelle : aucune décision de validité n'est prise ici, seul le parseur de
 * `:shared:core` en décide.
 *
 * Déviation du plan MVP (« Interfaces transverses ») : `onUnreadable` est ajouté à `start()`.
 * Le plan ne prévoyait que `onUri`, mais SPEC_ANDROID §11.2 impose un texte dédié
 * (« Boîtier non reconnu. Réessaie. ») pour un tag physiquement illisible (pas de NDEF,
 * `IOException`, `FormatException`) — un cas qu'aucune URI ne peut représenter. Consigné dans
 * ETAPE-04.md et répercuté sur le plan MVP.
 *
 * Étape 38 de la refonte : `onForeignTag` sépare le tag qui ne porte pas de Niumi Spot (ni NDEF ni
 * URI, « Ce n'est pas un Niumi Spot. ») de la lecture ratée (`IOException`, `FormatException`,
 * « Ton Niumi Spot n'a pas pu être lu. »). Par défaut il rejoint `onUnreadable`, comme avant.
 */
public interface NfcReader {
    public fun start(
        activity: Activity,
        onUri: (String) -> Unit,
        onUnreadable: () -> Unit,
        onForeignTag: () -> Unit = onUnreadable,
    ): OperationResult

    public fun stop(activity: Activity)

    public val availability: NfcAvailability
}
