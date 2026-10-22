package com.niumi.feature.setup.apps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.core.domain.AppSelectionSummary
import com.niumi.designsystem.component.NiumiLoadingIndicatorDefaults
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme
import com.niumi.system.apps.InstalledApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Écran 4 (`SPECIFICATION.md` §8, maquettes 10 à 12, décisions du 2026-10-22) : six cases vides et
 * « Continuer » inactif avant le choix ; la sélection validée ensuite, cinq icônes et « +N » au-delà
 * de six ; la feuille avec sa croix, sa recherche, ses cases à cocher, « Valider » inactif sans
 * coche, et les cases non cochées inactives à 50. Chaque bouton n'appelle que son action.
 */
@RunWith(AndroidJUnit4::class)
class AppSelectionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val actions =
        AppSelectionActions(
            onOpenPage = { calls += "open" },
            onContinue = { calls += "continue" },
            onToggle = { calls += "toggle:$it" },
            onSearch = { calls += "search:$it" },
            onDismissPage = { calls += "dismiss" },
            onValidate = { calls += "validate" },
        )

    private fun apps(count: Int): List<InstalledApp> =
        (1..count).map { InstalledApp("com.example.app$it", "Application $it", icon = null) }

    /** [width] fixe la largeur de l'écran : le nombre de cases de la carte en dépend. */
    private fun setContent(
        state: AppSelectionUiState,
        width: Dp? = null,
    ) {
        composeRule.setContent {
            NiumiTheme(dark = false) {
                val sized = if (width == null) Modifier else Modifier.requiredWidth(width)
                Box(modifier = sized) { AppSelectionScreen(state = state, actions = actions) }
            }
        }
    }

    private fun page(
        apps: List<InstalledApp>,
        draft: Set<String>,
        query: String = "",
    ): AppSelectionPageState =
        AppSelectionPageState(
            draft = draft,
            query = query,
            items =
                apps.map {
                    val selected = it.packageName in draft
                    AppSelectionItem(
                        app = it,
                        isSelected = selected,
                        isEnabled = selected || draft.size < AppSelectionSummary.MAX_COUNT,
                        showPackageName = false,
                    )
                },
            chosenItems = emptyList(),
            isLoading = false,
            hasApps = apps.isNotEmpty(),
            isSessionInProgress = false,
        )

    @Test
    fun beforeTheChoiceTheCardIsEmptyAndContinueIsOff() {
        setContent(AppSelectionUiState(isLoading = false))

        composeRule.onNode(hasText(AppSelectionTexts.TITLE) and isHeading()).assertExists()
        composeRule.onNodeWithText(AppSelectionTexts.BODY).assertExists()
        composeRule.onNodeWithText(AppSelectionTexts.NONE_CHOSEN).assertExists()
        composeRule.onNodeWithText(AppSelectionTexts.HINT_EMPTY).assertExists()
        composeRule.onNodeWithText(AppSelectionTexts.CONTINUE).assertIsNotEnabled().performClick()
        composeRule.onNodeWithText(AppSelectionTexts.CHOOSE).performClick()

        assertThat(calls).containsExactly("open")
    }

    @Test
    fun afterTheChoiceTheCardShowsTheValidatedSelection() {
        setContent(AppSelectionUiState(saved = apps(6), isLoading = false))

        composeRule.onNodeWithText("6 applications choisies").assertExists()
        composeRule.onNodeWithContentDescription("Icônes des 6 applications choisies").assertExists()
        composeRule.onNodeWithText(AppSelectionTexts.HINT_CHOSEN).assertExists()
        composeRule.onNodeWithText(AppSelectionTexts.MODIFY).assertHeightIsEqualTo(NiumiDimens.pillButton)
        composeRule.onNodeWithText(AppSelectionTexts.CONTINUE).assertIsEnabled().performClick()
        composeRule.onNodeWithText(AppSelectionTexts.MODIFY).performClick()

        assertThat(calls).containsExactly("continue", "open").inOrder()
    }

    @Test
    fun beyondSixTheLastSlotCountsTheOthers() {
        setContent(AppSelectionUiState(saved = apps(15), isLoading = false), width = 390.dp)

        composeRule.onNodeWithText("15 applications choisies").assertExists()
        composeRule.onNodeWithContentDescription("Icônes des 15 applications choisies").assertExists()
        composeRule.onNodeWithText("+10", useUnmergedTree = true).assertExists()
    }

    /** 360 dp de large : cinq cases seulement, la cinquième compte les onze autres. */
    @Test
    fun onANarrowScreenTheCardShowsFewerSlots() {
        setContent(AppSelectionUiState(saved = apps(15), isLoading = false), width = 360.dp)

        composeRule.onNodeWithText("+11", useUnmergedTree = true).assertExists()
    }

    @Test
    fun thePageHasItsTitleCrossSearchAndCheckboxes() {
        val apps = apps(8)
        setContent(AppSelectionUiState(isLoading = false, page = page(apps, draft = setOf("com.example.app2"))))

        composeRule.onNode(hasText(AppSelectionTexts.PAGE_TITLE) and isHeading()).assertExists()
        composeRule.onNodeWithContentDescription(AppSelectionTexts.SEARCH_LABEL).assertHeightIsEqualTo(
            NiumiDimens.searchFieldHeight,
        )
        composeRule.onNodeWithText("1 application choisie").assertExists()
        composeRule.onNode(isToggleable() and hasText("Application 2")).assertIsOn()
        composeRule.onNode(isToggleable() and hasText("Application 1")).assertIsOff().performClick()
        composeRule.onNodeWithContentDescription(AppSelectionTexts.SEARCH_LABEL).performTextInput("insta")
        composeRule.onNodeWithText(AppSelectionTexts.VALIDATE).assertIsEnabled().performClick()
        composeRule.onNodeWithContentDescription(AppSelectionTexts.CLOSE_LABEL).performClick()
        composeRule.waitForIdle()

        // L'état de ce test ne suit pas la saisie : le champ renvoie un jour ou l'autre sa valeur
        // d'origine (« search: »), à un moment qui varie. Dans l'app, l'état suit la saisie.
        assertThat(calls.filterNot { it == "search:" })
            .containsExactly("toggle:com.example.app1", "search:insta", "validate", "dismiss")
            .inOrder()
    }

    /** Après l'écriture, la feuille se range d'elle-même, avec son animation, puis se ferme. */
    @Test
    fun aSavedPageClosesItself() {
        val saved = page(apps(3), draft = setOf("com.example.app1")).copy(isSaved = true)
        setContent(AppSelectionUiState(isLoading = false, page = saved))
        composeRule.waitForIdle()

        assertThat(calls).containsExactly("dismiss")
    }

    /** Croix d'effacement (décision du 2026-10-22) : seulement avec du texte, elle vide le champ. */
    @Test
    fun theClearCrossEmptiesTheSearch() {
        setContent(AppSelectionUiState(isLoading = false, page = page(apps(3), draft = emptySet(), query = "insta")))

        composeRule.onNodeWithContentDescription(AppSelectionTexts.CLEAR_SEARCH).performClick()

        assertThat(calls).containsExactly("search:")
    }

    @Test
    fun withAnEmptySearchThereIsNoClearCross() {
        setContent(AppSelectionUiState(isLoading = false, page = page(apps(3), draft = emptySet())))

        composeRule.onNodeWithContentDescription(AppSelectionTexts.CLEAR_SEARCH).assertDoesNotExist()
    }

    /** Pendant le chargement, un indicateur tient la place de la carte, puis de la grille. */
    @Test
    fun whileLoadingTheScreenAndThePageShowTheIndicator() {
        setContent(AppSelectionUiState(isLoading = true))
        // Le délai d'apparition n'est pas un travail en attente : l'horloge de test ne le saute pas.
        composeRule.mainClock.advanceTimeBy(NiumiLoadingIndicatorDefaults.DELAY_MILLIS + FRAME)
        composeRule.onNodeWithContentDescription(AppSelectionTexts.LOADING).assertExists()
        composeRule.onNodeWithText(AppSelectionTexts.NONE_CHOSEN).assertDoesNotExist()
    }

    @Test
    fun whileLoadingThePageShowsTheIndicator() {
        val loading = page(emptyList(), draft = setOf("com.example.app1")).copy(isLoading = true, hasApps = false)
        setContent(AppSelectionUiState(isLoading = true, page = loading))
        composeRule.mainClock.advanceTimeBy(NiumiLoadingIndicatorDefaults.DELAY_MILLIS + FRAME)

        composeRule.onNodeWithContentDescription(AppSelectionTexts.LOADING).assertExists()
        composeRule.onNodeWithText(AppSelectionTexts.NO_APPS).assertDoesNotExist()
    }

    @Test
    fun withoutACheckValidateIsOff() {
        // Une application déjà validée : la carte dit « 1 application choisie », la feuille vide
        // dit « Aucune application choisie ».
        setContent(AppSelectionUiState(saved = apps(1), isLoading = false, page = page(apps(3), draft = emptySet())))

        composeRule.onNodeWithText(AppSelectionTexts.NONE_CHOSEN).assertExists()
        composeRule.onNodeWithText(AppSelectionTexts.VALIDATE).assertIsNotEnabled().performClick()

        assertThat(calls).isEmpty()
    }

    @Test
    fun atFiftyTheUncheckedTilesAreOffAndTheCounterSaysSo() {
        val apps = apps(AppSelectionSummary.MAX_COUNT + 2)
        val draft = apps.take(AppSelectionSummary.MAX_COUNT).mapTo(mutableSetOf()) { it.packageName }
        val last = apps.last().label
        setContent(AppSelectionUiState(isLoading = false, page = page(apps.reversed(), draft)))

        composeRule.onNodeWithText(AppSelectionTexts.limitReached()).assertExists()
        composeRule.onNode(isToggleable() and hasText(last)).assertIsNotEnabled().performClick()

        assertThat(calls).isEmpty()
    }

    @Test
    fun theEmptyStatesAreWritten() {
        setContent(AppSelectionUiState(isLoading = false, page = page(emptyList(), draft = emptySet())))

        composeRule.onNodeWithText(AppSelectionTexts.NO_APPS).assertExists()
    }

    @Test
    fun aSearchWithoutResultSaysSo() {
        val noMatch = page(emptyList(), draft = emptySet(), query = "zzz").copy(hasApps = true)
        setContent(AppSelectionUiState(isLoading = false, page = noMatch))

        composeRule.onNodeWithText(AppSelectionTexts.NO_MATCH).assertExists()
    }

    @Test
    fun theScreenPassesTheAccessibilityChecks() {
        setContent(AppSelectionUiState(saved = apps(6), isLoading = false))
        composeRule.enableAccessibilityChecks()

        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun thePagePassesTheAccessibilityChecks() {
        setContent(AppSelectionUiState(isLoading = false, page = page(apps(8), draft = setOf("com.example.app1"))))
        composeRule.enableAccessibilityChecks()

        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    /** Pleine page (décision du 2026-10-22) : l'écran 4 laisse toute la place à la page. */
    @Test
    fun thePageReplacesTheScreen() {
        setContent(AppSelectionUiState(saved = apps(1), isLoading = false, page = page(apps(3), draft = emptySet())))

        composeRule.onNodeWithText(AppSelectionTexts.TITLE).assertDoesNotExist()
        composeRule.onNodeWithText(AppSelectionTexts.CONTINUE).assertDoesNotExist()
    }

    /** Section « Applications déjà choisies » en tête, puis « Toutes les applications » (décision du 2026-10-22). */
    @Test
    fun theChosenSectionComesFirst() {
        val all = apps(4)
        val base = page(all, draft = setOf("com.example.app2"))
        val withSection =
            base.copy(
                chosenItems = base.items.filter { it.app.packageName == "com.example.app2" },
                items = base.items.filterNot { it.app.packageName == "com.example.app2" },
            )
        setContent(AppSelectionUiState(isLoading = false, page = withSection))

        val chosenTop = composeRule.onNode(hasText(AppSelectionTexts.CHOSEN_SECTION) and isHeading()).topInRoot()
        val chosenTile = composeRule.onNode(isToggleable() and hasText("Application 2")).assertIsOn().topInRoot()
        val allTop = composeRule.onNode(hasText(AppSelectionTexts.ALL_SECTION) and isHeading()).topInRoot()
        val otherTile = composeRule.onNode(isToggleable() and hasText("Application 1")).topInRoot()
        assertThat(listOf(chosenTop, chosenTile, allTop, otherTile)).isInStrictOrder()
    }

    /** Sans application choisie à l'ouverture, la grille n'a pas de titre (maquette 11). */
    @Test
    fun withoutAChosenAppTheGridHasNoTitle() {
        setContent(AppSelectionUiState(isLoading = false, page = page(apps(4), draft = emptySet())))

        composeRule.onNodeWithText(AppSelectionTexts.CHOSEN_SECTION).assertDoesNotExist()
        composeRule.onNodeWithText(AppSelectionTexts.ALL_SECTION).assertDoesNotExist()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.topInRoot(): Float =
        fetchSemanticsNode().boundsInRoot.top

    private companion object {
        const val FRAME = 32L
    }
}
