package com.niumi.designsystem.component

/**
 * Les trois aspects d'un champ de l'accueil (pastille de jour, rangée de liste),
 * `design-plan/SPECIFICATION.md` §9. Décision de Mehdi du 2026-10-20 (étape 34).
 */
enum class NiumiFieldState {
    /** Réveil prévu : aspect normal, modifiable (maquette 14). */
    Normal,

    /** Réveil désactivé : affiché en grisé, mais modifiable (maquette 13). */
    Greyed,

    /**
     * Session en cours : grisé et verrouillé (maquette 15). L'action du champ n'est jamais
     * appelée ; un toucher ne peut que signaler « Modifiable après avoir scanné ton Niumi Point. »
     * (forme du message ouverte, étape 45).
     */
    Locked,
}
