package com.niumi.app.help

import com.google.common.truth.Truth.assertThat
import com.niumi.feature.setup.onboarding.OnboardingTexts
import org.junit.Test
import java.io.File

/**
 * L'écran 13 « Aide et limites » (SPEC_ANDROID §15, étape 21) reprend `docs/android/LIMITES.md`.
 * Le plan MVP l'écrit ainsi, et `PRIVACY_POLICY.md` y renvoie le lecteur de la fiche Play : les
 * deux ne peuvent pas diverger sans que l'un des deux mente.
 *
 * Ce test en fait une propriété vérifiée plutôt qu'une intention : le document est relu à chaque
 * build et comparé mot pour mot à [HelpTexts]. `niumi.rootDir` est injecté par le build de
 * `:app` (même mécanisme que `ModuleListTest`).
 */
class HelpTextsTest {
    private val limitesMarkdown: List<String> =
        File(
            requireNotNull(System.getProperty("niumi.rootDir")) {
                "La propriété système niumi.rootDir n'a pas été injectée par le build Gradle."
            },
            "docs/android/LIMITES.md",
        ).readLines()

    @Test
    fun theHelpScreenCarriesExactlyTheSectionsOfTheDocument() {
        val documentSections = limitesMarkdown.filter { it.startsWith("## ") }.map { it.removePrefix("## ") }

        assertThat(HelpTexts.sections.map { it.title }).containsExactlyElementsIn(documentSections).inOrder()
    }

    @Test
    fun theHelpScreenCarriesExactlyTheBulletsOfTheDocument() {
        val documentBullets = limitesMarkdown.filter { it.startsWith("- ") }.map { it.removePrefix("- ") }

        assertThat(HelpTexts.sections.flatMap { it.items }).containsExactlyElementsIn(documentBullets).inOrder()
    }

    /**
     * L'onboarding énonce six limites avant la première session (§4.5, §19.1). L'aide les reprend
     * **mot pour mot** : deux formulations différentes du même fait laisseraient l'utilisateur
     * croire à deux règles distinctes, et §15 interdit tout état de fiabilité ambigu.
     */
    @Test
    fun everyLimitOfTheOnboardingIsRepeatedWordForWord() {
        val helpItems = HelpTexts.sections.flatMap { it.items }

        assertThat(helpItems).containsAtLeastElementsIn(OnboardingTexts.limits)
    }

    /** §15 : tutoiement partout. Le vouvoiement se glisse par recopie d'une source externe. */
    @Test
    fun theHelpScreenNeverSwitchesToTheFormalAddress() {
        val texts = HelpTexts.sections.flatMap { it.items } + HelpTexts.sections.map { it.title } + HelpTexts.INTRO

        val formal = texts.filter { FORMAL_ADDRESS.containsMatchIn(it) }

        assertThat(formal).isEmpty()
    }

    /** L'aide est une consultation : elle ne promet rien qu'une session puisse démentir. */
    @Test
    fun theHelpScreenIsNotEmpty() {
        assertThat(HelpTexts.sections).isNotEmpty()
        assertThat(HelpTexts.sections.all { it.items.isNotEmpty() }).isTrue()
    }

    private companion object {
        val FORMAL_ADDRESS = Regex("""\b([Vv]ous|[Vv]otre|[Vv]os)\b""")
    }
}
