package com.niumi.core.domain

/**
 * Moyen par lequel une session a été libérée (SPEC_CORE_KMP §4, §7.1, contrat 1.5), renseigné à
 * l'entrée dans `RELEASING` : scan d'un Niumi Spot, sortie de secours prouvée (étape 42 du plan de
 * refonte), ou changement de planning avant le coucher (désarmement). Remplace l'ancien invariant
 * « `nfcVerifiedAt` avant `RELEASING` » : `nfcVerifiedAt` existe si et seulement si le moyen est
 * [NFC_SCAN].
 */
public enum class ReleaseMeans {
    NFC_SCAN,
    EMERGENCY_EXIT,
    PLAN_CHANGE,
}
