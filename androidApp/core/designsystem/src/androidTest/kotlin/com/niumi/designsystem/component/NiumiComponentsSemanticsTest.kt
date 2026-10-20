package com.niumi.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.niumi.designsystem.drawing.LockedPhoneDrawing
import com.niumi.designsystem.drawing.PhoneNearPointDrawing
import com.niumi.designsystem.ui.theme.NiumiDimens
import com.niumi.designsystem.ui.theme.NiumiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibilité des composants (SPEC_ANDROID §15 « Règles UI », VALEURS-DE-DESIGN.md §4) : chaque
 * élément touchable a un rôle et une zone d'au moins 44 dp ; un bouton inactif n'a aucune action
 * de clic ; les trois états d'un champ de l'accueil (décision de Mehdi du 2026-10-20) : grisé
 * reste modifiable, verrouillé n'appelle jamais l'action du champ.
 */
@RunWith(AndroidJUnit4::class)
class NiumiComponentsSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Affiche [content] puis le soumet aux contrôles automatiques de l'Accessibility Test Framework
     * (libellés, zones de toucher, contraste, ordre de lecture ; décision du 2026-10-20). Ils sont
     * rejoués à chaque `performClick` qui suit.
     */
    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent { NiumiTheme(dark = false, content = content) }
        composeRule.enableAccessibilityChecks()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    private fun SemanticsNodeInteraction.assertRole(role: Role) =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, role))

    private fun SemanticsNodeInteraction.assertTouchTarget() =
        assertHeightIsAtLeast(NiumiDimens.minTouch).assertWidthIsAtLeast(NiumiDimens.minTouch)

    private fun SemanticsNodeInteraction.assertNoClickAction() =
        assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))

    private fun SemanticsNodeInteraction.assertState(text: String) =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text))

    // Boutons

    @Test
    fun thePrimaryButtonIsAFullHeightButton() {
        var clicks = 0
        show { NiumiPrimaryButton(text = "Continuer", onClick = { clicks++ }) }

        composeRule
            .onNodeWithText("Continuer")
            .assertRole(Role.Button)
            .assertHeightIsAtLeast(NiumiDimens.primaryButton)
            .performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun anInactivePrimaryButtonHasNoClickAction() {
        var clicks = 0
        show { NiumiPrimaryButton(text = "Continuer", onClick = { clicks++ }, enabled = false) }

        composeRule
            .onNodeWithText("Continuer")
            .assertRole(Role.Button)
            .assertIsNotEnabled()
            .assertNoClickAction()
            .performClick()

        assertThat(clicks).isEqualTo(0)
    }

    @Test
    fun theOutlinedButtonIsAButton() {
        var clicks = 0
        show { NiumiOutlinedButton(text = "Recopier le texte", onClick = { clicks++ }) }

        composeRule
            .onNodeWithText("Recopier le texte")
            .assertRole(Role.Button)
            .assertHeightIsAtLeast(NiumiDimens.primaryButton)
            .performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun theDiscreetLinkIsAButtonOfFullTouchHeight() {
        var clicks = 0
        show { NiumiDiscreetLink(text = "Je n'ai pas accès à mon Niumi Point", onClick = { clicks++ }) }

        composeRule
            .onNodeWithText("Je n'ai pas accès à mon Niumi Point")
            .assertRole(Role.Button)
            .assertTouchTarget()
            .performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun aDurationPillIsARadioButtonThatSaysWhetherItIsChosen() {
        var clicks = 0
        show {
            Column {
                NiumiPillButton(text = "2 min", selected = true, onClick = { clicks++ })
                NiumiPillButton(text = "5 min", selected = false, onClick = { clicks++ })
            }
        }

        composeRule
            .onNodeWithText("2 min")
            .assertRole(Role.RadioButton)
            .assertIsSelected()
            .assertTouchTarget()
        composeRule
            .onNodeWithText("5 min")
            .assertIsNotSelected()
            .performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun anActionPillIsAButton() {
        var clicks = 0
        show { NiumiActionPill(text = "Modifier", onClick = { clicks++ }) }

        composeRule
            .onNodeWithText("Modifier")
            .assertRole(Role.Button)
            .assertTouchTarget()
            .performClick()

        assertThat(clicks).isEqualTo(1)
    }

    // Pastille de jour

    @Test
    fun aDayChipIsACheckboxNamedAfterItsDay() {
        var toggles = 0
        show {
            NiumiDayChip(label = "L", accessibilityLabel = "Lundi", checked = true, onToggle = { toggles++ })
        }

        composeRule
            .onNodeWithContentDescription("Lundi")
            .assertRole(Role.Checkbox)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assertTouchTarget()
            .performClick()

        assertThat(toggles).isEqualTo(1)
    }

    @Test
    fun aGreyedDayChipCanStillBeToggled() {
        var toggles = 0
        show {
            NiumiDayChip(
                label = "S",
                accessibilityLabel = "Samedi",
                checked = false,
                onToggle = { toggles++ },
                state = NiumiFieldState.Greyed,
            )
        }

        composeRule
            .onNodeWithContentDescription("Samedi")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            .performClick()

        assertThat(toggles).isEqualTo(1)
    }

    @Test
    fun aLockedDayChipNeverTogglesAndSaysItIsLocked() {
        var toggles = 0
        var lockedTouches = 0
        show {
            NiumiDayChip(
                label = "L",
                accessibilityLabel = "Lundi",
                checked = true,
                onToggle = { toggles++ },
                state = NiumiFieldState.Locked,
                onLockedClick = { lockedTouches++ },
            )
        }

        // « Verrouillé » s'ajoute au nom du jour : un état de description remplacerait « coché »
        // dans l'annonce de TalkBack, qui ne dirait plus quels jours sonnent (relecture).
        composeRule
            .onNodeWithContentDescription("Lundi, ${NiumiComponentTexts.LOCKED}")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assertTouchTarget()
            .performClick()

        assertThat(toggles).isEqualTo(0)
        assertThat(lockedTouches).isEqualTo(1)
    }

    @Test
    fun aLockedDayChipWithoutLockedActionHasNoClickAction() {
        var toggles = 0
        show {
            NiumiDayChip(
                label = "L",
                accessibilityLabel = "Lundi",
                checked = true,
                onToggle = { toggles++ },
                state = NiumiFieldState.Locked,
            )
        }

        composeRule
            .onNodeWithContentDescription("Lundi, ${NiumiComponentTexts.LOCKED}")
            .assertIsNotEnabled()
            .assertNoClickAction()
            .performClick()

        assertThat(toggles).isEqualTo(0)
    }

    // Rangée de liste

    @Test
    fun aListRowIsAButtonOfTheHeightOfTheDesign() {
        var clicks = 0
        show { NiumiListRow(title = "Aide", onClick = { clicks++ }) }

        composeRule
            .onNodeWithText("Aide")
            .assertRole(Role.Button)
            .assertHeightIsAtLeast(NiumiDimens.listRowMinHeight)
            .performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun aGreyedListRowStillOpens() {
        var clicks = 0
        show { NiumiListRow(title = "Sonnerie", onClick = { clicks++ }, state = NiumiFieldState.Greyed) }

        composeRule.onNodeWithText("Sonnerie").performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun aLockedListRowNeverOpensAndSaysItIsLocked() {
        var clicks = 0
        var lockedTouches = 0
        show {
            NiumiListRow(
                title = "Applications bloquées",
                onClick = { clicks++ },
                state = NiumiFieldState.Locked,
                onLockedClick = { lockedTouches++ },
            )
        }

        composeRule
            .onNodeWithText("Applications bloquées")
            .assertState(NiumiComponentTexts.LOCKED)
            .performClick()

        assertThat(clicks).isEqualTo(0)
        assertThat(lockedTouches).isEqualTo(1)
    }

    @Test
    fun aLockedListRowWithoutLockedActionHasNoClickAction() {
        var clicks = 0
        show {
            NiumiListRow(title = "Applications bloquées", onClick = { clicks++ }, state = NiumiFieldState.Locked)
        }

        composeRule
            .onNodeWithText("Applications bloquées")
            .assertIsNotEnabled()
            .assertNoClickAction()
            .performClick()

        assertThat(clicks).isEqualTo(0)
    }

    @Test
    fun aListRowWithoutActionIsNotClickable() {
        show { NiumiListRow(title = "Volume du téléphone", onClick = null) }

        composeRule.onNodeWithText("Volume du téléphone").assertNoClickAction()
    }

    // Interrupteur

    @Test
    fun theSwitchIsASwitchOfFullTouchSize() {
        val changes = mutableListOf<Boolean>()
        show {
            NiumiSwitch(
                checked = false,
                onCheckedChange = { changes += it },
                contentDescription = "Réveil désactivé",
            )
        }

        composeRule
            .onNodeWithContentDescription("Réveil désactivé")
            .assertRole(Role.Switch)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            .assertTouchTarget()
            .assertWidthIsAtLeast(NiumiDimens.switchWidth)
            .performClick()

        assertThat(changes).containsExactly(true)
    }

    @Test
    fun anInactiveSwitchHasNoClickAction() {
        val changes = mutableListOf<Boolean>()
        show {
            NiumiSwitch(
                checked = true,
                onCheckedChange = { changes += it },
                contentDescription = "Réveil activé",
                enabled = false,
            )
        }

        composeRule
            .onNodeWithContentDescription("Réveil activé")
            .assertIsNotEnabled()
            .assertNoClickAction()
            .performClick()

        assertThat(changes).isEmpty()
    }

    // Barre du haut, logo, indicateur

    @Test
    fun theBackArrowIsANamedButtonAndTheTitleAHeading() {
        var backs = 0
        show { NiumiTopBar(onBack = { backs++ }, backLabel = "Retour à l'accueil", title = "Réglages") }

        composeRule
            .onNodeWithContentDescription("Retour à l'accueil")
            .assertRole(Role.Button)
            .assertTouchTarget()
            .performClick()
        composeRule.onNodeWithText("Réglages").assert(isHeading())

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun theLogoIsAnImageNamedNiumi() {
        show { NiumiLogo() }

        composeRule
            .onNodeWithContentDescription(NiumiComponentTexts.LOGO)
            .assertRole(Role.Image)
            .assertNoClickAction()
    }

    @Test
    fun thePageIndicatorIsAnnouncedOnceAndIsNotClickable() {
        show { NiumiPageIndicator(count = 2, current = 0, label = "Panneau 1 sur 2") }

        composeRule
            .onNodeWithContentDescription("Panneau 1 sur 2")
            .assertNoClickAction()
    }

    // Dessins (étape 35)

    @Test
    fun eachDrawingIsAnnouncedOnceAsAnImage() {
        show {
            Column {
                LockedPhoneDrawing(contentDescription = "Téléphone verrouillé")
                PhoneNearPointDrawing(contentDescription = "Téléphone et Niumi Point")
            }
        }

        listOf("Téléphone verrouillé", "Téléphone et Niumi Point").forEach { label ->
            composeRule
                .onNodeWithContentDescription(label)
                .assertRole(Role.Image)
                .assertNoClickAction()
        }
    }
}
