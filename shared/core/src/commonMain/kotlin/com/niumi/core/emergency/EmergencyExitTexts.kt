package com.niumi.core.emergency

/**
 * Textes à recopier pour la sortie de secours (`SPECIFICATION.md` §13, contrat 1.5). Écart assumé à
 * SPEC_CORE_KMP §7.3 (« pas de texte affiché dans le module commun ») : le texte est un élément de
 * protocole partagé — la recopie se vérifie contre lui —, documenté en §12. Un seul texte est
 * validé (D8, décision du 2026-10-24) : la liste est prête à en recevoir d'autres, mais tant qu'il
 * est seul, le texte ne change pas d'une sortie à l'autre. Apostrophes typographiques (D13).
 */
public object EmergencyExitTexts {
    private val texts: Map<String, String> =
        linkedMapOf(
            "v1-01" to
                "J’avais décidé de bloquer mes applications jusqu’au scan de mon Niumi Spot. " +
                "Je choisis de sortir de cette session sans le scanner. " +
                "Je recopie ce texte en entier pour confirmer que c’est mon choix.",
        )

    public val ids: List<String> = texts.keys.toList()

    public fun text(id: String): String? = texts[id]

    public fun charCount(id: String): Int? = texts[id]?.length
}
